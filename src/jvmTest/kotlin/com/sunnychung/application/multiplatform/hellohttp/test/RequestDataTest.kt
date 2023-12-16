package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadExample
import com.sunnychung.application.multiplatform.hellohttp.model.PostFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RequestBodyWithKeyValuePairs
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserGrpcRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.test.Test
import kotlin.test.assertEquals

class RequestDataTest {
    @Test
    fun `deepCopy ids should not has same ID or ref from previous request object, ID links are not broken, and no missing elements`() {
        ProtocolApplication.values().forEach { protocol ->
            when (protocol) {
                ProtocolApplication.Http -> ContentType.values()
                ProtocolApplication.WebSocket -> arrayOf(ContentType.None)
                ProtocolApplication.Grpc -> arrayOf(ContentType.Json)
                ProtocolApplication.Graphql -> arrayOf(ContentType.Graphql)
            }.forEach { bodyType ->
                val subjectIds = mutableSetOf<String>()
                val subjectObjects = mutableListOf<Any>() // set is not used, because two different objects can be "equal"
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
                fun trackAllObjects(parent: Any, tracker: MutableList<Any>) {
                    if (tracker.any { it === parent }) return // prevent infinite loop due to cyclic dependencies
                    parent::class.memberProperties.forEach {
                        val clazz = it.javaField?.type
//                        println(">> ${it.name} -- ${clazz?.simpleName} - ${clazz?.isPrimitive}, ${clazz?.isEnum}, ${clazz == String::class.java}")
                        if (clazz?.isPrimitive == false && clazz?.isEnum == false && clazz != String::class.java) {
                            val value = (it as KProperty1<Any, *>).get(parent)
                            if (value != null) {
                                tracker += value
                                trackAllObjects(value, tracker)
                            }
                            if (value is Iterable<*>) {
                                value.forEach { it?.let { trackAllObjects(it, tracker) } }
                            } else if (value is Array<*>) {
                                value.forEach { it?.let { trackAllObjects(it, tracker) } }
                            }
                        }
                    }
                }
                trackAllObjects(subjectRequest, subjectObjects)
                println("${subjectObjects.size} object references found")

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
                        it.postFlight.updateVariablesFromHeader.forEach { assertIdIsNew(it.id) }
                        it.postFlight.updateVariablesFromBody.forEach { assertIdIsNew(it.id) }
                        it.overrides?.disabledHeaderIds?.forEach { assertIdIsNew(it) }
                        it.overrides?.disabledQueryParameterIds?.forEach { assertIdIsNew(it) }
                        it.overrides?.disabledBodyKeyValueIds?.forEach { assertIdIsNew(it) }
                        it.overrides?.disablePostFlightUpdateVarIds?.forEach { assertIdIsNew(it) }
                    }
                    payloadExamples?.forEach {
                        assertIdIsNew(it.id)
                    }
                }

                /** Assert all object references are new **/
                val newObjectRefs = mutableListOf<Any>()
                trackAllObjects(copied, newObjectRefs)
                newObjectRefs.forEach { newRef ->
                    assert(subjectObjects.none { it === newRef }) { "Object is repeated. Protocol: $protocol; Body type: $bodyType; Object Class: ${newRef::class}; Object: $newRef" }
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
                    assertEquals(it.postFlight.updateVariablesFromHeader.size, copiedIt.postFlight.updateVariablesFromHeader.size)
                    assertEquals(it.postFlight.updateVariablesFromBody.size, copiedIt.postFlight.updateVariablesFromBody.size)
                    it.overrides?.let {
                        assert(copiedIt.overrides != null)
                        val copiedIt = copiedIt.overrides!!
                        assertEquals(it.disabledHeaderIds.size, copiedIt.disabledHeaderIds.size)
                        assertEquals(it.disabledQueryParameterIds.size, copiedIt.disabledQueryParameterIds.size)
                        assertEquals(it.disabledBodyKeyValueIds.size, copiedIt.disabledBodyKeyValueIds.size)
                        assertEquals(it.disablePostFlightUpdateVarIds.size, copiedIt.disablePostFlightUpdateVarIds.size)
                    }
                }
                assertEquals(subjectRequest.payloadExamples?.size, copied.payloadExamples?.size)
            }
        }
    }
}
