package com.sunnychung.application.multiplatform.hellohttp.network.util

import com.sunnychung.application.multiplatform.hellohttp.network.Http1Payload
import com.sunnychung.application.multiplatform.hellohttp.network.RawPayload
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import java.io.OutputStream

class InspectOutputStream(val stream: OutputStream, val channel: MutableSharedFlow<RawPayload>) : OutputStream() {
    override fun write(b: Int) {
        synchronized(this) {
            stream.write(b)
            runBlocking {
                channel.emit(Http1Payload(instant = KInstant.now(), payload = byteArrayOf(b.toByte())))
            }
        }
    }

    override fun write(b: ByteArray) {
        synchronized(this) {
            stream.write(b)
            runBlocking {
                channel.emit(Http1Payload(instant = KInstant.now(), payload = b))
            }
        }
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        synchronized(this) {
            stream.write(b, off, len)
            runBlocking {
                channel.emit(Http1Payload(instant = KInstant.now(), payload = b.copyOfRange(off, off + len)))
            }
        }
    }
}
