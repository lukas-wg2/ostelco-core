package org.ostelco.prime.ocs

import com.google.common.base.Preconditions.checkNotNull
import com.lmax.disruptor.EventHandler
import io.grpc.stub.StreamObserver
import org.ostelco.ocs.api.ActivateResponse
import org.ostelco.ocs.api.CreditControlAnswerInfo
import org.ostelco.ocs.api.CreditControlRequestInfo
import org.ostelco.ocs.api.OcsServiceGrpc
import org.ostelco.prime.disruptor.PrimeEvent
import org.ostelco.prime.disruptor.PrimeEventProducer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class OcsService(producer: PrimeEventProducer) {

    private val CreditControlClientMap: ConcurrentMap<String, StreamObserver<CreditControlAnswerInfo>>

    /**
     * A holder for
     * [<]
     * instances that are somehow used
     */
    private val activateResponseHolder: ActivateResponseHolder

    private val producer: PrimeEventProducer

    private val eventHandler: EventHandler<PrimeEvent>

    private val ocsServerImplBaseImpl: OcsServiceGrpc.OcsServiceImplBase

    init {
        this.producer = checkNotNull(producer)
        this.CreditControlClientMap = ConcurrentHashMap()
        this.eventHandler = EventHandlerImpl(this)
        this.ocsServerImplBaseImpl = OcsGRPCService(this)
        this.activateResponseHolder = ActivateResponseHolder()
    }

    fun asEventHandler(): EventHandler<PrimeEvent> {
        return eventHandler
    }

    fun returnUnusedDataBucketEvent(
            msisdn: String,
            bucketBytes: Long) {
        producer.releaseReservedDataBucketEvent(
                msisdn,
                bucketBytes)
    }

    /**
     * Return a service that can be used to serve incoming GRPC requests.   The service
     * is typically bound to a service port using the GRPC ServerBuilder mechanism
     * provide by GRPC:
     * `
     * server = ServerBuilder.
     * forPort(port).
     * addService(service).
     * build();
    ` *
     *
     * @return The service that can receive incoming GPRS messages
     */
    fun asOcsServiceImplBase(): OcsServiceGrpc.OcsServiceImplBase {
        return this.ocsServerImplBaseImpl
    }

    protected fun getCreditControlClientForStream(
            streamId: String): StreamObserver<CreditControlAnswerInfo>? {
        // Here we need to Convert it back to an answer.
        CreditControlClientMap[streamId]
        return CreditControlClientMap[streamId]
    }

    fun activateOnNextResponse(response: ActivateResponse) {
        this.activateResponseHolder.onNextResponse(response)
    }

    fun updateActivateResponse(
            activateResponse: StreamObserver<ActivateResponse>) {
        this.activateResponseHolder.setActivateResponse(activateResponse)
    }

    fun deleteCreditControlClient(streamId: String) {
        this.CreditControlClientMap.remove(streamId)
    }

    fun creditControlRequestEvent(
            request: CreditControlRequestInfo,
            streamId: String) {
        producer.injectCreditControlRequestIntoRingbuffer(request, streamId)
    }

    fun putCreditControlClient(
            streamId: String,
            creditControlAnswer: StreamObserver<CreditControlAnswerInfo>) {
        CreditControlClientMap[streamId] = creditControlAnswer
    }

    fun sendCreditControlAnswer(streamId: String, creditControlAnswerInfo: CreditControlAnswerInfo) {
        val creditControlAnswer = getCreditControlClientForStream(streamId)

        creditControlAnswer?.onNext(creditControlAnswerInfo)
    }
}
