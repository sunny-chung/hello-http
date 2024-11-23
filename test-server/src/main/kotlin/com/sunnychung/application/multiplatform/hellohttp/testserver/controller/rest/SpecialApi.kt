package com.sunnychung.application.multiplatform.hellohttp.testserver.controller.rest

import com.sunnychung.application.multiplatform.hellohttp.test.payload.SomeDataModel
import com.sunnychung.application.multiplatform.hellohttp.test.payload.SomeDataModels
import com.sunnychung.application.multiplatform.hellohttp.test.payload.SomeInput
import com.sunnychung.application.multiplatform.hellohttp.test.payload.SomeOutput
import kotlinx.coroutines.delay
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import kotlin.random.Random
import kotlin.random.nextInt

@RequestMapping("rest")
@RestController
class SpecialApi {

    @RequestMapping("earlyError")
    fun earlyError() {
        throw RuntimeException("Early error")
    }

    @RequestMapping("wait")
    suspend fun wait(@RequestParam ms: Long): String {
        delay(ms)
        return "OK"
    }

    @RequestMapping("bigDocument", produces = ["text/plain"])
    suspend fun bigDocument(@RequestParam size: Int): ByteArray {
        return ByteArray(size) { i ->
            if (i % 30 != 29) {
                ('0' + (i % 10)).code.toByte()
            } else {
                '\n'.code.toByte()
            }
        }
    }

    @RequestMapping("bigJson")
    suspend fun bigJson(@RequestParam size: Int): SomeDataModels {
        fun Random.nextChar(): Char {
            return nextInt(0 .. 26).let {
                when (it) {
                    in 0 ..< 26 -> ('a'.code + it).toChar()
                    26 -> ' '
                    else -> throw IllegalArgumentException()
                }
            }
        }

        val rand = Random
        return (0 ..< size).map { i ->
            SomeDataModel(
                UUID.randomUUID().toString(),
                i,
                (16 .. 23).joinToString("") { rand.nextChar().toString() },
                rand.nextDouble(),
                rand.nextDouble(),
                (8 .. 15).map { (6 .. 22).joinToString("") { rand.nextChar().toString() } },
                (3 .. 8).map { SomeInput("Input #$it", rand.nextInt(), rand.nextInt()) },
                SomeOutput("Output for $i", rand.nextInt()),
                (600 .. 1300).joinToString("") { rand.nextChar().toString() },
            )
        }.let { SomeDataModels(it) }
    }
}
