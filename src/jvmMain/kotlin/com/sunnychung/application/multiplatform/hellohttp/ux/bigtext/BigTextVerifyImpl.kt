package com.sunnychung.application.multiplatform.hellohttp.ux.bigtext

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

    override fun fullString(): String {
        val r = bigTextImpl.fullString()
        val tr = stringImpl.fullString()
        assert(r == tr) { "fullString expected $tr, actual $r" }
        return r
    }

    override fun substring(start: Int, endExclusive: Int): String {
        val r = bigTextImpl.substring(start, endExclusive)
        val tr = stringImpl.substring(start, endExclusive)
        assert(r == tr) { "substring expected $tr, actual $r" }
        return r
    }

    override fun append(text: String) {
        println("append ${text.length}")
        bigTextImpl.append(text)
        stringImpl.append(text)
        verify()
    }

    override fun insertAt(pos: Int, text: String) {
        println("insert $pos, ${text.length}")
        bigTextImpl.insertAt(pos, text)
        stringImpl.insertAt(pos, text)
        verify()
    }

    override fun delete(start: Int, endExclusive: Int) {
        log.d { "delete $start ..< $endExclusive" }
        printDebugIfError {
            bigTextImpl.delete(start, endExclusive)
            stringImpl.delete(start, endExclusive)
        }
        verify()
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
            fullString()
        }
    }

    fun printDebugIfError(label: String = "", operation: () -> Unit) {
        try {
            operation()
        } catch (e: Throwable) {
            printDebug(label)
            throw e
        }
    }

    fun printDebug(label: String = "") = bigTextImpl.printDebug(label)
}
