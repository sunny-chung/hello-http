package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.MutableLoggerConfig
import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.util.JvmLogger
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import org.junit.jupiter.api.Disabled
import kotlin.random.Random
import kotlin.test.Test

private val log = Logger(object : MutableLoggerConfig {
    override var logWriterList: List<LogWriter> = listOf(JvmLogger())
    override var minSeverity: Severity = Severity.Info
}, tag = "BigTextImplBenchmarkTest")

@Disabled
class BigTextImplBenchmarkTest {

    private fun chunkSizes() = listOf(64, 1024, 64 * 1024, 256 * 1024, 1024 * 1024, 2 * 1024 * 1024, 4 * 1024 * 1024)

    private fun measureOne(operation: () -> Unit): KDuration {
        val startInstant = KInstant.now()
        operation()
        val endInstant = KInstant.now()
        return endInstant - startInstant
    }

    private fun benchmark(label: String, testOperation: (BigTextImpl, String) -> Unit) {
        chunkSizes().forEach { chunkSize ->
            log.i("-".repeat(29))
            val logHeader = "[$label] [chunkSize=$chunkSize]"
            log.i("$logHeader Start")
            val testDuration = measureOne {
                val text = BigTextImpl(chunkSize = chunkSize)
                testOperation(text, logHeader)
            }
            log.i("$logHeader End. ${testDuration.millis / 1000.0}s")
            log.i("-".repeat(29))
        }
    }

    private fun buildStringOfLength(length: Int): String {
        return ('a' + Random.nextInt(26)).toString().repeat(length)
    }

    @Test
    fun appendShort() {
        benchmark("appendShort") { text, logHeader ->
            repeat(10_000_000) {
                val length = Random.nextInt(0, 40)
                text.append(buildStringOfLength(length))
            }
            log.i("$logHeader Length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun appendLong() {
        val longStrings = listOf(2930851, 1314698, 16_526_034).map { buildStringOfLength(it) }
        benchmark("appendLong") { text, logHeader ->
            repeat(200) {
                text.append(longStrings[Random.nextInt(0, 3)])
            }
            log.i("$logHeader Length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun appendGiant() {
        val longStrings = listOf(129_730_851, 103_214_698, 156_526_034).map { buildStringOfLength(it) }
        benchmark("appendGiant") { text, logHeader ->
            repeat(10) {
                text.append(longStrings[Random.nextInt(0, 3)])
            }
            log.i("$logHeader Length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun appendMixedLengths() {
        benchmark("appendMixedLengths") { text, logHeader ->
            repeat(1500) {
                val length = when (Random.nextInt(100)) {
                    in 0 .. 34 -> Random.nextInt(0, 20)
                    in 35 .. 64 -> Random.nextInt(20, 500)
                    in 65 .. 84 -> Random.nextInt(500, 6000)
                    in 85 .. 94 -> Random.nextInt(6000, 120_000)
                    in 95 .. 98 -> Random.nextInt(120_000, 1_600_000)
                    in 99 .. 99 -> Random.nextInt(1_600_000, 150_000_000)
                    else -> throw IllegalStateException()
                }
                text.append(buildStringOfLength(length))
            }
            log.i("$logHeader Length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun insertShort() {
        benchmark("insertShort") { text, logHeader ->
            repeat(10_000_000) {
                val length = Random.nextInt(0, 40)
                text.insertAt(random(0, text.length), buildStringOfLength(length))
            }
            log.i("$logHeader Length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun insertLong() {
        val longStrings = listOf(2930851, 1314698, 16_526_034).map { buildStringOfLength(it) }
        benchmark("insertLong") { text, logHeader ->
            repeat(200) {
                text.insertAt(random(0, text.length), longStrings[Random.nextInt(0, 3)])
            }
            log.i("$logHeader Length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun insertGiant() {
        val longStrings = listOf(129_730_851, 103_214_698, 156_526_034).map { buildStringOfLength(it) }
        benchmark("insertGiant") { text, logHeader ->
            repeat(10) {
                text.insertAt(random(0, text.length), longStrings[Random.nextInt(0, 3)])
            }
            log.i("$logHeader Length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun insertMixedLengths() {
        benchmark("insertMixedLengths") { text, logHeader ->
            repeat(1500) {
                val length = when (Random.nextInt(100)) {
                    in 0 .. 34 -> Random.nextInt(0, 20)
                    in 35 .. 64 -> Random.nextInt(20, 500)
                    in 65 .. 84 -> Random.nextInt(500, 6000)
                    in 85 .. 94 -> Random.nextInt(6000, 120_000)
                    in 95 .. 98 -> Random.nextInt(120_000, 1_600_000)
                    in 99 .. 99 -> Random.nextInt(1_600_000, 150_000_000)
                    else -> throw IllegalStateException()
                }
                text.insertAt(random(0, text.length), buildStringOfLength(length))
            }
            log.i("$logHeader Length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun deleteShort() {
        benchmark("deleteShort") { text, logHeader ->
            text.append("a".repeat(1_234_567_890))
            repeat(10_000_000) {
                val length = Random.nextInt(0, 40)
                val pos = random(0, text.length)
                text.delete(pos, minOf(text.length, pos + length))
            }
            log.i("$logHeader Remain length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun deleteLong() {
        benchmark("deleteLong") { text, logHeader ->
            text.append("a".repeat(1_006_984_321))
            text.append("b".repeat(378_984_320))
            repeat(200) {
                val length = Random.nextInt(1314698, 6526034)
                val pos = random(0, text.length)
                text.delete(pos, minOf(text.length, pos + length))
            }
            log.i("$logHeader Remain length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun deleteGiant() {
        benchmark("deleteGiant") { text, logHeader ->
            text.append("a".repeat(1_006_984_324))
            text.append("b".repeat(378_984_320))
            repeat(10) {
                val length = Random.nextInt(103_214_698, 156_526_034)
                val pos = random(0, text.length)
                text.delete(pos, minOf(text.length, pos + length))
            }
            log.i("$logHeader Remain length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun deleteMixedLengths() {
        benchmark("deleteMixedLengths") { text, logHeader ->
            text.append("a".repeat(1_006_984_320))
            text.append("b".repeat(378_984_320))
            repeat(1500) {
                val length = when (Random.nextInt(100)) {
                    in 0 .. 34 -> Random.nextInt(0, 20)
                    in 35 .. 64 -> Random.nextInt(20, 500)
                    in 65 .. 84 -> Random.nextInt(500, 6000)
                    in 85 .. 94 -> Random.nextInt(6000, 120_000)
                    in 95 .. 98 -> Random.nextInt(120_000, 1_600_000)
                    in 99 .. 99 -> Random.nextInt(1_600_000, 150_000_000)
                    else -> throw IllegalStateException()
                }
                val pos = random(0, text.length)
                text.delete(pos, minOf(text.length, pos + length))
            }
            log.i("$logHeader Remain length = ${text.length}")
            log.i("$logHeader Node Count = ${text.tree.size()}")
        }
    }

    @Test
    fun randomMutateOperations() {
        benchmark("randomMutateOperations") { t, logHeader ->
            repeat(5000) {
                val length = when (Random.nextInt(100)) {
                    in 0 .. 34 -> Random.nextInt(0, 20)
                    in 35 .. 64 -> Random.nextInt(20, 500)
                    in 65 .. 84 -> Random.nextInt(500, 6000)
                    in 85 .. 94 -> Random.nextInt(6000, 120_000)
                    in 95 .. 98 -> Random.nextInt(120_000, 1_600_000)
                    in 99 .. 99 -> Random.nextInt(1_600_000, 150_000_000)
                    else -> throw IllegalStateException()
                }
                val newString = if (length > 0) {
                    val startChar: Char = if (it % 2 == 0) 'A' else 'a'
                    (0 until length - 1).asSequence().map { (startChar + it % 26).toString() }.joinToString("") + "|"
                } else {
                    ""
                }
                when (random(0, 15)) {
                    in 0..1 -> t.append(newString)
                    2 -> t.insertAt(t.length, newString)
                    3 -> t.insertAt(0, newString)
                    in 4..8 -> t.insertAt(random(0, t.length), newString)
                    in 9..11 -> if (t.length > 0) {
                        val p1 = random(0, t.length)
                        val p2 = minOf(t.length, p1 + random(0, t.length - p1)) // p1 + p2 <= t.length
                        t.delete(minOf(p1, p2), maxOf(p1, p2))
                    } else {
                        t.delete(0, 0)
                    }
                    12 -> t.delete(0, random(0, minOf(length, t.length))) // delete from start
                    13 -> t.delete(t.length - random(0, minOf(length, t.length)), t.length) // delete from end
                    14 -> t.delete(0, t.length) // delete whole string
                    else -> throw IllegalStateException()
                }
            }
            log.i("$logHeader Remain length = ${t.length}")
            log.i("$logHeader Node Count = ${t.tree.size()}")
        }
    }

    @Test
    fun length() {
        benchmark("length") { t, logHeader ->
            val durations = mutableListOf<KDuration>()
            repeat(5) {
                val text = BigTextImpl(t.chunkSize)
                text.append("a".repeat(random(1_006_984_321, 1_200_000_000)))
                var l = 0
                log.i("$logHeader Start evaluating length")
                durations += measureOne {
                    repeat(10_000_000) {
                        l = text.length
                    }
                }
                log.i("$logHeader Length = $l")
            }
            log.i("$logHeader Average: ${durations.map { it.millis }.average() / 1000.0}s")
        }
    }
}

private fun random(from: Int, toExclusive: Int): Int {
    if (toExclusive == from) {
        return 0
    }
    return Random.nextInt(from, toExclusive)
}
