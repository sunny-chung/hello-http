package com.sunnychung.application.multiplatform.hellohttp.network

import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.io.FilterOutputStream
import java.io.OutputStream

class InspectOutputStream(val stream: OutputStream, val channel: Channel<Pair<KInstant, ByteArray>>) : FilterOutputStream(stream) {
    override fun write(b: Int) {
        synchronized(this) {
            super.write(b)
            runBlocking {
                channel.send(KInstant.now() to byteArrayOf(b.toByte()))
            }
        }
    }

    override fun write(b: ByteArray) {
        super.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        super.write(b, off, len)
    }
}
