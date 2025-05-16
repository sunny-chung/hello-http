package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadExample
import com.sunnychung.application.multiplatform.hellohttp.model.PostFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.PreFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RequestBodyWithKeyValuePairs
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserGrpcRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.test.util.ObjectReferenceTracker
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestDataTest {
    @Test
    fun `UserRequestTemplate deepCopy ids should not has same ID or ref from previous request object, ID links are not broken, and no missing elements`() {
        ProtocolApplication.values().forEach { protocol ->
            when (protocol) {
                ProtocolApplication.Http -> ContentType.values()
                ProtocolApplication.WebSocket -> arrayOf(ContentType.None)
                ProtocolApplication.Grpc -> arrayOf(ContentType.Json)
                ProtocolApplication.Graphql -> arrayOf(ContentType.Graphql)
            }.forEach { bodyType ->
                val subjectIds = mutableSetOf<String>()
                fun generateUuidForSubject() = uuidString().also { subjectIds += it }

                var baseExample: UserRequestExample? = null
                val subjectRequest = UserRequestTemplate(
                    id = generateUuidForSubject(),
                    name = "Subject Request",
                    application = protocol,
                    method = "POST",
                    url = "\${{url}}",
                    grpc = when (protocol) {
                        ProtocolApplication.Grpc -> UserGrpcRequest(
                            apiSpecId = "apiSpec",
                            service = "service",
                            method = "method"
                        )
                        else -> null
                    },
                    examples = (0 until when (protocol) {
                        ProtocolApplication.WebSocket -> 1
                        else -> 10
                    }).map {
                        val e = UserRequestExample(
                            id = generateUuidForSubject(),
                            name = if (it == 0) "Base" else "Example $it",
                            contentType = bodyType,
                            headers = (0 until 10).map {
                                UserKeyValuePair(
                                    id = generateUuidForSubject(),
                                    key = "Header $it",
                                    value = "Value $it",
                                    valueType = FieldValueType.String,
                                    isEnabled = true
                                )
                            },
                            queryParameters = (0 until 10).map {
                                UserKeyValuePair(
                                    id = generateUuidForSubject(),
                                    key = "Query $it",
                                    value = "Value $it",
                                    valueType = FieldValueType.String,
                                    isEnabled = true
                                )
                            },
                            body = when (bodyType) {
                                ContentType.Json, ContentType.Raw -> StringBody("")
                                ContentType.Multipart -> MultipartBody(
                                    (0 until 10).map {
                                        UserKeyValuePair(
                                            id = generateUuidForSubject(),
                                            key = "Multiparty Body $it",
                                            value = "Value $it",
                                            valueType = FieldValueType.String,
                                            isEnabled = true
                                        )
                                    }
                                )

                                ContentType.FormUrlEncoded -> FormUrlEncodedBody(
                                    (0 until 10).map {
                                        UserKeyValuePair(
                                            id = generateUuidForSubject(),
                                            key = "Form Body $it",
                                            value = "Value $it",
                                            valueType = FieldValueType.String,
                                            isEnabled = true
                                        )
                                    }
                                )

                                ContentType.BinaryFile -> FileBody("/tmp/a")
                                ContentType.Graphql -> GraphqlBody("", "", null)
                                ContentType.None -> null
                            },
                            preFlight = PreFlightSpec(
                                updateVariablesFromHeader = (0 until 10).map {
                                    UserKeyValuePair(
                                        id = generateUuidForSubject(),
                                        key = "env_header_$it",
                                        value = "Request Header $it",
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                },
                                updateVariablesFromBody = (0 until 10).map {
                                    UserKeyValuePair(
                                        id = generateUuidForSubject(),
                                        key = "env_body_$it",
                                        value = "\$.request_json_path_$it",
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                },
                            ),
                            postFlight = PostFlightSpec(
                                updateVariablesFromHeader = (0 until 10).map {
                                    UserKeyValuePair(
                                        id = generateUuidForSubject(),
                                        key = "env_header_$it",
                                        value = "Response Header $it",
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                },
                                updateVariablesFromBody = (0 until 10).map {
                                    UserKeyValuePair(
                                        id = generateUuidForSubject(),
                                        key = "env_body_$it",
                                        value = "\$.response_json_path_$it",
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                },
                            ),
                            overrides = when (it) {
                                0 -> null
                                else -> UserRequestExample.Overrides(
                                    disabledHeaderIds = baseExample!!.headers.map { it.id }.shuffled().take(3).toSet(),
                                    disabledQueryParameterIds = baseExample!!.queryParameters.map { it.id }.shuffled().take(4).toSet(),
                                    disabledBodyKeyValueIds = (baseExample!!.body as? RequestBodyWithKeyValuePairs)
                                        ?.value?.map { it.id }?.shuffled()?.take(5)?.toSet() ?: emptySet(),
                                    disablePreFlightUpdateVarIds = with(baseExample!!.preFlight) { updateVariablesFromHeader + updateVariablesFromBody }
                                        .map { it.id }.shuffled().take(9).toSet(),
                                    disablePostFlightUpdateVarIds = with(baseExample!!.postFlight) { updateVariablesFromHeader + updateVariablesFromBody }
                                        .map { it.id }.shuffled().take(9).toSet(),
                                )
                            }
                        )
                        if (it == 0) baseExample = e
                        e
                    },
                    payloadExamples = when (protocol) {
                        ProtocolApplication.Http -> null
                        else -> (0 until 10).map {
                            PayloadExample(
                                id = generateUuidForSubject(),
                                name = "Payload $it",
                                body = ""
                            )
                        }
                    }
                )

                val subjectRequestTracker = ObjectReferenceTracker(subjectRequest)

                /** Assert all IDs are new **/
                fun assertIdIsNew(id: String) = assert(id !in subjectIds)
                val copied = subjectRequest.deepCopyWithNewId()
                with (copied) {
                    assertIdIsNew(id)
                    examples.forEach {
                        assertIdIsNew(it.id)
                        it.headers.forEach { assertIdIsNew(it.id) }
                        it.queryParameters.forEach { assertIdIsNew(it.id) }
                        if (it.body is RequestBodyWithKeyValuePairs) {
                            (it.body as RequestBodyWithKeyValuePairs).value.forEach { assertIdIsNew(it.id) }
                        }
                        it.preFlight.updateVariablesFromHeader.forEach { assertIdIsNew(it.id) }
                        it.preFlight.updateVariablesFromBody.forEach { assertIdIsNew(it.id) }
                        it.postFlight.updateVariablesFromHeader.forEach { assertIdIsNew(it.id) }
                        it.postFlight.updateVariablesFromBody.forEach { assertIdIsNew(it.id) }
                        it.overrides?.disabledHeaderIds?.forEach { assertIdIsNew(it) }
                        it.overrides?.disabledQueryParameterIds?.forEach { assertIdIsNew(it) }
                        it.overrides?.disabledBodyKeyValueIds?.forEach { assertIdIsNew(it) }
                        it.overrides?.disablePreFlightUpdateVarIds?.forEach { assertIdIsNew(it) }
                        it.overrides?.disablePostFlightUpdateVarIds?.forEach { assertIdIsNew(it) }
                    }
                    payloadExamples?.forEach {
                        assertIdIsNew(it.id)
                    }
                }

                /** Assert all object references are new **/
                subjectRequestTracker.assertNoObjectReferenceIsCopied(copied) { ref ->
                    "Object is repeated. Protocol: $protocol; Body type: $bodyType; Object Class: ${ref::class}; Object: $ref"
                }

                /** Assert all reference IDs are not broken **/
                copied.examples.forEachIndexed { index, it ->
                    if (index == 0) return@forEachIndexed
                    val baseExample = copied.examples.first()
                    it.overrides?.disabledHeaderIds?.forEach {
                        assert(it in baseExample.headers.map { it.id })
                    }
                    it.overrides?.disabledQueryParameterIds?.forEach {
                        assert(it in baseExample.queryParameters.map { it.id })
                    }
                    it.overrides?.disabledBodyKeyValueIds?.forEach {
                        assert(it in (baseExample.body as RequestBodyWithKeyValuePairs).value.map { it.id })
                    }
                    it.overrides?.disablePreFlightUpdateVarIds?.forEach {
                        assert(it in with(baseExample.preFlight) { updateVariablesFromHeader + updateVariablesFromBody}.map { it.id })
                    }
                    it.overrides?.disablePostFlightUpdateVarIds?.forEach {
                        assert(it in with(baseExample.postFlight) { updateVariablesFromHeader + updateVariablesFromBody}.map { it.id })
                    }
                }

                /** Assert no missing elements **/
                assertEquals(subjectRequest.examples.size, copied.examples.size)
                subjectRequest.examples.forEachIndexed { index, it ->
                    val copiedIt = copied.examples[index]
                    assertEquals(it.headers.size, copiedIt.headers.size)
                    assertEquals(it.queryParameters.size, copiedIt.queryParameters.size)
                    if (it.body is RequestBodyWithKeyValuePairs) {
                        assert(copiedIt.body is RequestBodyWithKeyValuePairs)
                        assertEquals((it.body as RequestBodyWithKeyValuePairs).value.size, (copiedIt.body as RequestBodyWithKeyValuePairs).value.size)
                    }
                    assertEquals(it.preFlight.updateVariablesFromHeader.size, copiedIt.preFlight.updateVariablesFromHeader.size)
                    assertEquals(it.preFlight.updateVariablesFromBody.size, copiedIt.preFlight.updateVariablesFromBody.size)
                    assertEquals(it.postFlight.updateVariablesFromHeader.size, copiedIt.postFlight.updateVariablesFromHeader.size)
                    assertEquals(it.postFlight.updateVariablesFromBody.size, copiedIt.postFlight.updateVariablesFromBody.size)
                    it.overrides?.let {
                        assert(copiedIt.overrides != null)
                        val copiedIt = copiedIt.overrides!!
                        assertEquals(it.disabledHeaderIds.size, copiedIt.disabledHeaderIds.size)
                        assertEquals(it.disabledQueryParameterIds.size, copiedIt.disabledQueryParameterIds.size)
                        assertEquals(it.disabledBodyKeyValueIds.size, copiedIt.disabledBodyKeyValueIds.size)
                        assertEquals(it.disablePreFlightUpdateVarIds.size, copiedIt.disablePreFlightUpdateVarIds.size)
                        assertEquals(it.disablePostFlightUpdateVarIds.size, copiedIt.disablePostFlightUpdateVarIds.size)
                    }
                }
                assertEquals(subjectRequest.payloadExamples?.size, copied.payloadExamples?.size)
            }
        }
    }

    @Test
    fun `UserRequestExample deepCopy ids should not has same ID or ref from previous object, and no missing elements`() {
        val subjectIds = mutableSetOf<String>()
        fun generateUuidForSubject() = uuidString().also { subjectIds += it }
        fun assertIdIsNew(id: String) = assert(id !in subjectIds)

        fun randomStrings(count: Int): Set<String> = (0 until count)
            .map { uuidString() }
            .toSet()

        ProtocolApplication.values().forEach { protocol ->
            when (protocol) {
                ProtocolApplication.Http -> ContentType.values()
                ProtocolApplication.WebSocket -> arrayOf(ContentType.None)
                ProtocolApplication.Grpc -> arrayOf(ContentType.Json)
                ProtocolApplication.Graphql -> arrayOf(ContentType.Graphql)
            }.forEach { bodyType ->
                (0 until 10).forEach {
                    val subject = UserRequestExample(
                        id = generateUuidForSubject(),
                        name = if (it == 0) "Base" else "Example $it",
                        contentType = bodyType,
                        headers = (0 until 10).map {
                            UserKeyValuePair(
                                id = generateUuidForSubject(),
                                key = "Header $it",
                                value = "Value $it",
                                valueType = FieldValueType.String,
                                isEnabled = true
                            )
                        },
                        queryParameters = (0 until 10).map {
                            UserKeyValuePair(
                                id = generateUuidForSubject(),
                                key = "Query $it",
                                value = "Value $it",
                                valueType = FieldValueType.String,
                                isEnabled = true
                            )
                        },
                        body = when (bodyType) {
                            ContentType.Json, ContentType.Raw -> StringBody("")
                            ContentType.Multipart -> MultipartBody(
                                (0 until 10).map {
                                    UserKeyValuePair(
                                        id = generateUuidForSubject(),
                                        key = "Multiparty Body $it",
                                        value = "Value $it",
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                }
                            )

                            ContentType.FormUrlEncoded -> FormUrlEncodedBody(
                                (0 until 10).map {
                                    UserKeyValuePair(
                                        id = generateUuidForSubject(),
                                        key = "Form Body $it",
                                        value = "Value $it",
                                        valueType = FieldValueType.String,
                                        isEnabled = true
                                    )
                                }
                            )

                            ContentType.BinaryFile -> FileBody("/tmp/a")
                            ContentType.Graphql -> GraphqlBody("", "", null)
                            ContentType.None -> null
                        },
                        preFlight = PreFlightSpec(
                            updateVariablesFromHeader = (0 until 10).map {
                                UserKeyValuePair(
                                    id = generateUuidForSubject(),
                                    key = "env_header_$it",
                                    value = "Request Header $it",
                                    valueType = FieldValueType.String,
                                    isEnabled = true
                                )
                            },
                            updateVariablesFromBody = (0 until 10).map {
                                UserKeyValuePair(
                                    id = generateUuidForSubject(),
                                    key = "env_body_$it",
                                    value = "\$.request_json_path_$it",
                                    valueType = FieldValueType.String,
                                    isEnabled = true
                                )
                            },
                        ),
                        postFlight = PostFlightSpec(
                            updateVariablesFromHeader = (0 until 10).map {
                                UserKeyValuePair(
                                    id = generateUuidForSubject(),
                                    key = "env_header_$it",
                                    value = "Response Header $it",
                                    valueType = FieldValueType.String,
                                    isEnabled = true
                                )
                            },
                            updateVariablesFromBody = (0 until 10).map {
                                UserKeyValuePair(
                                    id = generateUuidForSubject(),
                                    key = "env_body_$it",
                                    value = "\$.response_json_path_$it",
                                    valueType = FieldValueType.String,
                                    isEnabled = true
                                )
                            },
                        ),
                        overrides = when (it) {
                            0 -> null
                            else -> UserRequestExample.Overrides(
                                disabledHeaderIds = randomStrings(3),
                                disabledQueryParameterIds = randomStrings(4),
                                disabledBodyKeyValueIds = randomStrings(5),
                                disablePreFlightUpdateVarIds = randomStrings(9),
                                disablePostFlightUpdateVarIds = randomStrings(9),
                            )
                        }
                    )

                    val subjectRequestTracker = ObjectReferenceTracker(subject)
                    val copied = subject.deepCopyWithNewId()

                    /** Assert all object references are new **/
                    subjectRequestTracker.assertNoObjectReferenceIsCopied(copied) { ref ->
                        "Object is repeated. Object Class: ${ref::class}; Object: $ref"
                    }

                    /** Assert all IDs are new **/
                    with (copied) {
                        assertIdIsNew(id)
                        headers.forEach { assertIdIsNew(it.id) }
                        queryParameters.forEach { assertIdIsNew(it.id) }
                        when (body) {
                            is FormUrlEncodedBody -> (body as FormUrlEncodedBody).value.forEach {
                                assertIdIsNew(it.id)
                            }
                            is MultipartBody -> (body as MultipartBody).value.forEach {
                                assertIdIsNew(it.id)
                            }
                            else -> {}
                        }
                        preFlight.updateVariablesFromHeader.forEach { assertIdIsNew(it.id) }
                        preFlight.updateVariablesFromBody.forEach { assertIdIsNew(it.id) }
                        postFlight.updateVariablesFromHeader.forEach { assertIdIsNew(it.id) }
                        postFlight.updateVariablesFromBody.forEach { assertIdIsNew(it.id) }
                    }
                }
            }
        }
    }
}
