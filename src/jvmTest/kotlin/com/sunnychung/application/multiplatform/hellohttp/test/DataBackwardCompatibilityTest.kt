package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.OperationalDI
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.lib.multiplatform.kdatetime.KZoneOffset
import com.sunnychung.lib.multiplatform.kdatetime.KZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Order
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.ParameterizedTest.ARGUMENTS_PLACEHOLDER
import org.junit.jupiter.params.ParameterizedTest.DISPLAY_NAME_PLACEHOLDER
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DataBackwardCompatibilityTest {

    internal fun currentAppVersionExcludingLabel(): String =
        AppContext.MetadataManager.version.substringBefore("-")

    internal fun testDataBaseDirOfAppVersion(version: String) =
        File("test-data-archive", version)

    internal fun isCurrentAppVersionDev() = AppContext.MetadataManager.version.endsWith("-snapshot", ignoreCase = true)

    /**
     * This method assumes tests are NOT executed in parallel.
     */
    fun copyDataFilesAndTest(
        inputDataDir: File,
        isThrowExceptionIfMissingInput: Boolean = !isCurrentAppVersionDev(),
        testContent: suspend () -> Unit,
    ) {
        if (!inputDataDir.exists() || !inputDataDir.isDirectory) {
            if (isThrowExceptionIfMissingInput) {
                throw FileNotFoundException("Input data files directory is missing")
            }
            return
        }
        val dataFiles = inputDataDir.list()
        if (dataFiles.isNullOrEmpty()) {
            if (isThrowExceptionIfMissingInput) {
                throw FileNotFoundException("Input data files directory is empty")
            }
            return
        }

        // copy all data files to a temporary build directory
        val tempBaseDataDir = File("build", "temp-test-run-${UUID.randomUUID()}")
        if (tempBaseDataDir.exists()) {
            tempBaseDataDir.deleteRecursively()
        }
        val previousAppContext = AppContext.instance
        try {
            tempBaseDataDir.mkdirs()
            dataFiles.forEach {
                File(inputDataDir, it).copyRecursively(File(tempBaseDataDir, it))
            }

            AppContext.instance = AppContext() // use a new context
            AppContext.dataDir = tempBaseDataDir

            runBlocking {
                testContent()
            }
        } finally {
            tempBaseDataDir.deleteRecursively()
            AppContext.instance = previousAppContext
        }
    }

    /**
     * Read data files from $projectDir/test-data-archive/$version/app-data/,
     * and generate a backup file to $projectDir/test-data-archive/$version/app-data-backup.dump.
     *
     * Other test cases in this test depend on the execution product of this test case.
     */
    @Test
    @Order(-100) // highest priority
    fun `data files for current app version can be read successfully, and can be converted to backup dump file`() {
        val baseArchiveDataDir = testDataBaseDirOfAppVersion(currentAppVersionExcludingLabel())
        val inputDataDir = File(baseArchiveDataDir, "app-data")
        copyDataFilesAndTest(inputDataDir) {
            val backupDestination = File(baseArchiveDataDir, "app-data-backup.dump")
            AppContext.AutoBackupManager.backupNow(backupDestination)
            if (!backupDestination.isFile) {
                throw RuntimeException("Backup file cannot be created")
            }
        }
    }

    @ParameterizedTest(name = "$DISPLAY_NAME_PLACEHOLDER [$ARGUMENTS_PLACEHOLDER]")
    @ValueSource(strings = ["1.5.2", "1.6.0"])
    fun `load and convert data files correctly from app version`(version: String) {
        val inputDataDir = File(testDataBaseDirOfAppVersion(version), "app-data")
        copyDataFilesAndTest(inputDataDir, isThrowExceptionIfMissingInput = version != currentAppVersionExcludingLabel()) {
            // perform the I/O steps implemented in `main()`
            AppContext.PersistenceManager.initialize()
            AppContext.AutoBackupManager.backupNow()
            AppContext.OperationalRepository.read(OperationalDI())
            val preference = AppContext.UserPreferenceRepository.read(UserPreferenceDI())!!.preference
            AppContext.UserPreferenceViewModel.setColorTheme(preference.colourTheme)

            // data verification
            val projectCollection = AppContext.ProjectCollectionRepository.read(ProjectAndEnvironmentsDI())!!
            val projects = projectCollection.projects
            assertEquals(2, projects.size)

            projects.first { it.name == "Empty Project" }.let {
                assert(it.subprojects.isEmpty())
            }

            projects.first { it.name == "Test Server" }.let { project ->
                assertEquals(5, project.subprojects.size)

                project.subprojects.first { it.name == "HTTP only" }.let { subproject ->
                    assertEquals(4, subproject.environments.size)
                    subproject.environments.first { it.name == "HTTP Cleartext" }.let { env ->
                        env.variables.first { it.key == "prefix" }.let {
                            assertEquals("http://testserver.net:18081", it.value)
                            assertEquals(true, it.isEnabled)
                        }

                        assertEquals(null, env.httpConfig.protocolVersion)

                        assertEquals(null, env.sslConfig.isInsecure)
                        assertEquals(0, env.sslConfig.trustedCaCertificates.size)
                        assertEquals(null, env.sslConfig.isDisableSystemCaCertificates)
                        assertEquals(0, env.sslConfig.clientCertificateKeyPairs.size)
                    }
                    subproject.environments.first { it.name == "HTTP/2 Cleartext" }.let { env ->
                        env.variables.first { it.key == "prefix2" }.let {
                            assertEquals("http://testserver.net:18081", it.value)
                            assertEquals(true, it.isEnabled)
                        }

                        assertEquals(HttpConfig.HttpProtocolVersion.Http2Only, env.httpConfig.protocolVersion)

                        assertEquals(null, env.sslConfig.isInsecure)
                        assertEquals(0, env.sslConfig.trustedCaCertificates.size)
                        assertEquals(null, env.sslConfig.isDisableSystemCaCertificates)
                        assertEquals(0, env.sslConfig.clientCertificateKeyPairs.size)
                    }
                    subproject.environments.first { it.name == "HTTP/1 SSL Insecure" }.let { env ->
                        env.variables.first { it.key == "prefix" }.let {
                            assertEquals("https://testserver.net:18084", it.value)
                            assertEquals(true, it.isEnabled)
                        }

                        assertEquals(HttpConfig.HttpProtocolVersion.Http1Only, env.httpConfig.protocolVersion)

                        assertEquals(true, env.sslConfig.isInsecure)
                        assertEquals(0, env.sslConfig.trustedCaCertificates.size)
                        assertEquals(null, env.sslConfig.isDisableSystemCaCertificates)
                        assertEquals(0, env.sslConfig.clientCertificateKeyPairs.size)
                    }
                    subproject.environments.first { it.name == "HTTP/2 SSL Trust" }.let { env ->
                        env.variables.first { it.key == "prefix2" }.let {
                            assertEquals("https://testserver.net:18084", it.value)
                            assertEquals(true, it.isEnabled)
                        }

                        assertEquals(HttpConfig.HttpProtocolVersion.Negotiate, env.httpConfig.protocolVersion)

                        assertEquals(true, env.sslConfig.isInsecure)
                        assertEquals(1, env.sslConfig.trustedCaCertificates.size)
                        assert(env.sslConfig.trustedCaCertificates.first().name.contains("CN=Test Server CA"))
                        assert(
                            env.sslConfig.trustedCaCertificates.first().createdWhen > KZonedDateTime(
                                year = 2024,
                                month = 1,
                                day = 1,
                                hour = 0,
                                minute = 0,
                                second = 0,
                                zoneOffset = KZoneOffset.local()
                            ).toKInstant()
                        )
                        assertEquals(null, env.sslConfig.isDisableSystemCaCertificates)
                        assertEquals(0, env.sslConfig.clientCertificateKeyPairs.size)
                    }

                    val requests = AppContext.RequestCollectionRepository
                        .read(RequestsDI(subprojectId = subproject.id))!!
                        .requests
                    assertEquals(7, requests.size)
                    val responses = AppContext.PersistResponseManager
                        .loadResponseCollection(ResponsesDI(subprojectId = subproject.id))
                        .responsesByRequestExampleId
                    assert(responses.isNotEmpty())

                    requests.first { it.name == "Only Base Example" }.let {
                        assertEquals(ProtocolApplication.Http, it.application)
                        assertEquals("GET", it.method)
                        assertEquals("\${{prefix}}/rest/echo", it.url)
                        assertEquals(1, it.examples.size)
                        it.examples.first().let { ex ->
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(2, ex.queryParameters.size)
                            assertEquals("value1", ex.queryParameters.first { it.key == "key1" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "key1" }.isEnabled)
                            assertEquals("test value 2", ex.queryParameters.first { it.key == "key2" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "key2" }.isEnabled)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                    }

                    requests.first { it.name == "New Request" }.let {
                        assertEquals(ProtocolApplication.Http, it.application)
                        assertEquals("PATCH", it.method)
                        assertEquals("\${{prefix}}/rest/echo", it.url)
                        assertEquals(1, it.examples.size)
                        it.examples.first().let { ex ->
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)

                            assert(ex.id !in responses)
                        }
                    }

                    requests.first { it.name == "HTTP/2 GET" }.let {
                        assertEquals(ProtocolApplication.Http, it.application)
                        assertEquals("GET", it.method)
                        assertEquals("\${{prefix2}}/rest/echo", it.url)
                        assertEquals(4, it.examples.size)
                        var i = 0
                        it.examples[i++].let { ex -> // Base Example
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(2, ex.queryParameters.size)
                            assertEquals("v1", ex.queryParameters.first { it.key == "key_inherited1" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "key_inherited1" }.isEnabled)
                            assertEquals("value2", ex.queryParameters.first { it.key == "key_inherited2" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "key_inherited2" }.isEnabled)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)

                            assert(ex.id !in responses)
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Ex1", ex.name)
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(2, ex.queryParameters.size)
                            assertEquals("this+value+1", ex.queryParameters.first { it.key == "key_example1" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "key_example1" }.isEnabled)
                            assertEquals("value 2", ex.queryParameters.first { it.key == "key_new2" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "key_new2" }.isEnabled)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assertEquals(true, ex.overrides?.hasNoDisable() ?: true)

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Example 2", ex.name)
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assertEquals(true, ex.overrides?.hasNoDisable() ?: true)

                            assert(ex.id !in responses)
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Example3", ex.name)
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(1, ex.queryParameters.size)
                            assertEquals("value3", ex.queryParameters.first { it.key == "ex3" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "ex3" }.isEnabled)
                            assertEquals(1, ex.headers.size)
                            assertEquals("H1", ex.headers.first { it.key == "custom-header" }.value)
                            assertEquals(true, ex.headers.first { it.key == "custom-header" }.isEnabled)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assertEquals(true, ex.overrides?.hasNoDisable() ?: true)

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                    }

                    requests.first { it.name == "Mixed GET" }.let {
                        assertEquals(ProtocolApplication.Http, it.application)
                        assertEquals("GET", it.method)
                        assertEquals("\${{prefix}}/rest/echo", it.url)
                        assertEquals(4, it.examples.size)
                        var i = 0
                        it.examples[i++].let { ex -> // Base Example
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(2, ex.queryParameters.size)
                            assertEquals("v1", ex.queryParameters.first { it.key == "key_inherited1" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "key_inherited1" }.isEnabled)
                            assertEquals("value2", ex.queryParameters.first { it.key == "key_inherited2" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "key_inherited2" }.isEnabled)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)

                            assert(ex.id !in responses)
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Ex1", ex.name)
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(2, ex.queryParameters.size)
                            assertEquals("this+value+1", ex.queryParameters.first { it.key == "key_example1" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "key_example1" }.isEnabled)
                            assertEquals("value 2", ex.queryParameters.first { it.key == "key_new2" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "key_new2" }.isEnabled)
                            assertEquals(1, ex.headers.size)
                            assertEquals("valuevalue", ex.headers.first { it.key == "header" }.value)
                            assertEquals(true, ex.headers.first { it.key == "header" }.isEnabled)
                            assertEquals(1, ex.postFlight.updateVariablesFromHeader.size)
                            ex.postFlight.updateVariablesFromHeader.first { it.key == "length" }.let {
                                assertEquals("Content-Length", it.value)
                                assertEquals(true, it.isEnabled)
                            }
                            assertEquals(1, ex.postFlight.updateVariablesFromBody.size)
                            ex.postFlight.updateVariablesFromBody.first { it.key == "method" }.let {
                                assertEquals("\$.method", it.value)
                                assertEquals(true, it.isEnabled)
                            }
                            assertEquals(true, ex.overrides?.hasNoDisable() ?: true)

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Example 2", ex.name)
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assertEquals(true, ex.overrides?.hasNoDisable() ?: true)

                            assert(ex.id !in responses)
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Example3", ex.name)
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(1, ex.queryParameters.size)
                            assertEquals("value3", ex.queryParameters.first { it.key == "ex3" }.value)
                            assertEquals(true, ex.queryParameters.first { it.key == "ex3" }.isEnabled)
                            assertEquals(1, ex.headers.size)
                            assertEquals("H1", ex.headers.first { it.key == "custom-header" }.value)
                            assertEquals(true, ex.headers.first { it.key == "custom-header" }.isEnabled)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assertEquals(true, ex.overrides?.hasNoDisable() ?: true)

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                    }

                    requests.first { it.name == "Post" }.let {
                        assertEquals(ProtocolApplication.Http, it.application)
                        assertEquals("POST", it.method)
                        assertEquals("\${{prefix}}/rest/echo", it.url)
                        assertEquals(7, it.examples.size)
                        val baseEx = it.examples.first()
                        var i = 0
                        it.examples[i++].let { ex -> // Base Example
                            assertEquals(ContentType.Json, ex.contentType)
                            assert(ex.body is StringBody)
                            assert((ex.body as StringBody).value.isNotEmpty())
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(1, ex.headers.size)
                            ex.headers.first { it.key == "my-Header" }.let {
                                assertEquals("header", it.value)
                                assertEquals(true, it.isEnabled)
                            }
                            assertEquals(1, ex.postFlight.updateVariablesFromHeader.size)
                            ex.postFlight.updateVariablesFromHeader.first { it.key == "len" }.let {
                                assertEquals("Content-Length", it.value)
                                assertEquals(true, it.isEnabled)
                            }
                            assertEquals(1, ex.postFlight.updateVariablesFromBody.size)
                            ex.postFlight.updateVariablesFromBody.first { it.key == "met" }.let {
                                assertEquals("\$.method", it.value)
                                assertEquals(true, it.isEnabled)
                            }

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Directly inherited from BASE", ex.name)
                            assertEquals(ContentType.Json, ex.contentType)
                            assert(!ex.overrides!!.isOverrideBody)
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assert(ex.overrides!!.hasNoDisable())

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Different Body", ex.name)
                            assertEquals(ContentType.Json, ex.contentType)
                            assert(ex.overrides!!.isOverrideBody)
                            assert((ex.body as StringBody).value.isNotEmpty())
                            assertNotEquals(it.examples.first().body, ex.body)
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assert(ex.overrides!!.hasNoDisable())

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Form", ex.name)
                            assertEquals(ContentType.FormUrlEncoded, ex.contentType)
                            (ex.body as FormUrlEncodedBody).value.let { form ->
                                assertEquals(2, form.size)
                                var j = 0
                                form[j++].let {
                                    assertEquals("k1", it.key)
                                    assertEquals("v1", it.value)
                                    assertEquals(true, it.isEnabled)
                                }
                                form[j++].let {
                                    assertEquals("k2", it.key)
                                    assertEquals("v2", it.value)
                                    assertEquals(true, it.isEnabled)
                                }
                            }
                            assertEquals(1, ex.queryParameters.size)
                            ex.queryParameters.first().let {
                                assertEquals("q1", it.key)
                                assertEquals("v1", it.value)
                                assertEquals(true, it.isEnabled)
                            }
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assert(ex.overrides!!.hasNoDisable())

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Remove all inherited", ex.name)
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(1, ex.queryParameters.size)
                            ex.queryParameters.first().let {
                                assertEquals("qq", it.key)
                                assertEquals("Q", it.value)
                                assertEquals(true, it.isEnabled)
                            }
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assert(ex.overrides!!.disabledHeaderIds.containsAll(baseEx.headers.map { it.id }))
                            assert(ex.overrides!!.disablePostFlightUpdateVarIds.containsAll(baseEx.postFlight.updateVariablesFromHeader.map { it.id }))
                            assert(ex.overrides!!.disablePostFlightUpdateVarIds.containsAll(baseEx.postFlight.updateVariablesFromBody.map { it.id }))

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Multipart Text-only", ex.name)
                            assertEquals(ContentType.Multipart, ex.contentType)
                            (ex.body as MultipartBody).value.let { form ->
                                assertEquals(4, form.size)
                                form.first { it.key == "form1" }.assert("value1", true)
                                form.first { it.key == "form2" }.assert("value2 disabled", false)
                                form.first { it.key == "key3" }.assert("enabled value 3", true)
                                form.first { it.key == "unicode" }.assert("中文字元", true)
                            }
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(1, ex.postFlight.updateVariablesFromHeader.size)
                            ex.postFlight.updateVariablesFromHeader.first()
                                .assert("type", "Content-Type", true)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assert(ex.overrides!!.disabledHeaderIds.isEmpty())
                            assert(ex.overrides!!.disablePostFlightUpdateVarIds == baseEx.postFlight.updateVariablesFromHeader.map { it.id }.toSet())

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Multipart File", ex.name)
                            assertEquals(ContentType.Multipart, ex.contentType)
                            (ex.body as MultipartBody).value.let { form ->
                                assertEquals(2, form.size)
                                form.first { it.key == "file1" }.assert("C:\\Users\\S\\Documents\\中文檔名.txt", true, FieldValueType.File)
                                form.first { it.key == "text2" }.assert("value2", true)
                            }
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assert(ex.overrides!!.hasNoDisable())

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                    }

                    requests.first { it.name == "HTTP/2 Inherited Form" }.let {
                        assertEquals(ProtocolApplication.Http, it.application)
                        assertEquals("POST", it.method)
                        assertEquals("\${{prefix2}}/rest/echo", it.url)
                        assertEquals(5, it.examples.size)
                        val baseEx = it.examples.first()
                        var i = 0
                        it.examples[i++].let { ex -> // Base Example
                            assertEquals(ContentType.Multipart, ex.contentType)
                            (ex.body as MultipartBody).value.let { form ->
                                assertEquals(2, form.size)
                                form.first { it.key == "key1" }.assert("val1", true)
                                form.first { it.key == "fil2" }.assert("C:\\Users\\S\\Documents\\中文檔名.txt", true, FieldValueType.File)
                            }
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(1, ex.headers.size)
                            ex.headers.first().assert("auth", "abcDEF", true)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Inherited", ex.name)
                            assertEquals(ContentType.Multipart, ex.contentType)
                            val body = (ex.body as MultipartBody).value
                            assertEquals(0, body.size)
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assert(ex.overrides!!.hasNoDisable())

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Inherited with overrides", ex.name)
                            assertEquals(ContentType.Multipart, ex.contentType)
                            assert(ex.overrides!!.disabledBodyKeyValueIds == (baseEx.body as MultipartBody)
                                .value.filter { it.key == "fil2" }.map { it.id }.toSet())
                            (ex.body as MultipartBody).value.let { form ->
                                assertEquals(1, form.size)
                                form.first { it.key == "file2" }.assert("C:\\Users\\S\\Downloads\\serverCACert.pem", true, FieldValueType.File)
                            }
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assert(ex.overrides!!.disabledHeaderIds.isEmpty())
                            assert(ex.overrides!!.disabledQueryParameterIds.isEmpty())
                            assert(ex.overrides!!.disablePostFlightUpdateVarIds.isEmpty())

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("Form (different type cannot inherit)", ex.name)
                            assertEquals(ContentType.FormUrlEncoded, ex.contentType)
                            (ex.body as FormUrlEncodedBody).value.let { form ->
                                assertEquals(1, form.size)
                                form.first { it.key == "abc" }.assert("def", true)
                            }
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assert(ex.overrides!!.hasNoDisable())

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                        it.examples[i++].let { ex ->
                            assertEquals("json", ex.name)
                            assertEquals(ContentType.Json, ex.contentType)
                            assertEquals("{\"abc\": \"de\"}", (ex.body as StringBody).value)
                            assertEquals(true, ex.overrides!!.isOverrideBody)
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)
                            assert(ex.overrides!!.hasNoDisable())

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                    }

                    requests.first { it.name == "Custom Method" }.let {
                        assertEquals(ProtocolApplication.Http, it.application)
                        assertEquals("CUS", it.method)
                        assertEquals("\${{prefix2}}/rest/echo", it.url)
                        assertEquals(1, it.examples.size)
                        it.examples.first().let { ex ->
                            assertEquals(ContentType.None, ex.contentType)
                            assertEquals(0, ex.queryParameters.size)
                            assertEquals(0, ex.headers.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromHeader.size)
                            assertEquals(0, ex.postFlight.updateVariablesFromBody.size)

                            responses[ex.id]!!.assertHttpStatus200WithContent()
                        }
                    }
                }

                project.subprojects.first { it.name == "gRPC only" }.let { subproject ->
                    // TODO add asserts
                }

                project.subprojects.first { it.name == "Without Env" }.let { subproject ->
                    // TODO add asserts
                }

                project.subprojects.first { it.name == "Without Requests" }.let { subproject ->
                    // TODO add asserts
                }

                project.subprojects.first { it.name == "Mixed Request Types" }.let { subproject ->
                    // TODO add asserts
                }
            }
        }
    }
}

private fun UserResponse.assertHttpStatus200WithContent() {
    assertEquals(200, statusCode)
    assert(headers!!.isNotEmpty())
    assert(body!!.isNotEmpty())
}

private fun UserKeyValuePair.assert(value: String, isEnabled: Boolean, valueType: FieldValueType = FieldValueType.String) {
    assertEquals(value, this.value)
    assertEquals(valueType, this.valueType)
    assertEquals(isEnabled, this.isEnabled)
}

private fun UserKeyValuePair.assert(key: String, value: String, isEnabled: Boolean, valueType: FieldValueType = FieldValueType.String) {
    assertEquals(key, this.key)
    assert(value = value, isEnabled = isEnabled, valueType = valueType)
}
