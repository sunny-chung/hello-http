package com.sunnychung.application.multiplatform.hellohttp.error

class PostflightError(val variable: String, cause: Throwable) :
    Exception("Variable '$variable': ${cause.message ?: "No error message available"}", cause)
