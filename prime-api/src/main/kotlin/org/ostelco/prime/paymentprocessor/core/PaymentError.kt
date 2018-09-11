package org.ostelco.prime.paymentprocessor.core

import javax.ws.rs.core.Response

sealed class PaymentError(val description: String) {
    open var status : Int = 0
}

class ForbiddenError(description: String) : PaymentError(description) {
    override var status : Int = Response.Status.NOT_FOUND.getStatusCode()
}

class NotFoundError(description: String) : PaymentError(description) {
    override var status : Int = Response.Status.NOT_FOUND.getStatusCode()
}

class BadGatewayError(description: String) : PaymentError(description) {
    override var status : Int = Response.Status.BAD_REQUEST.getStatusCode()
}