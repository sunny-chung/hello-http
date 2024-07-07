package com.sunnychung.application.multiplatform.hellohttp.test.payload

class RequestData(
    val path: String,
    val method: String,
    val headers: List<Parameter>,
    val queryParameters: List<Parameter>,
    val formData: List<Parameter>,
    val multiparts: List<PartData>,
    val body: String?,
)
