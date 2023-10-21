package com.sunnychung.application.multiplatform.hellohttp.extension

fun <T> T.`if`(condition: (T) -> Boolean): T? {
    return if (condition(this)) this else null
}
