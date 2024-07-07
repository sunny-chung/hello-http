package com.sunnychung.application.multiplatform.hellohttp.testserver.controller.graphql

import com.sunnychung.application.multiplatform.hellohttp.test.payload.SomeInput
import com.sunnychung.application.multiplatform.hellohttp.test.payload.SomeOutput
import com.sunnychung.application.multiplatform.hellohttp.test.payload.SubData
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.reactor.asFlux
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SubscriptionMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import java.time.ZonedDateTime
import java.util.concurrent.atomic.AtomicInteger

@Controller
class GraphqlApi {

    @QueryMapping
    fun sum(@Argument input: SomeInput): SomeOutput {
        with(input) {
            return SomeOutput(a, b + c)
        }
    }

    @SubscriptionMapping
    fun interval(@Argument seconds: Int, @Argument stopAt: Int?, @Argument errorAt: Int?): Flux<SubData> {
        val i = AtomicInteger()
        return flow {
            emit(SubData(id = 0, instant = ZonedDateTime.now().toString()))
            while (currentCoroutineContext().isActive) {
                delay(seconds * 1000L)
                emit(SubData(id = i.addAndGet(1), instant = ZonedDateTime.now().toString()))
                if (errorAt != null && i.get() >= errorAt) {
                    throw RuntimeException("Error at ${i.get()}")
                } else if (stopAt != null && i.get() >= stopAt) {
                    break
                }
            }
        }.asFlux()
    }
}
