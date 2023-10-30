package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.OutputStream

class InspectOutputStream(val stream: OutputStream, val channel: Channel<Pair<KInstant, ByteArray>>) : OutputStream() {
    override fun write(b: Int) {
        synchronized(this) {
            stream.write(b)
            runBlocking {
                channel.send(KInstant.now() to byteArrayOf(b.toByte()))
            }
        }
    }

    override fun write(b: ByteArray) {
        synchronized(this) {
            stream.write(b)
            runBlocking {
                channel.send(KInstant.now() to b)
            }
        }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        synchronized(this) {
            stream.write(b, off, len)
            runBlocking {
                channel.send(KInstant.now() to b.copyOfRange(off, off + len))
            }
        }
    }
}
