package com.sunnychung.application.multiplatform.hellohttp.util

fun formatByteSize(size: Long): String {
    return if (size >= 10 * 1024L * 1024L) {
        "${"%.1f".format(size / 1024.0 / 1024.0)} MB"
    } else if (size >= 10 * 1024L) {
        "${"%.1f".format(size / 1024.0)} KB"
    } else {
        "${size} B"
    }
}
