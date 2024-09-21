package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.extension.length
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextNodeValue
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformOffsetMapping
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformerImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.InefficientBigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.LengthTree
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.TextBuffer
import java.util.TreeMap
import kotlin.test.assertEquals

internal class BigTextVerifyImpl(bigTextImpl: BigTextImpl) : BigText {
    val bigTextImpl: BigTextImpl = bigTextImpl
    val stringImpl = InefficientBigText("")
    var isDebug = true

    init {
        this.stringImpl.append(bigTextImpl.buildString())
    }

    internal constructor(chunkSize: Int = -1) : this(
        if (chunkSize > 0) BigTextImpl(chunkSize) else BigTextImpl()
    )

    val tree: LengthTree<BigTextNodeValue>
        get() = bigTextImpl.tree
    val buffers: MutableList<TextBuffer>
        get() = bigTextImpl.buffers

    val isTransform = bigTextImpl is BigTextTransformerImpl
    private val transformOffsetsByPosition = TreeMap<Int, Int>()
    private val transformOffsetsMappingByPosition = TreeMap<Int, Int>() // temporary dirty solution
    private val transformOps = mutableListOf<TransformOp>()

    override val length: Int
        get() {
            val l = bigTextImpl.length
            val tl = stringImpl.length
            assert(l == tl) { "length expected $tl, actual $l" }
            return l
        }

    val originalLength: Int
        get() = length - transformOffsetsByPosition.values.sum()

    private data class TransformOp(val originalRange: IntRange, val offsetMapping: BigTextTransformOffsetMapping)

    override fun buildString(): String {
        val r = bigTextImpl.buildString()
        val tr = stringImpl.buildString()
        assertEquals(tr, r, "fullString mismatch")
        return r
    }

    override fun substring(start: Int, endExclusive: Int): String {
        val r = bigTextImpl.substring(start, endExclusive)
        val tr = stringImpl.substring(start, endExclusive)
        assertEquals(tr, r, "substring mismatch")
        return r
    }

    override fun append(text: String): Int {
        println("append ${text.length}")
        val r = bigTextImpl.append(text)
        if (isTransform) {
            val pos = stringImpl.length
            transformOffsetsByPosition[pos] = (transformOffsetsByPosition[pos] ?: 0) + text.length
            transformOffsetsMappingByPosition[pos] = (transformOffsetsMappingByPosition[pos] ?: 0) + text.length
        }
        stringImpl.append(text)
        verify()
        return r
    }

    override fun insertAt(pos: Int, text: String): Int {
        println("insert $pos, ${text.length}")
        val r = bigTextImpl.insertAt(pos, text)
        if (isTransform) {
            transformOffsetsByPosition[pos] = (transformOffsetsByPosition[pos] ?: 0) + text.length
            transformOffsetsMappingByPosition[pos] = (transformOffsetsMappingByPosition[pos] ?: 0) + text.length
        }
        val pos = pos + transformOffsetsByPosition.subMap(0, pos).values.sum().also {
            println("VerifyImpl pos $pos offset $it")
        }
        stringImpl.insertAt(pos, text)
//        transformOps += TransformOp(pos until pos + text.length, BigTextTransformOffsetMapping.WholeBlock)
        verify()
        return r
    }

    override fun delete(start: Int, endExclusive: Int): Int {
        println("delete $start ..< $endExclusive")
        var r: Int = 0
        printDebugIfError {
            r = bigTextImpl.delete(start, endExclusive)
            if (isTransform) {
                transformOffsetsByPosition[start] = (transformOffsetsByPosition[start] ?: 0) - (endExclusive - start)
                (start + 1 .. endExclusive).forEach { i ->
                    transformOffsetsMappingByPosition[i] = (transformOffsetsMappingByPosition[i] ?: 0) - 1
                }
            }
            val offset = transformOffsetsByPosition.subMap(0, start).values.sum()
            stringImpl.delete(offset + start, offset + endExclusive)
        }
        transformOps += TransformOp(start until endExclusive, BigTextTransformOffsetMapping.WholeBlock)
        println("new len = ${bigTextImpl.length}")
        verify()
        return r
    }

    override fun replace(range: IntRange, text: String) {
        replace(range, text, BigTextTransformOffsetMapping.Incremental)
    }

    fun replace(range: IntRange, text: String, offsetMapping: BigTextTransformOffsetMapping) {
        println("replace $range -> ${text.length}")
        var r: Int = 0
        printDebugIfError {
            if (isTransform) {
                (bigTextImpl as BigTextTransformerImpl).replace(range, text, offsetMapping)
                when (offsetMapping) {
                    BigTextTransformOffsetMapping.WholeBlock -> {
                        transformOffsetsByPosition[range.start] = (transformOffsetsByPosition[range.start] ?: 0) - range.length + text.length
//                        transformOffsetsByPosition[range.start] = (transformOffsetsByPosition[range.start] ?: 0) + text.length
                        transformOffsetsMappingByPosition[range.start] = (transformOffsetsMappingByPosition[range.start] ?: 0) + text.length
//                        if (text.length < range.length) {
//                            (range.start + text.length .. range.endInclusive + 1).forEach { i ->
//                                transformOffsetsByPosition[i] = (transformOffsetsByPosition[i] ?: 0) - 1
//                            }
//                        }
                        (range.start + 1 .. range.endInclusive + 1).forEach { i ->
//                            transformOffsetsByPosition[i] = (transformOffsetsByPosition[i] ?: 0) - 1
                            transformOffsetsMappingByPosition[i] = (transformOffsetsMappingByPosition[i] ?: 0) - 1
                        }
                    }
                    BigTextTransformOffsetMapping.Incremental -> {
//                        (1 .. minOf(text.length, range.length)).forEach { i ->
//                            transformOffsetsByPosition[i] = (transformOffsetsByPosition[i] ?: 0) + 1
//                        }
                        if (text.length < range.length) {
                            (range.start + text.length + 1 .. range.endInclusive + 1).forEach { i ->
                                transformOffsetsByPosition[i] = (transformOffsetsByPosition[i] ?: 0) - 1
                                transformOffsetsMappingByPosition[i] = (transformOffsetsMappingByPosition[i] ?: 0) - 1
                            }
                        } else if (text.length > range.length) {
                            transformOffsetsByPosition[range.endInclusive] =
                                (transformOffsetsByPosition[range.endInclusive] ?: 0) + text.length - range.length
                            transformOffsetsMappingByPosition[range.endInclusive + 1] =
                                (transformOffsetsMappingByPosition[range.endInclusive + 1] ?: 0) + text.length - range.length
                        }
                    }
                }
            } else {
                bigTextImpl.replace(range, text)
            }
            val offset = transformOffsetsByPosition.subMap(0, range.start).values.sum()
            stringImpl.replace(range.start + offset .. range.endInclusive + offset, text)

            transformOps += TransformOp(range, offsetMapping)
        }
        println("new len = ${bigTextImpl.length}")
        verify()
//        return r
    }

    fun verifyPositionCalculation() {
        val t = bigTextImpl as BigTextTransformerImpl
        val originalLength = originalLength
        if (isDebug) {
            println("Original:    ${t.delegate.buildString()}")
            println("Transformed: ${t.buildString()}")
        }
        val transformedLength = t.length
        (0 .. originalLength).forEach { i ->
            val expected = findTransformedPositionByOriginalPosition(i)
            val actual = t.findTransformedPositionByOriginalPosition(i)
            if (isDebug) {
                println("Original pos $i to transformed = $actual")
            }
            assertEquals(expected, actual, "Original pos $i to transformed, expected = $expected, but actual = $actual")
        }
        (0 .. transformedLength).forEach { i ->
            val expected = findOriginalPositionByTransformedPosition(i)
            val actual = t.findOriginalPositionByTransformedPosition(i)
            if (isDebug) {
                println("Transformed pos $i to original = $actual")
            }
            assertEquals(expected, actual, "Transformed pos $i to original, expected = $expected, but actual = $actual")
        }
    }

    fun findTransformedPositionByOriginalPositionRaw(originalPosition: Int): Pair<Int, Int> {
        val subMap = transformOffsetsMappingByPosition.subMap(0, true, originalPosition, true)
//        val offset = subMap.values.sum() + (subMap.lastEntry()?.let {
////            if (it.value < 0 && originalPosition in it.key until it.key - it.value) {
////                - it.value /* subtract the effect brought by sum() first */ +
////                    maxOf(it.value, it.key - originalPosition /* incremental offset */)
////            } else {
//                null
////            }
//        } ?: 0)
        val offsets = subMap.values.sum() to (subMap[originalPosition] ?: 0)
        return offsets
    }

    fun findTransformedPositionByOriginalPosition(originalPosition: Int): Int {
        val o = findTransformedPositionByOriginalPositionRaw(originalPosition)
        return originalPosition + o.first //+ o.second
    }

    fun findOriginalPositionByTransformedPosition(transformedPosition: Int): Int {
        var i = 0
        val originalLength = originalLength
        var result: Int? = null
        var start: Int? = null
        while (i <= originalLength) {
            val (offsetSum, offsetLast) = findTransformedPositionByOriginalPositionRaw(i)
            val mapped = i + offsetSum
            if (mapped > transformedPosition) {
                if (result != null) {
                    return transformOps.firstOrNull { it.offsetMapping == BigTextTransformOffsetMapping.WholeBlock && it.originalRange.first == start }
                        ?.originalRange
                        ?.endInclusive
                        ?.let { maxOf(it + 1, result!!) }
                        ?: result
                }
                return i
            } else if (mapped == transformedPosition) {
                result = i
                if (start == null) {
                    start = i
                }
            }
            ++i
        }
        result?.let { return it }
        throw IndexOutOfBoundsException("Transformed position $transformedPosition not found")
    }

    override fun hashCode(): Int {
        val r = bigTextImpl.hashCode()
        val tr = stringImpl.hashCode()
        assert(r == tr) { "hashCode expected $tr, actual $r" }
        return r
    }

    override fun equals(other: Any?): Boolean {
        val r = bigTextImpl.equals(other)
        val tr = stringImpl.equals(other)
        assert(r == tr) { "equals expected $tr, actual $r" }
        return r
    }

    fun verify(label: String = "") {
        printDebugIfError(label.ifEmpty { "ERROR" }) {
            length
            buildString()
        }
    }

    fun printDebugIfError(label: String = "ERROR", operation: () -> Unit) {
        try {
            operation()
        } catch (e: Throwable) {
            printDebug(label)
            throw e
        }
    }

    fun printDebug(label: String = "") = bigTextImpl.printDebug(label)

    fun inspect(label: String = "") = bigTextImpl.inspect(label)
}
