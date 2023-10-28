package com.sunnychung.application.multiplatform.hellohttp.extension

import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun UserRequestTemplate.toHttpRequest(exampleId: String, environment: Environment?, resolveVariableMode: UserRequestTemplate.ResolveVariableMode = UserRequestTemplate.ExpandByEnvironment): HttpRequest = withScope(exampleId, environment, resolveVariableMode) {

    fun UserRequestBody.expandStringBody(): UserRequestBody {
        if (this is StringBody) {
            return StringBody(value.resolveVariables())
        }
        return this
    }

    HttpRequest(
        method = method,
        url = url.resolveVariables(),
        headers = getMergedKeyValues({ it.headers }, selectedExample.overrides?.disabledHeaderIds)
            .map { it.key to it.value },
        queryParameters = getMergedKeyValues({ it.queryParameters }, selectedExample.overrides?.disabledQueryParameterIds)
            .map { it.key to it.value },
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
        },
    )
}

fun UserRequestTemplate.toOkHttpRequest(exampleId: String, environment: Environment?): Request {
    val req = toHttpRequest(exampleId, environment)
    val selectedExample = examples.first { it.id == exampleId }

    var b = Request.Builder()
        .url(req.url.toHttpUrl()
            .newBuilder()
            .run {
                var b = this
                req.queryParameters
                    .forEach { b = b.addQueryParameter(it.first, it.second) }
                b
            }
            .build())
        .method(
            method = method,
            body = req.body?.toOkHttpBody(selectedExample.contentType.headerValue?.toMediaType())
        )

    req.headers
        .forEach { b = b.addHeader(it.first, it.second) }

    return b.build()
}

fun UserRequestTemplate.toCurlCommand(exampleId: String, environment: Environment?): String {
    fun String.escape(): String {
        return replace("\\", "\\\\").replace("\"", "\\\"")
    }

    fun String.urlEncoded(): String {
        return URLEncoder.encode(this, StandardCharsets.UTF_8)
    }

    val request = toHttpRequest(exampleId, environment)

    val url = request.url.toHttpUrl().newBuilder().run {
        var b = this
        request.queryParameters
            .forEach { b = b.addQueryParameter(it.first, it.second) }
        b
    }.build().toString()

    var curl = "time curl --verbose"
    if (environment?.sslConfig?.isInsecure == true) {
        curl += " \\\n  --insecure"
    }
    curl += " \\\n  --request \"${request.method.escape()}\""
    curl += " \\\n  --url \"${url.escape()}\""
    request.headers.forEach {
        curl += " \\\n  --header \"${it.first.escape()}: ${it.second.escape()}\""
    }
    when (request.body) {
        is FormUrlEncodedBody -> {
            request.body.value.forEach {
                curl += " \\\n  --data-urlencode \"${it.key.urlEncoded().escape()}=${it.value.escape()}\""
            }
        }
        is MultipartBody -> {
            request.body.value.forEach {
                when (it.valueType) {
                    FieldValueType.String -> curl += " \\\n  --form \"${it.key.escape()}=\\\"${it.value.escape().escape()}\\\"\""
                    FieldValueType.File -> curl += " \\\n  --form \"${it.key.escape()}=@\\\"${it.value.escape().escape()}\\\"\""
                }
            }
        }
        is StringBody -> {
            curl += " \\\n  --data \"${request.body.value.escape()}\""
        }
        is FileBody -> {
            if (request.body.filePath != null) {
                curl += " \\\n  --data-binary \"@${request.body.filePath}\""
            }
        }
        null -> {}
    }
    return curl
}
