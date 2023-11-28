package com.sunnychung.application.multiplatform.hellohttp.network

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.TypeRegistry
import com.google.protobuf.util.JsonFormat
import com.sunnychung.application.multiplatform.hellohttp.extension.GrpcRequestExtra
import com.sunnychung.application.multiplatform.hellohttp.helper.CountDownLatch
import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcApiSpec
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcMethod
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.util.flowAndStreamObserver
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ClientInterceptors
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.ForwardingClientCallListener.SimpleForwardingClientCallListener
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.ProtoUtils
import io.grpc.reflection.v1alpha.ServerReflectionGrpc
import io.grpc.reflection.v1alpha.ServerReflectionRequest
import io.grpc.reflection.v1alpha.ServerReflectionResponse
import io.grpc.stub.ClientCalls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.URI
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class GrpcTransportClient(networkClientManager: NetworkClientManager) : AbstractTransportClient(networkClientManager) {

    fun buildChannel(uri: URI, sslConfig: SslConfig): ManagedChannel {
        return ManagedChannelBuilder.forAddress(uri.host, uri.port)
            .disableRetry()
            .apply {
                if (sslConfig.isInsecure == true) {
                    usePlaintext()
                }
            }
            .build()
    }

    override fun sendRequest(
        request: HttpRequest,
        requestExampleId: String,
        requestId: String,
        subprojectId: String,
        postFlightAction: ((UserResponse) -> Unit)?,
        httpConfig: HttpConfig,
        sslConfig: SslConfig,
    ): CallData {
        val uri = URI.create(request.url)

        val call = createCallData(
            requestBodySize = null,
            requestExampleId = requestExampleId,
            requestId = requestId,
            subprojectId = subprojectId
        )

        val extra = request.extra as GrpcRequestExtra
        val `package` = extra.destination.service.substringBeforeLast(delimiter = '.', missingDelimiterValue = "")
        val serviceName = extra.destination.service.substringAfterLast('.')
        val serviceFullName = listOf(`package`, serviceName).joinToString(separator = ".")
        val methodName = extra.destination.method

        val apiSpec: GrpcApiSpec = extra.apiSpec!!

        val out = call.response

        CoroutineScope(Dispatchers.IO).launch {
            call.status = ConnectionStatus.CONNECTING

            try {

                val channel0 = buildChannel(uri, sslConfig) // blocking call
                val channel = ClientInterceptors.intercept(channel0, object : ClientInterceptor {
                    override fun <ReqT : Any?, RespT : Any?> interceptCall(
                        method: MethodDescriptor<ReqT, RespT>,
                        callOptions: CallOptions,
                        next: Channel
                    ): ClientCall<ReqT, RespT> =
                        object : SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                                request.headers.forEach {
                                    headers.put(Metadata.Key.of(it.first, Metadata.ASCII_STRING_MARSHALLER), it.second)
                                }
                                super.start(object : SimpleForwardingClientCallListener<RespT>(responseListener) {
                                    override fun onHeaders(headers: Metadata) {
                                        out.headers = headers.keys().flatMap { key ->
                                            headers.getAll(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER))
                                                ?.map { key to it } ?: emptyList()
                                        }
                                        super.onHeaders(headers)
                                    }

                                    override fun onMessage(message: RespT) {
                                        log.d { "grpc inte onMessage" }
                                        super.onMessage(message)
                                    }

                                    override fun onClose(status: Status?, trailers: Metadata?) {
                                        log.d { "grpc inte onClose $status [$trailers]" }
                                        super.onClose(status, trailers)
                                    }
                                }, headers)
                            }
                        }

                })

                val fileDescriptorProtoByFilename = apiSpec.rawFileDescriptors.map { FileDescriptorProto.parseFrom(it) }
                    .associateBy { it.name }
                val fileDescriptorProto = fileDescriptorProtoByFilename.values
                    .first { it.`package` == `package` && it.serviceList.any { it.name == serviceName && it.methodList.any { it.name == methodName } } }
                val fileDescriptor = buildFileDescriptor(fileDescriptorProto, fileDescriptorProtoByFilename)
                val methodDescriptor0 =
                    fileDescriptor.services.first { it.fullName == serviceFullName }.methods.first { it.name == methodName }
                val grpcMethodDescriptor = MethodDescriptor.newBuilder<DynamicMessage, DynamicMessage>()
                    .setFullMethodName(
                        MethodDescriptor.generateFullMethodName(
                            methodDescriptor0.service.fullName,
                            methodDescriptor0.name
                        )
                    )
                    .setRequestMarshaller(
                        ProtoUtils.marshaller(
                            DynamicMessage.newBuilder(methodDescriptor0.inputType).build()
                        )
                    )
                    .setResponseMarshaller(
                        ProtoUtils.marshaller(
                            DynamicMessage.newBuilder(methodDescriptor0.outputType).build()
                        )
                    )
                    .setType(MethodDescriptor.MethodType.UNKNOWN) // TODO
                    .build()

                val typeRegistry = TypeRegistry.newBuilder().add(fileDescriptor.messageTypes).build()
                val grpcRequest = DynamicMessage.newBuilder(methodDescriptor0.inputType)
                    .apply {
                        JsonFormat.parser().usingTypeRegistry(typeRegistry)
                            .merge((request.body as StringBody).value, this)
                    }
                    .build()

                call.waitForPreparation()
                out.startAt = KInstant.now()
                out.application = ProtocolApplication.Grpc

                emitEvent(call.id, "Connecting to ${hostFromUrl(request.url)}")

                val (responseFlow, responseObserver) = flowAndStreamObserver<DynamicMessage>()
                try {
                    call.cancel = {
                        responseObserver.onCompleted()
                        channel0.shutdown()
                        call.status = ConnectionStatus.DISCONNECTED
                    }

                    ClientCalls.asyncUnaryCall(
                        channel.newCall(grpcMethodDescriptor, CallOptions.DEFAULT),
                        grpcRequest,
                        responseObserver
                    )
                    val response = responseFlow.last()

                    val responseJson = JsonFormat.printer()
                        .usingTypeRegistry(typeRegistry)
                        .includingDefaultValueFields()
                        .print(response)

                    log.d { "Response = $responseJson" }

                    emitEvent(call.id, "Received response message") // TODO better events?

                    out.body = responseJson.encodeToByteArray()

                    out.endAt = KInstant.now()
                    out.statusText = "No error"
                    call.status = ConnectionStatus.DISCONNECTED

                    if (!out.isError && postFlightAction != null) {
                        executePostFlightAction(call.id, out, postFlightAction)
                    }
                } catch (e: StatusRuntimeException) {
                    out.endAt = KInstant.now()
                    call.status = ConnectionStatus.DISCONNECTED

                    out.statusCode = e.status.code.value()
                    out.statusText = e.status.code.name
                    out.errorMessage = e.message
                    out.isError = true

                    log.d(e) { "Grpc Status Error" }
                } catch (e: Throwable) {
                    out.endAt = KInstant.now()
                    call.status = ConnectionStatus.DISCONNECTED

                    out.errorMessage = e.message
                    out.isError = true

                    log.d(e) { "Grpc Error" }
                } finally {
                    channel0.shutdownNow()

                    call.consumePayloads()
                    emitEvent(call.id, "Response completed")
                }
            } catch (e: Throwable) {
                log.d(e) { "Grpc Outer Error" }

                call.status = ConnectionStatus.DISCONNECTED
                out.errorMessage = e.message
                out.isError = true

                emitEvent(call.id, "Terminated with error")
            }
        }
        return call
    }

    private fun buildFileDescriptor(proto: FileDescriptorProto, fileDescriptorProtoByFilename: Map<String, FileDescriptorProto>): FileDescriptor {
        return FileDescriptor.buildFrom(proto, proto.dependencyList.map { buildFileDescriptor(fileDescriptorProtoByFilename[it]!!, fileDescriptorProtoByFilename) }.toTypedArray())
    }

    suspend fun fetchServiceSpec(
        url: String,
        sslConfig: SslConfig
    ): GrpcApiSpec {
        val uri = URI.create(url)
        val channel = buildChannel(uri, sslConfig)
        try {
            return fetchServiceSpec("${uri.host}:${uri.port}", channel)
        } finally {
            channel.shutdownNow()
        }
    }

    suspend fun fetchServiceSpec(name: String, channel: ManagedChannel): GrpcApiSpec {
        val stub = ServerReflectionGrpc.newStub(channel)

        val (responseFlow, responseObserver) = flowAndStreamObserver<ServerReflectionResponse>()
//        val serviceResponseFlow = responseFlow.map {
//            it.listServicesResponse.serviceList
//        }
        val request = stub.serverReflectionInfo(responseObserver)
        request.onNext(ServerReflectionRequest.newBuilder()
            .setListServices("*")
            .build())
        val services = responseFlow
            .filter { it.messageResponseCase == ServerReflectionResponse.MessageResponseCase.LIST_SERVICES_RESPONSE }
            .map {
                it.listServicesResponse.serviceList
            }.first()

        val fileDescriptorByServiceNameByProtoFilename = ConcurrentHashMap<String, Map<String, FileDescriptorProto>>()
        val rawFileDescriptors = Collections.synchronizedList(mutableListOf<ByteArray>())
        val latch = CountDownLatch(services.size)
        services.forEach {
            log.d { "grpc refl svc '${it.name}'" }
//            serviceToFileDescriptor[it.name] = ""
            request.onNext(ServerReflectionRequest.newBuilder()
                .setFileContainingSymbol(it.name)
                .build())
        }

        val flow2 = responseFlow
            .filter { it.messageResponseCase == ServerReflectionResponse.MessageResponseCase.FILE_DESCRIPTOR_RESPONSE }
            .map {
//            log.d { "resp [${it.originalRequest.fileContainingSymbol}] = $it" }

                rawFileDescriptors += it.fileDescriptorResponse.fileDescriptorProtoList.map { it.toByteArray() }
                fileDescriptorByServiceNameByProtoFilename[it.originalRequest.fileContainingSymbol] =
                    it.fileDescriptorResponse.fileDescriptorProtoList
                        .map { FileDescriptorProto.parseFrom(it) }
                        .onEach { log.d { "fd = $it" } }
                        .associateBy { it.name }

                latch.decrement()
            }
            .launchIn(CoroutineScope(Dispatchers.IO))
        latch.await()
        flow2.cancel() // release resource

        val methods = fileDescriptorByServiceNameByProtoFilename.flatMap { (serviceName, fileDescriptors) ->
            fileDescriptors.flatMap { (k, v) ->
                v.serviceList.flatMap {
                    val serviceFullName = "${v.`package`}.${it.name}"
                    it.methodList.map { GrpcMethod(serviceFullName, it.name, it.clientStreaming, it.serverStreaming) }
                }
            }
        }
            .distinct()
//            .groupBy { it.serviceFullName }
//            .mapValues { it.value.distinct() }

        log.d { "Fetch spec result:\n$methods" }

        log.d { "done" }

        return GrpcApiSpec(
            id = uuidString(),
            name = name,
            methods = methods,
            rawFileDescriptors = rawFileDescriptors.distinct(),
            source = GrpcApiSpec.Source.Reflection,
            isActive = true,
            updateTime = KInstant.now(),
        )
    }

}

fun hostFromUrl(url: String): String {
    return try {
        val uri = URI.create(url)
        "${uri.host}:${uri.port}"
    } catch (_: Throwable) {
        ""
    }
}
