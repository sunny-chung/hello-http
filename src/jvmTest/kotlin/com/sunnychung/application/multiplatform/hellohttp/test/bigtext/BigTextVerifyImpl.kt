package com.sunnychung.application.multiplatform.hellohttp.test.bigtext

import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigText
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.BigTextImpl
import com.sunnychung.application.multiplatform.hellohttp.ux.bigtext.InefficientBigText

internal class BigTextVerifyImpl internal constructor(chunkSize: Int = -1) : BigText {
    val bigTextImpl = if (chunkSize > 0) BigTextImpl(chunkSize) else BigTextImpl()
    val stringImpl = InefficientBigText("")

    val tree = bigTextImpl.tree
    val buffers = bigTextImpl.buffers

    override val length: Int
        get() {
            val l = bigTextImpl.length
            val tl = stringImpl.length
            assert(l == tl) { "length expected $tl, actual $l" }
            return l
        }

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
        stringImpl.append(text)
        verify()
        return r
    }

    override fun insertAt(pos: Int, text: String): Int {
        println("insert $pos, ${text.length}")
        val r = bigTextImpl.insertAt(pos, text)
        stringImpl.insertAt(pos, text)
        verify()
        return r
    }

    override fun delete(start: Int, endExclusive: Int): Int {
        println("delete $start ..< $endExclusive")
        var r: Int = 0
        printDebugIfError {
            r = bigTextImpl.delete(start, endExclusive)
            stringImpl.delete(start, endExclusive)
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
