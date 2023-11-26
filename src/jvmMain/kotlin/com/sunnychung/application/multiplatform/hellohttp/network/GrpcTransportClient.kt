package com.sunnychung.application.multiplatform.hellohttp.network

import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.FileDescriptorProtoKt
import com.sunnychung.application.multiplatform.hellohttp.helper.CountDownLatch
import com.sunnychung.application.multiplatform.hellohttp.manager.NetworkClientManager
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcApiSpec
import com.sunnychung.application.multiplatform.hellohttp.model.GrpcMethod
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.network.util.flowAndStreamObserver
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.reflection.v1alpha.ServerReflectionGrpc
import io.grpc.reflection.v1alpha.ServerReflectionRequest
import io.grpc.reflection.v1alpha.ServerReflectionResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import java.net.URI
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class GrpcTransportClient(networkClientManager: NetworkClientManager) : AbstractTransportClient(networkClientManager) {
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
        val channel = ManagedChannelBuilder.forAddress(uri.host, uri.port)
            .apply {
                if (sslConfig.isInsecure == true) {
                    usePlaintext()
                }
            }
            .build()
        TODO()
    }

    suspend fun fetchServiceSpec(
        url: String,
        sslConfig: SslConfig
    ): GrpcApiSpec {
        val uri = URI.create(url)
        val channel = ManagedChannelBuilder.forAddress(uri.host, uri.port)
            .apply {
                if (sslConfig.isInsecure == true) {
                    usePlaintext()
                }
            }
            .build()
        return fetchServiceSpec("${uri.host}:${uri.port}", channel)
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
            rawFileDescriptors = rawFileDescriptors,
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
