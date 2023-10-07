package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.FilterInputStream
import java.io.InputStream

class InspectInputStream(val stream: InputStream, val channel: Channel<Pair<KInstant, ByteArray>>) : FilterInputStream(stream) {
    override fun read(): Int {
        synchronized(this) {
            val b = super.read()
            runBlocking {
                channel.send(KInstant.now() to byteArrayOf(b.toByte()))
            }
            return b
        }
    }

    override fun read(b: ByteArray): Int {
        synchronized(this) {
            val a = super.read(b)
            runBlocking {
                channel.send(KInstant.now() to b)
            }
            return a
        }
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        synchronized(this) {
            val a = super.read(b, off, len)
            runBlocking {
                channel.send(KInstant.now() to b.copyOfRange(off, off + len))
            }
            return a
        }
    }
}
