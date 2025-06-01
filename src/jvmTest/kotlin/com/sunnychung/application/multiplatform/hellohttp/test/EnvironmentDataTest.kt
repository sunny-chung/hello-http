package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.model.ClientCertificateKeyPair
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.ImportedFile
import com.sunnychung.application.multiplatform.hellohttp.model.SslConfig
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.network.util.CookieJar
import com.sunnychung.application.multiplatform.hellohttp.test.util.ObjectReferenceTracker
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class EnvironmentDataTest {
    @Test
    fun `deepCopy ids should not has same ID or ref from previous object, and no missing elements`() {
        val subjectIds = mutableSetOf<String>()
        fun generateUuidForSubject() = uuidString().also { subjectIds += it }

        val subject = Environment(
            id = generateUuidForSubject(),
            name = "Subject Environment",
            variables = (0 until 10).map {
                UserKeyValuePair(
                    id = generateUuidForSubject(),
                    key = "Key $it",
                    value = "Value $it",
                    valueType = FieldValueType.String,
                    isEnabled = true
                )
            }.toMutableList(),
            httpConfig = HttpConfig(protocolVersion = HttpConfig.HttpProtocolVersion.Http1Only),
            sslConfig = SslConfig(
                isInsecure = false,
                trustedCaCertificates = (0 until 10).map {
                    ImportedFile(
                        id = generateUuidForSubject(),
                        name = "CA Cert $it",
                        originalFilename = "ca-cert-$it.crt",
                        createdWhen = KInstant.now(),
                        isEnabled = it % 2 == 0,
                        content = ByteArray(it) { it.toByte() },
                    )
                },
                clientCertificateKeyPairs = (0 until 1).map {
                    ClientCertificateKeyPair(
                        id = uuidString(),
                        certificate = ImportedFile(
                            id = generateUuidForSubject(),
                            name = "Client Cert $it",
                            originalFilename = "client-cert-$it.crt",
                            createdWhen = KInstant.now(),
                            isEnabled = it % 2 == 0,
                            content = ByteArray(it) { it.toByte() },
                        ),
                        privateKey = ImportedFile(
                            id = generateUuidForSubject(),
                            name = "Private Key $it",
                            originalFilename = "key-$it.key",
                            createdWhen = KInstant.now(),
                            isEnabled = it % 2 == 0,
                            content = ByteArray(it) { it.toByte() },
                        ),
                        createdWhen = KInstant.now(),
                        isEnabled = true,
                    )
                }
            ),
            userFiles = (0 until 10).map {
                ImportedFile(
                    id = generateUuidForSubject(),
                    name = "User Files $it",
                    originalFilename = "user-file-$it.bin",
                    createdWhen = KInstant.now(),
                    isEnabled = it % 2 == 1,
                    content = ByteArray(it) { it.toByte() },
                )
            },
            cookieJar = CookieJar().apply {
                store(URI("http://example.com"), listOf("a=b", "c=de"))
                store(URI("http://example.com"), listOf("fg=hi"))
            }
        )

        val subjectRequestTracker = ObjectReferenceTracker(subject)
        val copied = subject.deepCopyWithNewId()

        /** Assert all object references are new **/
        subjectRequestTracker.assertNoObjectReferenceIsCopied(copied) { ref ->
            "Object is repeated. Object Class: ${ref::class}; Object: $ref"
        }

        /** Assert all IDs are new **/
        fun assertIdIsNew(id: String) = assert(id !in subjectIds)
        with (copied) {
            assertIdIsNew(id)
            variables.forEach { assertIdIsNew(it.id) }
            with (sslConfig) {
                assertIdIsNew(id)
                trustedCaCertificates.forEach { assertIdIsNew(it.id) }
                clientCertificateKeyPairs.forEach { assertIdIsNew(it.id) }
            }
            userFiles.forEach { assertIdIsNew(it.id) }
        }

        /** Assert no missing elements **/
        assertEquals(subject.variables.size, copied.variables.size)
        assertEquals(subject.sslConfig.trustedCaCertificates.size, copied.sslConfig.trustedCaCertificates.size)
        assertEquals(subject.sslConfig.clientCertificateKeyPairs.size, copied.sslConfig.clientCertificateKeyPairs.size)
        assertEquals(subject.userFiles.size, copied.userFiles.size)
    }
}
