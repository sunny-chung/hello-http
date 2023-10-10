package com.sunnychung.application.multiplatform.hellohttp.util

fun String?.emptyToNull(): String? {
    return if (this == "") null else this
}
