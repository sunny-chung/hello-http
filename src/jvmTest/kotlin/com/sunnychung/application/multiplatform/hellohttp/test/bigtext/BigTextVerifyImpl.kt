package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextNodeValue
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextTransformerImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.InefficientBigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.LengthTree
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.TextBuffer
import java.util.TreeMap

internal class BigTextVerifyImpl(bigTextImpl: BigTextImpl) : BigText {
    val bigTextImpl: BigTextImpl = bigTextImpl
    val stringImpl = InefficientBigText("")

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
    val transformOffsetsByPosition = TreeMap<Int, Int>()

    override val length: Int
        get() {
            val l = bigTextImpl.length
            val tl = stringImpl.length
            assert(l == tl) { "length expected $tl, actual $l" }
            return l
        }

    val originalLength: Int
        get() = length - transformOffsetsByPosition.values.sum()

    override fun buildString(): String {
        val r = bigTextImpl.buildString()
        val tr = stringImpl.buildString()
        assert(r == tr) { "fullString expected $tr, actual $r" }
        return r
    }

    override fun substring(start: Int, endExclusive: Int): String {
        val r = bigTextImpl.substring(start, endExclusive)
        val tr = stringImpl.substring(start, endExclusive)
        assert(r == tr) { "substring expected $tr, actual $r" }
        return r
    }

    override fun append(text: String): Int {
        println("append ${text.length}")
        val r = bigTextImpl.append(text)
        if (isTransform) {
            val pos = stringImpl.length
            transformOffsetsByPosition[pos] = (transformOffsetsByPosition[pos] ?: 0) + text.length
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
        }
        val pos = pos + transformOffsetsByPosition.subMap(0, pos).values.sum().also {
            println("VerifyImpl pos $pos offset $it")
        }
        stringImpl.insertAt(pos, text)
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
            }
            val offset = transformOffsetsByPosition.subMap(0, start).values.sum()
            stringImpl.delete(offset + start, offset + endExclusive)
        }
        verify()
        return r
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
        printDebugIfError(label) {
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
