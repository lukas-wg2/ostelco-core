package org.ostelco.simcards.admin

import junit.framework.TestCase.*
import org.junit.Test
import org.ostelco.simcards.admin.GenerateBatchDescription.Companion.luhnCheck
import org.ostelco.simcards.admin.GenerateBatchDescription.Companion.luhnComplete
import org.ostelco.simcards.admin.GenerateBatchDescription.Companion.prettyPrintSimBatchDescription

class GenerateBatchDescriptionTest {


    @Test
    fun testNegativeLuhnCheck() {
        for (x in listOf(
                "79927398710",
                "79927398711",
                "79927398712",
                "79927398714",
                "79927398715",
                "79927398716",
                "79927398717",
                "79927398718",
                "79927398719")) {
            assertFalse(luhnCheck(x))
        }
    }

    @Test
    fun testPositiveLuhnCheck() {
        for (x in listOf("79927398713")) {
            assertTrue(luhnCheck(x))
        }
    }

    @Test
    fun testLuhnComplete() {
        assertTrue(luhnCheck(luhnComplete("79927398710")))
    }

    @Test
    fun testGenerateIccid() {
        val iccid = IccidBasis(cc = 47, serialNumber = 1)
        val iccidString = iccid.asIccid()
        assertEquals(19, iccidString.length)
    }

    @Test
    fun funPrettyPrintBatchDescription() {
        val iccid = IccidBasis(cc = 47, serialNumber = 1).asIccid()

        val batch = SimBatchDescription(
                customer = "FooTel",
                profileType = "FooTelStd",
                orderDate = "20181212",
                batchNo = 1,
                quantity = 1,
                iccidStart = iccid,
                imsiStart = "4201710010000",
                opKeyLabel = "FooTel-OP",
                transportKeyLabel = "FooTel-TK-1"
        )

        val pp = prettyPrintSimBatchDescription(batch)
        println(pp)
        assertTrue(pp.length > 100)
    }
}


/**
 *  MM = Constant (ISO 7812 Major Industry Identifier)
 *  CC = Country Code
 *  II = Issuer Identifier
 *  serialNumber = unique  positive number.
 */
class IccidBasis(val mm: Int = 89, val cc: Int, val ii: Int = 0, val serialNumber: Int) {
    fun asIccid(): String {
        val protoIccid = "%02d%02d%02d%012d".format(mm, cc, ii, serialNumber)
        return luhnComplete(protoIccid)
    }
}


// TODO: This is just a first iteration,things like dates etc. should be
//       not be using strings but proper time-objects, but we'll do this for now
//       just too get going.

class SimBatchDescription(
        val customer: String,
        val profileType: String,
        val orderDate: String,
        val batchNo: Int,
        val quantity: Int,
        val iccidStart: String,
        val imsiStart: String,
        val opKeyLabel: String,
        val transportKeyLabel: String)


class GenerateBatchDescription {
    companion object {
        /**
         * Implement the Luhn algorithm for checksums.  Used when
         * producing valid ICCID numbers.
         * https://en.wikipedia.org/wiki/Luhn_algorithm
         */
        fun luhnCheck(ccNumber: String): Boolean {
            var sum = 0
            var alternate = false
            for (i in ccNumber.length - 1 downTo 0) {
                var n = Integer.parseInt(ccNumber.substring(i, i + 1))
                if (alternate) {
                    n *= 2
                    if (n > 9) {
                        n = n % 10 + 1
                    }
                }
                sum += n
                alternate = !alternate
            }
            return sum % 10 == 0
        }

        fun luhnComplete(s: String): String {
            for (c in listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")) {
                val candidate = "${s}${c}"
                if (luhnCheck(candidate)) {
                    return candidate
                }
            }
            throw RuntimeException("Luhn completion failed for string '$s'")
        }


        fun prettyPrintSimBatchDescription(bd: SimBatchDescription): String {
            return """
            *HEADER DESCRIPTION
            ***************************************
            Customer        : ${bd.customer}
            ProfileType     : ${bd.profileType}
            Order Date      : ${bd.orderDate}
            Batch No        : ${bd.orderDate}${bd.batchNo}
            Quantity        : ${bd.quantity}
            OP Key label    :
            Transport Key   :
            ***************************************
            *INPUT VARIABLES
            ***************************************
            var_In:
            ICCID: ${bd.iccidStart}
            IMSI: ${bd.imsiStart}
            ***************************************
            *OUTPUT VARIABLES
            ***************************************
            var_Out: ICCID/IMSI/KI
            """.trimIndent()
        }
    }
}