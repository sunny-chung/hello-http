package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.document.RequestCollection
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponseCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.extension.encodeToStream
import com.sunnychung.application.multiplatform.hellohttp.model.ConnectionSecurity
import com.sunnychung.application.multiplatform.hellohttp.model.ConnectionSecurityType
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadExample
import com.sunnychung.application.multiplatform.hellohttp.model.PostFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.RawExchange
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.hours
import com.sunnychung.lib.multiplatform.kdatetime.extension.milliseconds
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.ByteArrayOutputStream
import java.util.Collections
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSerializationApi::class)
class CborStreamTest {

    @Test
    fun `write to output stream via custom Cbor and then read back via official Cbor - regular structured data`() {
        val originalData = RequestCollection(
            id = RequestsDI(uuidString()),
            requests = mutableListOf(
                UserRequestTemplate(
                    id = uuidString(),
                    name = "New Request中文字",
                    method = "POST",
                    url = "https://www.google.com/abcd?a=1",
                    examples = listOf(
                        UserRequestExample(id = uuidString(), name = "Base"),
                        UserRequestExample(
                            id = uuidString(),
                            name = "Example 1",
                            contentType = ContentType.Multipart,
                            headers = mutableListOf(
                                UserKeyValuePair(key = "Header-Key-1", value = "AB0123".repeat(42)),
                            ),
                            queryParameters = mutableListOf(
                                UserKeyValuePair(key = "queryParam1", value = "ab0123".repeat(42)),
                                UserKeyValuePair(key = "queryParam2", value = "中文字"),
                                UserKeyValuePair(key = "queryParam3", value = "123😁✌🏽"),
                            ),
                            body = MultipartBody(
                                listOf(
                                    UserKeyValuePair(key = "normalParam", value = "AB0123"),
                                    UserKeyValuePair(
                                        id = uuidString(),
                                        key = "disabledParam",
                                        value = "AB0123",
                                        isEnabled = false,
                                        valueType = FieldValueType.String
                                    ),
                                    UserKeyValuePair(
                                        id = uuidString(),
                                        key = "fileParam",
                                        value = "C:\\Users\\abc\\Documents\\My Files\\中文字.txt",
                                        isEnabled = true,
                                        valueType = FieldValueType.File
                                    ),
                                )
                            ),
                            overrides = UserRequestExample.Overrides(),
                        ),
                        UserRequestExample(
                            id = uuidString(),
                            name = "Example 2 (JSON)",
                            contentType = ContentType.Json,
                            body = StringBody(
                                "{\n\t\"key\": \"value 1\"\n}\n"
                            ),
                            postFlight = PostFlightSpec(
                                updateVariablesFromBody = listOf(
                                    UserKeyValuePair(key = "accessToken", value = "$.data.accessToken"),
                                )
                            ),
                            overrides = UserRequestExample.Overrides(
                                disabledHeaderIds = setOf(uuidString()),
                                isOverridePreFlightScript = false,
                            ),
                        )
                    )
                ),
                UserRequestTemplate(
                    id = uuidString(),
                    name = "WebSocket Request",
                    application = ProtocolApplication.WebSocket,
                    url = "ws://localhost:12345",
                    payloadExamples = listOf(
                        PayloadExample(id = uuidString(), name = "New Payload", body = "{\n\t\"key\": \"value 1\"\n}\n"),
                        PayloadExample(id = uuidString(), name = "New Payload", body = "{\n\t\"key\": \"value 2\"\n}"),
                    )
                ),
            )
        )

        // write via custom CborStream
        val baos = ByteArrayOutputStream()
        com.sunnychung.application.multiplatform.hellohttp.extension.CborStream {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }.encodeToStream(serializer = RequestCollection.serializer(), value = originalData, out = baos)

        // read back via official Cbor
        val dataRead = kotlinx.serialization.cbor.Cbor {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }.decodeFromByteArray(deserializer = RequestCollection.serializer(), bytes = baos.toByteArray())

        assertEquals(originalData, dataRead)
    }

    @Test
    fun `write to output stream via custom Cbor and then read back via official Cbor - custom serialized large data`() {
        val originalData = ResponseCollection(
            id = ResponsesDI(subprojectId = uuidString()),
            _responsesByRequestExampleId = listOf(
                UserResponse(
                    id = uuidString(),
                    requestId = uuidString(),
                    requestExampleId = uuidString(),
                    startAt = KInstant.now() - 1.hours() - 2345.milliseconds(),
                    endAt = KInstant.now() - 1.hours(),
                    statusCode = 429,
                    statusText = "Too Many Requests",
                    body = Random.nextBytes(30 * 1024 * 1024 + 123), // ~ 30 MB
                    responseSizeInBytes = 30 * 1024 * 1024 + 123L,
                    headers = listOf(
                        "Date" to KZonedInstant.nowAtLocalZoneOffset().toIso8601String(),
                        "Content-Length" to (30 * 1024 * 1024 + 123L).toString(),
                    ),
                    connectionSecurity = ConnectionSecurity(
                        security = ConnectionSecurityType.Unencrypted,
                        clientCertificatePrincipal = null,
                        peerCertificatePrincipal = null,
                    ),
                    rawExchange = RawExchange(exchanges = Collections.synchronizedList(listOf(
                        RawExchange.Exchange(
                            instant = KInstant.now() - 1.hours() - 100.milliseconds(),
                            lastUpdateInstant = KInstant.now() - 1.hours() - 94.milliseconds(),
                            direction = RawExchange.Direction.Outgoing,
                            detail = null,
                            payload = Random.nextBytes(1 * 1024 * 1024 + 123), // ~ 1.1 MB
                            payloadSize = 1 * 1024 * 1024 + 123L,
                        ),
                        RawExchange.Exchange(
                            instant = KInstant.now() - 1.hours() - 10.milliseconds(),
                            lastUpdateInstant = KInstant.now() - 1.hours() - 5.milliseconds(),
                            direction = RawExchange.Direction.Incoming,
                            detail = null,
                            payload = Random.nextBytes(1 * 1024 * 1024 + 8), // ~ 1 MB
                            payloadSize = 1 * 1024 * 1024 + 8L,
                        ),
                    ))),

                )
            ).associateBy { it.id }.toMutableMap()
        )

        // write via custom CborStream
        val baos = ByteArrayOutputStream()
        com.sunnychung.application.multiplatform.hellohttp.extension.CborStream {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }.encodeToStream(serializer = ResponseCollection.serializer(), value = originalData, out = baos)

        // read back via official Cbor
        val dataRead = kotlinx.serialization.cbor.Cbor {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }.decodeFromByteArray(deserializer = ResponseCollection.serializer(), bytes = baos.toByteArray())

        // synchronize transient values
        dataRead.responsesByRequestExampleId.forEach { k, v ->
            val originalFirstResponse = originalData.responsesByRequestExampleId.values.first()
            v.rawExchange.uiVersion = originalFirstResponse.rawExchange.uiVersion

            if (v.body?.contentEquals(originalFirstResponse.body) == true) {
                v.body = originalFirstResponse.body // In UserResponse#equals, body is compared by reference
            }

            originalFirstResponse.rawExchange.exchanges.forEachIndexed { index, o ->
                // In RawExchange.Exchange#equals, payload is compared by reference
                if (o.payload?.contentEquals(v.rawExchange.exchanges[index].payload) == true) {
                    v.rawExchange.exchanges[index].payload = o.payload
                }
            }
        }

        // do not use `assertEquals()` here
        // on fail, it serializes objects which causes Gradle out-of-memory,
        // and it doesn't print the whole objects anyway
        assert(originalData == dataRead)

        assert(originalData.responsesByRequestExampleId.all { (k, v) ->
            v.contentEquals(dataRead.responsesByRequestExampleId[k])
        })
    }
}
