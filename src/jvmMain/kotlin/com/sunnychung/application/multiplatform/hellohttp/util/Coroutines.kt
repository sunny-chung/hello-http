package com.sunnychung.application.multiplatform.hellohttp.util

fun <I, R> suspended(lambda: suspend (I) -> R): suspend (I) -> R = lambda
