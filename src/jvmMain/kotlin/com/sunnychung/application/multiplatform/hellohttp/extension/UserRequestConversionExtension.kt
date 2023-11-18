package com.sunnychung.application.multiplatform.hellohttp.extension

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlRequestBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.platform.LinuxOS
import com.sunnychung.application.multiplatform.hellohttp.platform.MacOS
import com.sunnychung.application.multiplatform.hellohttp.platform.OS
import com.sunnychung.application.multiplatform.hellohttp.platform.currentOS
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import graphql.language.OperationDefinition
import graphql.parser.InvalidSyntaxException
import graphql.parser.Parser
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.http.nio.AsyncRequestProducer
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder
import org.apache.hc.core5.net.URIBuilder
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun UserRequestTemplate.toHttpRequest(
    exampleId: String,
    environment: Environment?,
    resolveVariableMode: UserRequestTemplate.ResolveVariableMode = UserRequestTemplate.ExpandByEnvironment,
    addDefaultUserAgent: Boolean = true
): HttpRequest = withScope(exampleId, environment, resolveVariableMode) {

    fun UserRequestBody.expandStringBody(): UserRequestBody {
        if (this is StringBody) {
            return StringBody(value.resolveVariables())
        }
        return this
    }

    val overrides = selectedExample.overrides

    var req = HttpRequest(
        method = method,
        url = url.resolveVariables(),
        headers = getMergedKeyValues({ it.headers }, overrides?.disabledHeaderIds)
            .map { it.key to it.value }
            .run {
                if (addDefaultUserAgent && none { it.first.equals("user-agent", ignoreCase = true) }) {
                    this + Pair("User-Agent", "Hello-HTTP/${AppContext.MetadataManager.version}")
                } else {
                    this
                }
            },
        queryParameters = getMergedKeyValues({ it.queryParameters }, overrides?.disabledQueryParameterIds)
            .map { it.key to it.value },
        body = when (selectedExample.body) {
            null -> null
            is FormUrlEncodedBody -> FormUrlEncodedBody(
                getMergedKeyValues(
                    propertyGetter = { (it.body as? FormUrlEncodedBody)?.value },
                    disabledIds = overrides?.disabledBodyKeyValueIds
                )
            )
            is MultipartBody -> MultipartBody(
                getMergedKeyValues(
                    propertyGetter = { (it.body as? MultipartBody)?.value },
                    disabledIds = overrides?.disabledBodyKeyValueIds
                )
            )
            else -> if (overrides?.isOverrideBody != false) selectedExample.body.expandStringBody() else baseExample.body?.expandStringBody()
        },
        contentType = selectedExample.contentType,
        application = application,
    )

    if (application == ProtocolApplication.Graphql) {
        val graphqlBody = req.body as GraphqlBody
        val baseGraphqlBody = baseExample.body as GraphqlBody
        val operationType = (if (overrides?.isOverrideBodyContent == true) graphqlBody else baseGraphqlBody)
            .getOperation(isThrowError = false)?.operation
            ?: OperationDefinition.Operation.QUERY // bypass invalid syntax or operation name, let server throw error. users can test such scenario

        val jsonMapper = jacksonObjectMapper()
        val body = StringBody(
            jsonMapper.writeValueAsString(
                GraphqlRequestBody(
                    query = (if (overrides?.isOverrideBodyContent == true) graphqlBody else baseGraphqlBody).document,
                    variables = jsonMapper.readTree((if (overrides?.isOverrideBodyVariables == true) graphqlBody else baseGraphqlBody).variables),
                    operationName = graphqlBody.operationName.emptyToNull()
                )
            )
        )
        req = if (operationType != OperationDefinition.Operation.SUBSCRIPTION) {
            req.copy(
                application = ProtocolApplication.Http,
                method = "POST",
                headers = req.headers.filter { !it.first.equals("Content-Type", true) && !it.first.equals("Accept", true) } +
                        ("Content-Type" to "application/json") +
                        ("Accept" to "application/graphql-response+json; charset=utf-8, application/json; charset=utf-8"),
                body = body,
            )
        } else {
            req.copy(
                body = body,
            )
        }
    }

    req
}

fun HttpRequest.toOkHttpRequest(): Request {
    val req = this

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
            body = req.body?.toOkHttpBody(contentType.headerValue?.toMediaType())
        )

    req.headers
        .forEach { b = b.addHeader(it.first, it.second) }

    return b.build()
}

fun HttpRequest.toApacheHttpRequest(): Pair<AsyncRequestProducer, Long> {
//    val b = BasicClassicHttpRequest(
//        method,
//        URIBuilder(url)
//            .run {
//                var b = this
//                queryParameters
//                    .forEach { b = b.addParameter(it.first, it.second) }
//                b
//            }
//            .build()
//    )
    val b = AsyncRequestBuilder
        .create(method)
        .setUri(URIBuilder(url)
            .run {
                var b = this
                queryParameters
                    .forEach { b = b.addParameter(it.first, it.second) }
                b
            }
            .build())

    headers.forEach { b.addHeader(it.first, it.second) }

    val entity = when (body) {
        is FileBody -> body.filePath?.let { AsyncEntityProducers.create(File(it), org.apache.hc.core5.http.ContentType.DEFAULT_BINARY) }

        is FormUrlEncodedBody -> AsyncEntityProducers.createUrlEncoded(
            body.value.map { BasicNameValuePair(it.key, it.value) },
            Charsets.UTF_8
        )

        is MultipartBody -> {
            val entity = MultipartEntityBuilder.create()
                .run {
                    var b = this
                    body.value.forEach {
                        val part = when (it.valueType) {
                            FieldValueType.String -> org.apache.hc.client5.http.entity.mime.StringBody(it.value, org.apache.hc.core5.http.ContentType.MULTIPART_FORM_DATA)
                            FieldValueType.File -> org.apache.hc.client5.http.entity.mime.FileBody(File(it.value))
                        }
                        b = b.addPart(it.key, part)
                    }
                    b
                }
                .build()
            // TODO memory bomb!
            AsyncEntityProducers.create(
                entity.content.readAllBytes(),
                org.apache.hc.core5.http.ContentType.parse(entity.contentType),
                *(entity.trailers?.get()?.toTypedArray() ?: emptyArray())
            )
        }
        is StringBody -> AsyncEntityProducers.create(body.value)
        null -> null
        else -> throw UnsupportedOperationException()
    }
    if (entity != null) {
        b.entity = entity
    }
    return Pair(b.build(), entity?.contentLength ?: 0L)
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

    val currentOS = currentOS()
    val newLine = " ${currentOS.commandLineEscapeNewLine}\n  "

    var curl = ""
    if (currentOS in setOf(MacOS, LinuxOS)) {
        curl += "time "
    }
    curl += "curl --verbose"
    if (environment?.sslConfig?.isInsecure == true) {
        curl += "${newLine}--insecure"
    }
    curl += "${newLine}--request \"${request.method.escape()}\""
    curl += "${newLine}--url \"${url.escape()}\""
    request.headers.forEach {
        curl += "${newLine}--header \"${it.first.escape()}: ${it.second.escape()}\""
    }
    when (request.body) {
        is FormUrlEncodedBody -> {
            request.body.value.forEach {
                curl += "${newLine}--data-urlencode \"${it.key.urlEncoded().escape()}=${it.value.escape()}\""
            }
        }
        is MultipartBody -> {
            request.body.value.forEach {
                when (it.valueType) {
                    FieldValueType.String -> curl += "${newLine}--form \"${it.key.escape()}=\\\"${it.value.escape().escape()}\\\"\""
                    FieldValueType.File -> curl += "${newLine}--form \"${it.key.escape()}=@\\\"${it.value.escape().escape()}\\\"\""
                }
            }
        }
        is StringBody -> {
            curl += "${newLine}--data \"${request.body.value.escape()}\""
        }
        is FileBody -> {
            if (request.body.filePath != null) {
                curl += "${newLine}--data-binary \"@${request.body.filePath}\""
            }
        }
        null -> {}
        else -> throw UnsupportedOperationException()
    }
    return curl
}
