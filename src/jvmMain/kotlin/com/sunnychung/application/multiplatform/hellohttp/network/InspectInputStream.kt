package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import java.io.FilterInputStream
import java.io.InputStream

class InspectInputStream(val stream: InputStream, val channel: MutableSharedFlow<Pair<KInstant, ByteArray>>) : FilterInputStream(stream) {
    override fun read(): Int {
        synchronized(this) {
            log.d { "read1" }
            val b = super.read()
            runBlocking {
                channel.emit(KInstant.now() to byteArrayOf(b.toByte()))
            }
            return b
        }
    }

    override fun read(b: ByteArray): Int {
        synchronized(this) {
            log.d { "read2" }
            val length = super.read(b)
            runBlocking {
                channel.emit(KInstant.now() to b.copyOfRange(0, length))
            }
            return length
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        synchronized(this) {
            log.d { "read3 $off $len" }
            val length = super.read(b, off, len)
            if (length > 0) {
                runBlocking {
                    channel.emit(KInstant.now() to b.copyOfRange(off, off + length))
                }
            }
            return length
        }
    }
}
