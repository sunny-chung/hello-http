package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.exporter.apidoc.ApiDocHtmlExporter
import com.sunnychung.application.multiplatform.hellohttp.model.ClientCertificateKeyPair
import com.sunnychung.application.multiplatform.hellohttp.model.ColourTheme
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.ImportedFile
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.network.util.Cookie
import com.sunnychung.application.multiplatform.hellohttp.network.util.CookieJar
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.extension.seconds
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiDocHtmlExporterTest {

    @Test
    fun shouldRedactSensitiveEnvironmentDataAndFilterEnvironmentValues() {
        val now = KInstant.now()

        val environment = Environment(
            id = "env-1",
            name = "Dev",
            variables = mutableListOf(
                UserKeyValuePair(id = "v-1", key = "ENABLED_VAR", value = "hello", valueType = com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType.String, isEnabled = true),
                UserKeyValuePair(id = "v-2", key = "DISABLED_SECRET", value = "should-not-export", valueType = com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType.String, isEnabled = false),
            ),
            userFiles = listOf(
                ImportedFile(
                    id = "user-file-1",
                    name = "bundle",
                    originalFilename = "bundle.pem",
                    createdWhen = now,
                    isEnabled = true,
                    content = "very-sensitive-user-file-content".toByteArray(),
                )
            ),
            cookieJar = CookieJar(
                listOf(
                    Cookie(
                        name = "session",
                        value = "cookie-value",
                        domain = "example.com",
                        expires = now + 3600.seconds(),
                        isEnabled = true,
                    ),
                    Cookie(
                        name = "expired",
                        value = "old",
                        domain = "example.com",
                        expires = now + (-3600).seconds(),
                        isEnabled = true,
                    ),
                )
            ),
            sslConfig = com.sunnychung.application.multiplatform.hellohttp.model.SslConfig(
                trustedCaCertificates = listOf(
                    ImportedFile(
                        id = "ca-1",
                        name = "CA #1",
                        originalFilename = "ca-1.pem",
                        createdWhen = now,
                        isEnabled = true,
                        content = "very-sensitive-cert-content".toByteArray(),
                    )
                ),
                clientCertificateKeyPairs = listOf(
                    ClientCertificateKeyPair(
                        id = "client-1",
                        certificate = ImportedFile(
                            id = "cert-1",
                            name = "Client Cert",
                            originalFilename = "client-cert.pem",
                            createdWhen = now,
                            isEnabled = true,
                            content = "certificate-content".toByteArray(),
                        ),
                        privateKey = ImportedFile(
                            id = "key-1",
                            name = "Client Key",
                            originalFilename = "client-key.pem",
                            createdWhen = now,
                            isEnabled = true,
                            content = "very-sensitive-private-key-content".toByteArray(),
                        ),
                        createdWhen = now,
                        isEnabled = true,
                    )
                ),
            ),
        )

        val request = UserRequestTemplate(
            id = "request-1",
            name = "Get Users",
            application = ProtocolApplication.Http,
            method = "GET",
            url = "https://example.com/users",
            examples = listOf(
                UserRequestExample(
                    id = "example-1",
                    name = "Base",
                    body = StringBody(""),
                )
            ),
        )
        val subproject = Subproject(
            id = "subproject-1",
            name = "API",
            treeObjects = mutableListOf(TreeRequest(request.id)),
            environments = mutableListOf(environment),
        )
        val project = Project(
            id = "project-1",
            name = "Project",
            subprojects = mutableListOf(subproject),
        )

        val html = ApiDocHtmlExporter().exportAsHtml(
            project = project,
            subproject = subproject,
            requests = listOf(request),
            responsesByRequestExampleId = emptyMap(),
            exportedAt = now,
            defaultTheme = ColourTheme.Dark,
            appVersion = "1.0.0",
            gitCommitHash = "abcdef0",
        )

        assertTrue(html.contains("ENABLED_VAR"))
        assertFalse(html.contains("DISABLED_SECRET"))
        assertTrue(html.contains("session"))
        assertFalse(html.contains("\">expired<"))

        assertTrue(html.contains("bundle.pem"))
        assertTrue(html.contains("ca-1.pem"))
        assertTrue(html.contains("client-cert.pem"))
        assertTrue(html.contains("client-key.pem"))

        assertFalse(html.contains("very-sensitive-user-file-content"))
        assertFalse(html.contains("very-sensitive-cert-content"))
        assertFalse(html.contains("very-sensitive-private-key-content"))
    }

    @Test
    fun shouldFollowMarkdownRenderingRules() {
        val base64Png = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/s4QAAAAASUVORK5CYII="
        val markdown = """
            # Heading

            This is __underlined__ and <b>html</b>.

            ![inline]($base64Png)
            ![remote](https://example.com/image.png)

            [external link](https://example.com)
        """.trimIndent()

        val request = UserRequestTemplate(
            id = "request-md",
            name = "Markdown",
            application = ProtocolApplication.Http,
            method = "GET",
            url = "https://example.com/md",
            examples = listOf(
                UserRequestExample(
                    id = "example-md",
                    name = "Base",
                    body = StringBody(""),
                    documentation = markdown,
                )
            ),
        )

        val subproject = Subproject(
            id = "subproject-md",
            name = "Doc",
            treeObjects = mutableListOf(TreeRequest(request.id)),
            environments = mutableListOf(),
        )
        val project = Project(
            id = "project-md",
            name = "Project",
            subprojects = mutableListOf(subproject),
        )

        val html = ApiDocHtmlExporter().exportAsHtml(
            project = project,
            subproject = subproject,
            requests = listOf(request),
            responsesByRequestExampleId = emptyMap(),
            exportedAt = KInstant.now(),
            defaultTheme = ColourTheme.Dark,
            appVersion = "1.0.0",
            gitCommitHash = "abcdef0",
        )

        assertTrue(html.contains("md-inline-image"))
        assertTrue(html.contains("![remote](https://example.com/image.png)"))
        assertTrue(html.contains("md-underline"))
        assertTrue(html.contains("&lt;b&gt;html&lt;/b&gt;"))
        assertTrue(html.contains("data-external-link"))
    }

    @Test
    fun shouldRenderLineBreaksInMarkdownParagraphText() {
        val markdown = """
            O List
            1. a
            2. b
            3. c
              3.1. a
              3.2. b
            4. 4
        """.trimIndent()

        val request = UserRequestTemplate(
            id = "request-md-lines",
            name = "Markdown Lines",
            application = ProtocolApplication.Http,
            method = "GET",
            url = "https://example.com/md-lines",
            examples = listOf(
                UserRequestExample(
                    id = "example-md-lines",
                    name = "Base",
                    body = StringBody(""),
                    documentation = markdown,
                )
            ),
        )

        val subproject = Subproject(
            id = "subproject-md-lines",
            name = "Doc",
            treeObjects = mutableListOf(TreeRequest(request.id)),
            environments = mutableListOf(),
        )
        val project = Project(
            id = "project-md-lines",
            name = "Project",
            subprojects = mutableListOf(subproject),
        )

        val html = ApiDocHtmlExporter().exportAsHtml(
            project = project,
            subproject = subproject,
            requests = listOf(request),
            responsesByRequestExampleId = emptyMap(),
            exportedAt = KInstant.now(),
            defaultTheme = ColourTheme.Dark,
            appVersion = "1.0.0",
            gitCommitHash = "abcdef0",
        )

        assertTrue(html.contains("3.1. a"))
        assertTrue(html.contains("3.2. b"))
        assertTrue(html.contains("md-br"))
    }

    @Test
    fun shouldKeepDuplicateRequestEntriesInTree() {
        val request = UserRequestTemplate(
            id = "req-1",
            name = "Duplicate",
            application = ProtocolApplication.Http,
            method = "GET",
            url = "https://example.com/dup",
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    body = StringBody(""),
                )
            ),
        )

        val subproject = Subproject(
            id = "subproject-dup",
            name = "Subproject",
            treeObjects = mutableListOf(
                TreeRequest(request.id),
                TreeFolder(
                    id = "folder-1",
                    name = "Group",
                    childs = mutableListOf(TreeRequest(request.id)),
                ),
            ),
            environments = mutableListOf(),
        )
        val project = Project(
            id = "project-dup",
            name = "Project",
            subprojects = mutableListOf(subproject),
        )

        val html = ApiDocHtmlExporter().exportAsHtml(
            project = project,
            subproject = subproject,
            requests = listOf(request),
            responsesByRequestExampleId = emptyMap(),
            exportedAt = KInstant.now(),
            defaultTheme = ColourTheme.Dark,
            appVersion = "1.0.0",
            gitCommitHash = "abcdef0",
        )

        assertTrue(html.contains("request-req-1-1"))
        assertTrue(html.contains("request-req-1-2"))
    }
}
