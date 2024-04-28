package com.sunnychung.application.multiplatform.hellohttp.testserver.controller.grpc

import com.google.protobuf.Empty
import com.sunnychung.grpc.services.MyServiceGrpcKt
import com.sunnychung.grpc.types.IntResource
import com.sunnychung.grpc.types.StringResource
import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
import net.devh.boot.grpc.server.service.GrpcService
import sunnychung.grpc.types.Types.Name

@GrpcService
class MyServiceController : MyServiceGrpcKt.MyServiceCoroutineImplBase() {

    override suspend fun hi(request: Empty): StringResource {
        return StringResource.newBuilder()
            .setData("Hello HTTP!")
            .build()
    }

    override suspend fun sayHello(request: Name): StringResource {
        delay(3000)
        return StringResource.newBuilder()
            .setData("Hello HTTP, ${request.name}!")
            .build()
    }

    override suspend fun error(request: IntResource): IntResource {
        throw StatusRuntimeException(Status.fromCodeValue(request.data))
    }

    override suspend fun clientStream(requests: Flow<IntResource>): IntResource {
        val sum = requests.map { it.data }
            .reduce { accumulator, value -> accumulator + value }
        return IntResource.newBuilder()
            .setData(sum)
            .build()
    }

    override fun serverStream(request: IntResource): Flow<IntResource> {
        val count = request.data
        return (1..count).asFlow()
            .map {
                delay(1000)
                IntResource.newBuilder().setData(it).build()
            }
    }

    override fun biStream(requests: Flow<IntResource>): Flow<IntResource> {
        return requests.map {
            IntResource.newBuilder().setData(it.data + 100).build()
        }
    }
}
