package org.ostelco.prime.ocs.core

import io.grpc.stub.StreamObserver
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.Test
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.MultipleServiceCreditControl
import org.ostelco.ocs.api.ServiceUnit
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import kotlin.test.fail

class OnlineChargingTest {

    @Ignore
    @Test
    fun `load test OnlineCharging directly`() = runBlocking {

        // Add delay to DB call and skip analytics and low balance notification
        OnlineCharging.loadUnitTest = true

        val streamId = UUID.randomUUID().toString()

        // count down latch to wait for all responses to return
        val cdl = CountDownLatch(COUNT)

        // response handle which will count down on receiving response
        val creditControlAnswerInfo: StreamObserver<CreditControlAnswerInfo> = object : StreamObserver<CreditControlAnswerInfo> {

            override fun onNext(value: CreditControlAnswerInfo?) {
                // count down on receiving response
                cdl.countDown()
            }

            override fun onError(t: Throwable?) {
                fail(t?.message)
            }

            override fun onCompleted() {

            }
        }

        // Setup connection stream
        OnlineCharging.putCreditControlClient(streamId = streamId, creditControlAnswer = creditControlAnswerInfo)

        // Sample request which will be sent repeatedly
        val request = CreditControlRequestInfo.newBuilder()
                .setRequestId(UUID.randomUUID().toString())
                .setMsisdn(MSISDN)
                .addMscc(0, MultipleServiceCreditControl.newBuilder()
                        .setRequested(ServiceUnit.newBuilder().setTotalOctets(100))
                        .setUsed(ServiceUnit.newBuilder().setTotalOctets(80)))
                .build()

        // Start timestamp in millisecond
        val start = Instant.now()

        // Send the same request COUNT times
        repeat(COUNT) {
            OnlineCharging.creditControlRequestEvent(streamId = streamId, request = request)
        }

        // Wait for all the responses to be returned
        println("Waiting for all responses to be returned")
        cdl.await()

        // Stop timestamp in millisecond
        val stop = Instant.now()

        // Print load test results
        val diff = stop.toEpochMilli() - start.toEpochMilli()
        println("Time diff: %,d milli sec".format(diff))
        val rate = COUNT * 1000.0 / diff
        println("Rate: %,.2f req/sec".format(rate))
    }

    companion object {
        private const val COUNT = 1_000_000
        private const val MSISDN = "4790300147"
    }
}