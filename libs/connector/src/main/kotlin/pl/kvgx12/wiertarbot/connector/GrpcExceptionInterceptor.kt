package pl.kvgx12.wiertarbot.connector

import io.grpc.*
import pl.kvgx12.wiertarbot.connector.utils.getLogger

class GrpcExceptionInterceptor : ServerInterceptor {
    private val logger = getLogger()

    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val wrappedCall = object : ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
            override fun close(status: Status, trailers: Metadata) {
                if (!status.isOk) {
                    val cause = status.cause
                    if (cause != null) {
                        logger.error("gRPC call failed: ${methodDescriptor.fullMethodName}, status: $status", cause)
                    } else {
                        logger.error("gRPC call failed: ${methodDescriptor.fullMethodName}, status: $status")
                    }
                }
                super.close(status, trailers)
            }
        }

        val listener = try {
            next.startCall(wrappedCall, headers)
        } catch (e: Exception) {
            logger.error("Exception starting gRPC call: ${call.methodDescriptor.fullMethodName}", e)
            call.close(Status.INTERNAL.withDescription("Internal server error").withCause(e), Metadata())
            return object : ServerCall.Listener<ReqT>() {}
        }

        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(listener) {
            override fun onHalfClose() {
                try {
                    super.onHalfClose()
                } catch (e: Exception) {
                    handleException(wrappedCall, e)
                }
            }

            override fun onReady() {
                try {
                    super.onReady()
                } catch (e: Exception) {
                    handleException(wrappedCall, e)
                }
            }

            override fun onMessage(message: ReqT) {
                try {
                    super.onMessage(message)
                } catch (e: Exception) {
                    handleException(wrappedCall, e)
                }
            }

            override fun onCancel() {
                try {
                    super.onCancel()
                } catch (e: Exception) {
                    handleException(wrappedCall, e)
                }
            }

            override fun onComplete() {
                try {
                    super.onComplete()
                } catch (e: Exception) {
                    handleException(wrappedCall, e)
                }
            }
        }
    }

    private fun <ReqT, RespT> handleException(call: ServerCall<ReqT, RespT>, e: Exception) {
        logger.error("Uncaught exception in gRPC listener: ${call.methodDescriptor.fullMethodName}", e)
        try {
            call.close(Status.INTERNAL.withDescription("Internal server error").withCause(e), Metadata())
        } catch (closeException: Exception) {
            logger.warn("Failed to close gRPC call after exception", closeException)
        }
    }
}
