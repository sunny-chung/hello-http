package com.sunnychung.application.multiplatform.hellohttp.util

fun ByteArray.copyStartFromIndex(start: Int): ByteArray {
    return copyOfRange(start, this.size)
}
