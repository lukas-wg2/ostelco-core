package org.ostelco.storage.scaninfo.shredder

import com.google.cloud.datastore.Cursor
import com.google.cloud.datastore.DatastoreException
import com.google.cloud.datastore.StructuredQuery
import com.google.cloud.datastore.StructuredQuery.OrderBy
import io.dropwizard.Application
import io.dropwizard.Configuration
import io.dropwizard.cli.ConfiguredCommand
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.sourceforge.argparse4j.inf.Namespace
import org.ostelco.prime.model.ScanMetadata
import org.ostelco.prime.store.datastore.EntityStore
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.*
import kotlin.system.measureTimeMillis


/**
 * Main entry point, invoke dropwizard application.
 */
fun main(args: Array<String>) = ScanInfoShredderApplication().run(*args)

/**
 * The configuration for Scan Information Shredder module.
 */
class ScanInfoShredderConfig : Configuration() {
    var storeType = "default"
    var namespace = ""
    var deleteScan = false
    var deleteUrl = "https://netverify.com/api/netverify/v2/scans/"
}

/**
 * Main entry point to the scaninfo-shredder API server.
 */
private class ScanInfoShredderApplication : Application<ScanInfoShredderConfig>() {

    override fun initialize(bootstrap: Bootstrap<ScanInfoShredderConfig>) {
        // Enable variable substitution with environment variables
        bootstrap.configurationSourceProvider = SubstitutingSourceProvider(
                bootstrap.configurationSourceProvider,
                EnvironmentVariableSubstitutor(false))
        bootstrap.addCommand(ShredScans())
        bootstrap.addCommand(CacheJars())
    }

    override fun run(
            configuration: ScanInfoShredderConfig,
            environment: Environment) {
    }
}

/**
 * Helper class for getting environment variables.
 * Introduced to help testing.
 */
open class EnvironmentVars {
    /**
     * Retrieve the value of the environment variable.
     */
    open fun getVar(name: String): String? = System.getenv(name)
}

/**
 * Thrown when something really bad is detected and it's necessary to terminate
 * execution immediately.  No cleanup of anything will be done.
 */
private class ScanInfoShredderException(message: String) : RuntimeException(message)


/**
 * Adapter class that will delete Scan Information from Jumio.
 */
internal class ScanInfoShredder(private val config: ScanInfoShredderConfig) {

    private val logger: Logger = LoggerFactory.getLogger(ScanInfoShredder::class.java)

    val expiryDuration = 1209600000 // 2 Weeks in Milliseconds

    internal val scanMetadataStore = EntityStore(
            entityClass = ScanMetadata::class,
            type = config.storeType,
            namespace = config.namespace)

    private lateinit var filter: StructuredQuery.Filter

    /* Generated by Jumio and can be obtained from the console. */
    private lateinit var apiToken: String
    private lateinit var apiSecret: String

    // Initialize the object, get all the environment variables and initialize the encrypter library.
    fun init(environmentVars: EnvironmentVars) {
        val storeType = config.storeType
        if (storeType != "emulator" && storeType != "inmemory-emulator") {
            apiToken = environmentVars.getVar("JUMIO_API_TOKEN")
                    ?: throw Error("Missing environment variable JUMIO_API_TOKEN")
            apiSecret = environmentVars.getVar("JUMIO_API_SECRET")
                    ?: throw Error("Missing environment variable JUMIO_API_SECRET")
        } else {
            // Don't throw error during local tests
            apiToken = ""
            apiSecret = ""
        }
        logger.info("Config storeType = ${config.storeType}, namespace = ${config.namespace}, deleteScan = ${config.deleteScan} deleteUrl = ${config.deleteUrl} ")
        initDatastore()
    }

    // Integration testing helper for Datastore.
    private fun initDatastore() {
        val expiryTime: Long = Instant.now().toEpochMilli() - expiryDuration
        filter = StructuredQuery.PropertyFilter.le(ScanMetadata::processedTime.name, expiryTime)
    }

    /**
     * Deletes the scan information from Jumio database.
     */
    private fun deleteScanInformation(vendorScanId: String, baserUrl: String, username: String, password: String): Boolean {
        val seperator: String = if (baserUrl.endsWith("/")) "" else "/"
        val url = URL("$baserUrl$seperator$vendorScanId")
        val httpConn = url.openConnection() as HttpURLConnection
        val userpass = "$username:$password"
        val authHeader = "Basic ${Base64.getEncoder().encodeToString(userpass.toByteArray())}"
        httpConn.setRequestProperty("Authorization", authHeader)
        httpConn.setRequestProperty("Accept", "application/json")
        httpConn.setRequestProperty("User-Agent", "ScanInformationStore")
        //httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpConn.requestMethod = "DELETE"
        httpConn.doOutput = true

        try {
            val responseCode = httpConn.responseCode
            // always check HTTP response code first
            if (responseCode != HttpURLConnection.HTTP_OK) {
                val statusMessage = "$responseCode: ${httpConn.responseMessage}"
                logger.error("Failed to delete $url $statusMessage")
                return false
            } else {
                logger.info("Deleted $url")
            }
        } catch (e: IOException) {
            logger.error("Caught exception while trying to delete scan", e)
            return false
        } finally {
            httpConn.disconnect()
        }
        return true
    }

    private fun listScans(startCursorString: String?): Pair<Collection<ScanMetadata>, String?> {
        try {
            var startCursor: Cursor? = null
            if (startCursorString != null && startCursorString != "") {
                startCursor = Cursor.fromUrlSafe(startCursorString)  // Where we left off
            }

            // cursor = Where to start next time
            var cursor: Cursor? = null
            val resultScans = scanMetadataStore.fetch(
                    query = { queryBuilder ->
                        queryBuilder
                                .setLimit(100)  // Only process 100 at a time
                                .setStartCursor(startCursor)  // Where we left off
                                .setFilter(filter)  // Which are expired
                                .setOrderBy(OrderBy.asc(ScanMetadata::processedTime.name)) // Sorted by "processedTime"
                    },
                    onAfterQueryResultRead = { queryResults ->
                        cursor = queryResults.cursorAfter
                    })
            // Are we paging? Save Cursor
            return if (resultScans.size == 100) {
                // Cursors are WebSafe
                Pair(resultScans, cursor?.toUrlSafe())
            } else {
                Pair(resultScans, null)
            }
        } catch (e: DatastoreException) {
            logger.error("Caught exception while scanning metadata", e)
            return Pair(emptyList(), null)
        }
    }

    // Deletes the scan metadata from datastore
    private fun deleteScanMetadata(data: ScanMetadata): Boolean {
        try {
            val keyString = "${data.customerId}-${data.id}"
            scanMetadataStore.delete(keyString)
            logger.info("Deleted datastore record for $keyString")
        } catch (e: DatastoreException) {
            logger.error("Caught exception while scanning metadata", e)
            return false
        }
        return true
    }

    private fun CoroutineScope.dropScan(it: ScanMetadata): Job = launch {
        val infoDeleted = if (config.deleteScan) {
            deleteScanInformation(it.scanReference, config.deleteUrl, apiToken, apiSecret)
        } else {
            logger.info("Delete disabled, skipping ${it.scanReference}")
            true
        }
        if (infoDeleted) {
            // Delete the datastore record.
            deleteScanMetadata(it)
        }
    }

    suspend fun shred(): Int {
        var totalItems = 0
        logger.info("Looking for expired scan data...")
        val time = measureTimeMillis {
            var batchNo = 0
            var startCursor: String? = null
            do {
                var lastBatchSize = 0
                val batchTime = measureTimeMillis {
                    coroutineScope {
                        val scanResult = listScans(startCursor)
                        scanResult.first.forEach { dropScan(it) }
                        lastBatchSize = scanResult.first.size
                        totalItems += lastBatchSize
                        startCursor = scanResult.second
                        if (lastBatchSize > 0) {
                            batchNo++;
                        }
                        // coroutineScope waits for all children to finish.
                    }
                }
                if (lastBatchSize > 0) {
                    logger.info("Batch ${batchNo} finished in ${batchTime} milli seconds")
                }
            } while (startCursor != null)
        }
        logger.info("Deleted ${totalItems} scans from Jumio")
        logger.info("Task finished in ${time} milli seconds")
        return totalItems
    }
}

private class ShredScans : ConfiguredCommand<ScanInfoShredderConfig>(
        "shred",
        "Delete all Scans which are expired") {
    override fun run(bootstrap: Bootstrap<ScanInfoShredderConfig>?, namespace: Namespace?, configuration: ScanInfoShredderConfig?) {

        if (configuration == null) {
            throw ScanInfoShredderException("Configuration is null")
        }


        if (namespace == null) {
            throw ScanInfoShredderException("Namespace from config is null")
        }

        runBlocking {
            val shredder = ScanInfoShredder(configuration)
            shredder.init(EnvironmentVars())
            shredder.shred()
        }
    }
}

private class CacheJars : ConfiguredCommand<ScanInfoShredderConfig>(
        "quit",
        "Do nothing, only used to prime caches") {
    override fun run(bootstrap: Bootstrap<ScanInfoShredderConfig>?,
                     namespace: Namespace?,
                     configuration: ScanInfoShredderConfig?) {
        // Doing nothing, as advertised.
    }
}
