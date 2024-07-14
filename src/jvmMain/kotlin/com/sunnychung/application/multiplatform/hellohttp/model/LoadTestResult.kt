package com.sunnychung.application.multiplatform.hellohttp.model

import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.sunnychung.application.multiplatform.hellohttp.extension.atPercent
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.serializer.KInstantAsLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Collections
import java.util.SortedMap
import java.util.SortedSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * The data structure should support:
 * - Number of unresponsive requests
 * - Number of responses per status code
 * - Histogram of latencies (max 1001 samples), including min, max
 * - Average latency
 * - Success/Error rate over response time
 * - Average/Min/Max/Median/90%/95%/99% Latencies over response time
 * - Number of pending requests over time
 */
data class LoadTestResult(
    val numRequestsSent: Int,
    val numResponses: Int,
    val numResponsesByStatusCode: MutableMap<Int, Int> = ConcurrentHashMap(),
    val latenciesMsHistogram: List<Long>,
    val averageLatencyMs: Double,
    val numCatagorizedResponsesOverTime: SortedMap<Long, Map<LoadTestResponse.Category, Int>>,
    val latenciesMsOverTime: SortedMap<Long, SingleStatistic>,
    val startAt: KInstantAsLong,
    val endAt: KInstantAsLong,

    val input: LoadTestInput,
) {
    val lock = Mutex()

    data class SingleStatistic(
        val min: Double,
        val max: Double,
        val average: Double,
        val median: Double,
        val at90Percent: Double,
        val at95Percent: Double,
        val at99Percent: Double,
    )
}

@Deprecated("")
data class LoadTestResponse(
    val statusCode: Int,
    val category: Category,
    val latencyMs: Long,
    val startAt: KInstantAsLong,
    val endAt: KInstantAsLong,
) {
    enum class Category {
        Success, ServerError, ClientError,
    }
}

class LongDuplicateContainer(val value: Long) : Comparable<LongDuplicateContainer> {
    companion object {
        private val counter = AtomicLong(0)
    }

    private val uniqueId = counter.incrementAndGet()

    // never return 0 (equals) so that we can have duplicated values in the sorted set
    override fun compareTo(other: LongDuplicateContainer): Int {
        return if (value < other.value) {
            -1
        } else {
            1
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LongDuplicateContainer) return false

        if (value != other.value) return false
        if (uniqueId != other.uniqueId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + uniqueId.hashCode()
        return result
    }


}

class LoadTestState(val input: LoadTestInput, val callId: String, val startAt: KInstant, val categorizer: (UserResponse) -> LoadTestResponse.Category) {
    var numRequestsSent: AtomicInteger = AtomicInteger(0)
    val latenciesMs: SortedSet<LongDuplicateContainer> = Collections.synchronizedSortedSet(sortedSetOf())
    val responsesOverResponseTime: SortedSet<UserResponseByResponseTime> = Collections.synchronizedSortedSet(sortedSetOf())

    val lock = Mutex()

    suspend fun toResult(intervalMs: Long): LoadTestResult {
        val now = KInstant.now()
        return lock.withLock {
            val responsesOverResponseTime = responsesOverResponseTime.toList()
            val latenciesMs = latenciesMs.toList()

            val responsesGroupedByInterval = responsesOverResponseTime
                .groupBy({(it.endAt!!.toEpochMilliseconds() / intervalMs) * intervalMs}, { it.userResponse })
//            log.v { "responsesOverResponseTime = ${jsonMapper().writeValueAsString(responsesOverResponseTime)}" }
//            log.v { "responsesGroupedByInterval = ${jsonMapper().writeValueAsString(responsesGroupedByInterval)}" }
            val latencies = latenciesMs.map { it.value }
            LoadTestResult(
                numRequestsSent = numRequestsSent.get(),
                numResponses = latenciesMs.size,
                latenciesMsHistogram = if (latencies.size <= 1001) {
                    latencies
                } else {
                    val latenciesMs = latencies
                    (0 .. 999).map { i -> latenciesMs[(i.toDouble() * 1000.0 / latenciesMs.size.toDouble()).roundToInt()] }.plus(latenciesMs.last()).toList()
                },
                averageLatencyMs = latencies.average(),
                numCatagorizedResponsesOverTime = responsesGroupedByInterval
                    .mapValues { (_, responses) -> responses.groupBy { categorizer(it) }.mapValues { it.value.size } }
                    .toSortedMap(),
                latenciesMsOverTime = responsesGroupedByInterval
                    .mapValues { (_, result) ->
                        val latencies = result.asSequence().mapNotNull {
                            ((it.endAt ?: return@mapNotNull null) - (it.startAt ?: return@mapNotNull null)).toMilliseconds()
                        }.toList()
                        LoadTestResult.SingleStatistic(
                            min = latencies.min().toDouble(),
                            max = latencies.max().toDouble(),
                            average = latencies.average(),
                            median = latencies.atPercent(50),
                            at90Percent = latencies.atPercent(90),
                            at95Percent = latencies.atPercent(95),
                            at99Percent = latencies.atPercent(99),
                        )
                    }
                    .toSortedMap(),
                startAt = startAt,
                endAt = now,
                input = input,
            )
        }
    }
}

data class LoadTestInput(
    val numConcurrent: Int,
    val timeout: KDuration,
    val intendedDuration: KDuration,

    val requestId: String = "", // corresponding id of UserRequest
    val requestExampleId: String = "", // corresponding id of UserRequestExample
    val application: ProtocolApplication = ProtocolApplication.Http,
    val requestData: RequestData? = null,
)
