package com.sunnychung.application.multiplatform.hellohttp.extension

import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request

fun UserRequest.toOkHttpRequest(exampleId: String, environment: Environment?): Request = withScope(exampleId, environment) {

    fun UserRequestBody.expandStringBody(): UserRequestBody {
        if (this is StringBody) {
            return StringBody(value.expandVariables())
        }
        return this
    }

    var b = Request.Builder()
        .url(url.expandVariables().toHttpUrl()
            .newBuilder()
            .run {
                var b = this
                getMergedKeyValues({ it.queryParameters }, selectedExample.overrides?.disabledQueryParameterIds)
                    .forEach { b = b.addQueryParameter(it.key, it.value) }
                b
            }
            .build())
        .method(
            method = method,
            body = when (selectedExample.body) {
                null -> null
                is FormUrlEncodedBody -> FormUrlEncodedBody(
                    getMergedKeyValues(
                        propertyGetter = { (it.body as? FormUrlEncodedBody)?.value },
                        disabledIds = selectedExample.overrides?.disabledBodyKeyValueIds
                    )
                )
                is MultipartBody -> MultipartBody(
                    getMergedKeyValues(
                        propertyGetter = { (it.body as? MultipartBody)?.value },
                        disabledIds = selectedExample.overrides?.disabledBodyKeyValueIds
                    )
                )
                else -> if (selectedExample.overrides?.isOverrideBody != false) selectedExample.body.expandStringBody() else baseExample.body?.expandStringBody()
            }?.toOkHttpBody(selectedExample.contentType.headerValue?.toMediaType()!!)
        )
    getMergedKeyValues({ it.headers }, selectedExample.overrides?.disabledHeaderIds)
        .filter { it.isEnabled }
        .forEach { b = b.addHeader(it.key, it.value) }

    b.build()
}
