package com.sunnychung.application.multiplatform.hellohttp.exporter.apidoc

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.model.ClientCertificateKeyPair
import com.sunnychung.application.multiplatform.hellohttp.model.ColourTheme
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpConfig
import com.sunnychung.application.multiplatform.hellohttp.model.ImportedFile
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadExample
import com.sunnychung.application.multiplatform.hellohttp.model.PayloadMessage
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeObject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.util.formatByteSize
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import java.io.File

class ApiDocHtmlExporter {

    private val jsonWriter = jacksonObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    private val markdownRenderer = ApiDocMarkdownRenderer()

    suspend fun exportSubprojectToDirectory(
        project: Project,
        subproject: Subproject,
        parentDirectory: File,
    ): File {
        if (!parentDirectory.isDirectory) {
            throw IllegalArgumentException("Destination must be a directory")
        }

        val requests = AppContext.RequestCollectionRepository
            .read(RequestsDI(subprojectId = subproject.id))
            ?.requests
            .orEmpty()

        val responsesByRequestExampleId = AppContext.ResponseCollectionRepository
            .read(ResponsesDI(subprojectId = subproject.id))
            ?.responsesByRequestExampleId
            .orEmpty()

        val defaultTheme = AppContext.UserPreferenceRepository
            .read(UserPreferenceDI())
            ?.preference
            ?.colourTheme
            ?: ColourTheme.Dark

        val html = exportAsHtml(
            project = project,
            subproject = subproject,
            requests = requests,
            responsesByRequestExampleId = responsesByRequestExampleId,
            exportedAt = KInstant.now(),
            defaultTheme = defaultTheme,
            appVersion = AppContext.MetadataManager.version,
            gitCommitHash = AppContext.MetadataManager.gitCommitHash,
        )

        val nowText = KZonedInstant.nowAtLocalZoneOffset().format("yyyy-MM-dd--HH-mm-ss")
        val exportDirName = createExportDirectoryName(
            projectName = project.name,
            subprojectName = subproject.name,
            dateTimeText = nowText,
        )
        val outputDirectory = createUniqueDirectory(parentDirectory, exportDirName)
        if (!outputDirectory.mkdirs()) {
            throw IllegalStateException("Cannot create export directory: ${outputDirectory.absolutePath}")
        }

        val htmlFile = File(outputDirectory, "index.html")
        htmlFile.writeText(html)

        return outputDirectory
    }

    internal fun exportAsHtml(
        project: Project,
        subproject: Subproject,
        requests: List<UserRequestTemplate>,
        responsesByRequestExampleId: Map<String, UserResponse>,
        exportedAt: KInstant,
        defaultTheme: ColourTheme,
        appVersion: String,
        gitCommitHash: String,
    ): String {
        val requestById = requests.associateBy { it.id }
        val occurrenceByRequestId = mutableMapOf<String, Int>()
        val (treeNodes, requestEntries) = collectRequestTree(
            treeObjects = subproject.treeObjects,
            requestById = requestById,
            occurrenceByRequestId = occurrenceByRequestId,
            folderPath = emptyList(),
        )

        val panels = mutableListOf<LinkedHashMap<String, Any>>()
        val overviewPanelId = "overview"
        val environmentsPanelId = "environments"

        panels += mapOfNotEmpty(
            "id" to overviewPanelId,
            "panelType" to "overview",
            "title" to "Overview",
            "subtitle" to "Project and Subproject Summary",
            "pathText" to "${project.name} / ${subproject.name}",
            "bodyHtml" to renderOverviewPanelBody(
                project = project,
                subproject = subproject,
                requests = requests,
                requestEntries = requestEntries,
                exportedAt = exportedAt,
                appVersion = appVersion,
                gitCommitHash = gitCommitHash,
            ),
            "detailsHtml" to renderOverviewPanelDetails(
                requestEntries = requestEntries,
            ),
        )

        panels += mapOfNotEmpty(
            "id" to environmentsPanelId,
            "panelType" to "environment",
            "title" to "Environments",
            "subtitle" to null,
            "pathText" to "${project.name} / ${subproject.name}",
            "bodyHtml" to renderEnvironmentsPanelBody(subproject.environments),
            "detailsHtml" to renderEnvironmentsPanelDetails(subproject.environments),
        )

        requestEntries.forEach { entry ->
            panels += mapOfNotEmpty(
                "id" to entry.itemId,
                "panelType" to "request",
                "title" to requestTitle(entry.request),
                "subtitle" to requestSubtitle(entry.request),
                "pathText" to requestPathText(entry),
                "bodyHtml" to renderRequestPanelBody(
                    entry = entry,
                ),
                "detailsHtml" to renderRequestPanelDetails(
                    entry = entry,
                    responsesByRequestExampleId = responsesByRequestExampleId,
                ),
            )
        }

        val navNodes = mutableListOf<LinkedHashMap<String, Any>>()
        navNodes += mapOfNotEmpty(
            "type" to "section",
            "itemId" to overviewPanelId,
            "label" to "Overview",
            "searchText" to "overview project subproject metadata",
        )
        navNodes += mapOfNotEmpty(
            "type" to "section",
            "itemId" to environmentsPanelId,
            "label" to "Environments",
            "searchText" to "environments variables ssl cookies user files",
        )
        navNodes += treeNodes

        val data = mapOfNotEmpty(
            "meta" to mapOfNotEmpty(
                "projectId" to project.id,
                "projectName" to project.name,
                "subprojectId" to subproject.id,
                "subprojectName" to subproject.name,
                "appVersion" to appVersion,
                "gitCommitHash" to gitCommitHash,
                "exportedAt" to KDateTimeFormat.ISO8601_DATETIME.format(exportedAt),
                "exportedAtText" to formatInstant(exportedAt),
                "defaultTheme" to defaultTheme.name.lowercase(),
            ),
            "environments" to collectEnvironmentSummaries(subproject.environments),
            "navNodes" to navNodes,
            "panels" to panels,
        )

        val dataJson = jsonWriter.writeValueAsString(data)
            .replace("</script>", "<\\/script>")

        return ApiDocHtmlTemplate.render(dataJson = dataJson)
    }

    private fun renderOverviewPanelBody(
        project: Project,
        subproject: Subproject,
        requests: List<UserRequestTemplate>,
        requestEntries: List<RequestEntry>,
        exportedAt: KInstant,
        appVersion: String,
        gitCommitHash: String,
    ): String {
        val totalRequestEntries = requestEntries.size
        val distinctRequestCount = requests.size
        val protocolCount = ProtocolApplication.values().associateWith { application ->
            requests.count { it.application == application }
        }

        return buildString {
            append("<p class=\"api-paragraph\">")
            append("This file is a self-contained API documentation export from Hello HTTP. ")
            append("Use the request tree on the left to switch items, search endpoints, or switch to continuous mode for printing/PDF.")
            append("</p>")

            append("<div class=\"api-meta-grid\">")
            append(renderMetaCard("Project", project.name))
            append(renderMetaCard("Subproject", subproject.name))
            append(renderMetaCard("Exported At", formatInstant(exportedAt)))
            append(renderMetaCard("Hello HTTP", "v$appVersion ($gitCommitHash)"))
            append(renderMetaCard("Request Entries", totalRequestEntries.toString()))
            append(renderMetaCard("Unique Requests", distinctRequestCount.toString()))
            append("</div>")

            append("<h3 class=\"api-section-title\">Subproject Configuration</h3>")
            append("<div class=\"api-table-wrapper\"><table class=\"api-table\">")
            append("<thead><tr><th>Key</th><th>Value</th></tr></thead><tbody>")
            append(renderSimpleTableRow("Cookie Enabled", subproject.configuration.isCookieEnabled().toString()))
            append("</tbody></table></div>")

            append("<h3 class=\"api-section-title\">Protocol Coverage</h3>")
            append("<div class=\"api-meta-grid\">")
            append(renderMetaCard("HTTP", (protocolCount[ProtocolApplication.Http] ?: 0).toString()))
            append(renderMetaCard("WebSocket", (protocolCount[ProtocolApplication.WebSocket] ?: 0).toString()))
            append(renderMetaCard("GraphQL", (protocolCount[ProtocolApplication.Graphql] ?: 0).toString()))
            append(renderMetaCard("gRPC", (protocolCount[ProtocolApplication.Grpc] ?: 0).toString()))
            append("</div>")

            append(renderOverviewRequestsTable(requestEntries))
        }
    }

    private fun renderOverviewRequestsTable(requestEntries: List<RequestEntry>): String {
        if (requestEntries.isEmpty()) {
            return ""
        }
        return buildString {
            append("<h3 class=\"api-section-title\">Requests</h3>")
            append("<div class=\"api-table-wrapper\"><table class=\"api-table\">")
            append("<thead><tr><th>Name</th><th>Protocol</th><th>Method</th><th>Endpoint</th></tr></thead><tbody>")
            requestEntries.forEach { entry ->
                append("<tr>")
                append(renderCell(requestTitle(entry.request)))
                append(renderCell(entry.request.application.displayText()))
                append(renderCell(entry.request.method.ifBlank { "-" }))
                append(renderCell(requestEndpoint(entry.request)))
                append("</tr>")
            }
            append("</tbody></table></div>")
        }
    }

    private fun renderOverviewPanelDetails(
        requestEntries: List<RequestEntry>,
    ): String {
        if (requestEntries.isEmpty()) {
            return ""
        }
        return ""
    }

    private fun renderEnvironmentsPanelBody(environments: List<Environment>): String {
        if (environments.isEmpty()) {
            return ""
        }

        return buildString {
            append("<p class=\"api-paragraph\">")
            append("Sensitive file, certificate, and private-key content are omitted. ")
            append("Only enabled variables and non-expired, enabled cookies are shown.")
            append("</p>")

            environments.forEachIndexed { index, environment ->
                append("<section data-env-id=\"")
                append(escapeHtmlAttribute(environment.id))
                append("\">")
                append("<h3 class=\"api-section-title api-env-title\">Environment: ")
                append(escapeHtml(environment.name))
                append("</h3>")

                append("<div class=\"api-meta-grid\">")
                append(renderMetaCard("HTTP Protocol Version", (environment.httpConfig.protocolVersion ?: HttpConfig.HttpProtocolVersion.Negotiate).displayText()))
                append(renderMetaCard("Disable SSL Verification", yesNoDefault(environment.sslConfig.isInsecure, defaultValue = false)))
                append(renderMetaCard("Disable System CA Certificates", yesNoDefault(environment.sslConfig.isDisableSystemCaCertificates, defaultValue = false)))
                append("</div>")

                append(renderEnvironmentVariables(environment))
                append(renderEnvironmentSslData(environment))
                append(renderEnvironmentCookies(environment))
                append(renderEnvironmentUserFiles(environment))
                append("</section>")

                if (index != environments.lastIndex) {
                    append("<hr class=\"md-hr\" />")
                }
            }
        }
    }

    private fun renderEnvironmentsPanelDetails(environments: List<Environment>): String {
        if (environments.isEmpty()) {
            return ""
        }

        return buildString {
            environments.forEachIndexed { index, environment ->
                append("<section data-env-id=\"")
                append(escapeHtmlAttribute(environment.id))
                append("\">")
                append("<h3 class=\"api-section-title api-env-title\">Environment Detail: ")
                append(escapeHtml(environment.name))
                append("</h3>")
                append(renderEnvironmentVariables(environment))
                append(renderEnvironmentCookies(environment))
                append(renderEnvironmentUserFiles(environment))
                append("</section>")
                if (index != environments.lastIndex) {
                    append("<hr class=\"md-hr\" />")
                }
            }
        }
    }

    private fun renderEnvironmentVariables(environment: Environment): String {
        val enabledVariables = environment.variables.filter { it.isEnabled }
        if (enabledVariables.isEmpty()) {
            return ""
        }
        return buildString {
            append("<section class=\"api-variable-section\">")
            append("<h4 class=\"api-subsection-title\">Variables</h4>")
            append("<div class=\"api-table-wrapper\"><table class=\"api-table\">")
            append("<thead><tr><th>Key</th><th>Value</th></tr></thead><tbody>")
            enabledVariables.forEach { variable ->
                append("<tr>")
                append("<td>")
                append(escapeHtml(variable.key))
                append("</td>")
                append("<td>")
                append(escapeHtml(variable.value))
                append("</td>")
                append("</tr>")
            }
            append("</tbody></table></div>")
            append("</section>")
        }
    }

    private fun renderEnvironmentSslData(environment: Environment): String {
        val sslConfig = environment.sslConfig
        val trustedCaSection = renderImportedFilesTable(
            title = "Additional Trusted CA Certificates",
            files = sslConfig.trustedCaCertificates,
        )
        val clientPairSection = renderClientCertificatePairsTable(sslConfig.clientCertificateKeyPairs)
        if (trustedCaSection.isBlank() && clientPairSection.isBlank()) {
            return ""
        }
        return buildString {
            append("<h4 class=\"api-subsection-title\">SSL</h4>")
            append(trustedCaSection)
            append(clientPairSection)
        }
    }

    private fun renderEnvironmentCookies(environment: Environment): String {
        val cookies = environment.cookieJar.getAllNonExpiredCookies().filter { it.isEnabled }
        if (cookies.isEmpty()) {
            return ""
        }
        return buildString {
            append("<h4 class=\"api-subsection-title\">Cookies</h4>")
            append("<div class=\"api-table-wrapper\"><table class=\"api-table\">")
            append("<thead><tr><th>Domain</th><th>Path</th><th>Name</th><th>Value</th><th>Expires</th><th>Attributes</th></tr></thead><tbody>")
            cookies.forEach { cookie ->
                append("<tr>")
                append("<td>")
                append(escapeHtml(cookie.domain))
                append("</td>")
                append("<td>")
                append(escapeHtml(cookie.path))
                append("</td>")
                append("<td>")
                append(escapeHtml(cookie.name))
                append("</td>")
                append("<td>")
                append(escapeHtml(cookie.value))
                append("</td>")
                append("<td>")
                append(escapeHtml(cookie.expires?.let(::formatInstant) ?: "Session"))
                append("</td>")
                append("<td>")
                append(escapeHtml(cookie.toAttributeString().ifBlank { "-" }))
                append("</td>")
                append("</tr>")
            }
            append("</tbody></table></div>")
        }
    }

    private fun renderEnvironmentUserFiles(environment: Environment): String {
        return renderImportedFilesTable(
            title = "User Files",
            files = environment.userFiles,
        )
    }

    private fun renderImportedFilesTable(title: String, files: List<ImportedFile>): String {
        val enabledFiles = files.filter { it.isEnabled }
        if (enabledFiles.isEmpty()) {
            return ""
        }
        return buildString {
            append("<h4 class=\"api-subsection-title\">")
            append(escapeHtml(title))
            append("</h4>")
            append("<div class=\"api-table-wrapper\"><table class=\"api-table\">")
            append("<thead><tr><th>Name</th><th>Original Filename</th><th>Import Time</th><th>Size</th></tr></thead><tbody>")
            enabledFiles.forEach { file ->
                append("<tr>")
                append(renderCell(file.name))
                append(renderCell(file.originalFilename))
                append(renderCell(formatInstant(file.createdWhen)))
                append(renderCell(formatByteSize(file.content.size.toLong())))
                append("</tr>")
            }
            append("</tbody></table></div>")
        }
    }

    private fun renderClientCertificatePairsTable(pairs: List<ClientCertificateKeyPair>): String {
        val enabledPairs = pairs.filter { it.isEnabled }
        if (enabledPairs.isEmpty()) {
            return ""
        }
        return buildString {
            append("<h4 class=\"api-subsection-title\">Client Certificate Key Pairs</h4>")
            append("<div class=\"api-table-wrapper\"><table class=\"api-table\">")
            append("<thead><tr><th>Name</th><th>Import Time</th><th>Certificate File</th><th>Private Key File</th></tr></thead><tbody>")
            enabledPairs.forEach { pair ->
                append("<tr>")
                append(renderCell(pair.name))
                append(renderCell(formatInstant(pair.createdWhen)))
                append(renderCell(pair.certificate.originalFilename))
                append(renderCell(pair.privateKey.originalFilename))
                append("</tr>")
            }
            append("</tbody></table></div>")
        }
    }

    private fun renderRequestPanelBody(
        entry: RequestEntry,
    ): String {
        val request = entry.request
        val grpcInfo = request.grpc?.takeIf { request.application == ProtocolApplication.Grpc }

        val examples = request.examples
        val baseExample = examples.firstOrNull()

        return buildString {
            append("<div class=\"api-meta-grid\">")
            append(renderMetaCard("Protocol", request.application.displayText()))
            append("</div>")

            if (grpcInfo != null) {
                append("<h3 class=\"api-section-title\">gRPC</h3>")
                append("<div class=\"api-meta-grid\">")
                append(renderMetaCard("Service", grpcInfo.service.ifBlank { "-" }))
                append(renderMetaCard("Method", grpcInfo.method.ifBlank { "-" }))
                append("</div>")
            }

            append(renderPayloadExamples(request.payloadExamples))

            val documentedExamples = examples.map { example ->
                mergeExampleContent(
                    request = request,
                    baseExample = baseExample,
                    currentExample = example,
                )
            }.filter { it.documentation.isNotBlank() }

            if (documentedExamples.isNotEmpty()) {
                append("<h3 class=\"api-section-title\">Documentation</h3>")
                documentedExamples.forEach { mergedExample ->
                    append("<section class=\"api-example-block\" data-example-id=\"")
                    append(escapeHtmlAttribute(mergedExample.id))
                    append("\" data-example-name=\"")
                    append(escapeHtmlAttribute(mergedExample.name))
                    append("\">")
                    append("<h4 class=\"api-subsection-title api-example-title\">Example: ")
                    append(escapeHtml(mergedExample.name))
                    append("</h4>")
                    append(markdownRenderer.render(mergedExample.documentation))
                    append("</section>")
                }
            }
        }
    }

    private fun renderRequestPanelDetails(
        entry: RequestEntry,
        responsesByRequestExampleId: Map<String, UserResponse>,
    ): String {
        val request = entry.request
        val examples = request.examples
        val baseExample = examples.firstOrNull()

        return buildString {
            if (examples.isEmpty()) {
                return@buildString
            }

            examples.forEachIndexed { index, example ->
                val mergedExample = mergeExampleContent(
                    request = request,
                    baseExample = baseExample,
                    currentExample = example,
                )

                append("<section class=\"api-example-block\" data-example-id=\"")
                append(escapeHtmlAttribute(example.id))
                append("\" data-example-name=\"")
                append(escapeHtmlAttribute(example.name))
                append("\">")
                if (index > 0) {
                    append("<hr class=\"md-hr\" />")
                }
                append("<h3 class=\"api-section-title api-example-title\">Request Example: ")
                append(escapeHtml(example.name))
                append("</h3>")
                append(renderMetaCardGridOrEmpty("Content Type", mergedExample.contentType.displayText))
                append(renderHeadersTable(mergedExample))
                append(renderUserKeyValuePairsTable("Cookies", mergedExample.cookies))
                append(renderUserKeyValuePairsTable("Query Parameters", mergedExample.queryParameters))
                append(renderUserKeyValuePairsTable("Variables", mergedExample.variables))
                append(renderRequestBody(
                    body = mergedExample.body,
                    method = request.method,
                ))
                append(renderPreFlight(mergedExample.preFlight))
                append(renderPostFlight(mergedExample.postFlight))
                append(renderResponseDetails(
                    response = responsesByRequestExampleId[example.id],
                    requestMethod = request.method,
                ))
                append("</section>")
            }
        }
    }

    private fun renderPayloadExamples(payloadExamples: List<PayloadExample>?): String {
        val nonEmptyPayloadExamples = payloadExamples
            .orEmpty()
            .map { it to normalizeMultilineContent(it.body) }
            .filter { (_, body) -> body.isNotBlank() }
        if (nonEmptyPayloadExamples.isEmpty()) {
            return ""
        }

        return buildString {
            append("<h3 class=\"api-section-title\">Payload Examples</h3>")
            nonEmptyPayloadExamples.forEach { (payload, body) ->
                append("<section>")
                append("<h4 class=\"api-subsection-title\">")
                append(escapeHtml(payload.name))
                append("</h4>")
                append("<pre class=\"api-code\">")
                append(escapeHtml(body))
                append("</pre>")
                append("</section>")
            }
        }
    }

    private fun renderUserKeyValuePairsTable(title: String, entries: List<UserKeyValuePair>): String {
        val enabledEntries = entries.filter { it.isEnabled }
        if (enabledEntries.isEmpty()) {
            return ""
        }
        return buildString {
            append("<h4 class=\"api-subsection-title\">")
            append(escapeHtml(title))
            append("</h4>")
            append("<div class=\"api-table-wrapper\"><table class=\"api-table\">")
            append("<thead><tr><th>Key</th><th>Value</th><th>Value Type</th></tr></thead><tbody>")
            enabledEntries.forEach { entry ->
                append("<tr>")
                append(renderCell(entry.key))
                append(renderCell(entry.value))
                append(renderCell(entry.valueType.displayText()))
                append("</tr>")
            }
            append("</tbody></table></div>")
        }
    }

    private fun renderHeadersTable(example: MergedExampleContent): String {
        val enabledEntries = example.headers.filter { it.isEnabled }
        val rows = mutableListOf<DisplayKeyValueRow>()

        val hasContentTypeHeader = enabledEntries.any { it.key.equals("content-type", ignoreCase = true) }
        if (!hasContentTypeHeader && example.contentType.headerValue != null) {
            rows += DisplayKeyValueRow(
                key = "Content-Type",
                value = example.contentType.headerValue,
                valueType = "String",
                isGenerated = true,
            )
        }

        enabledEntries.forEach { entry ->
            rows += DisplayKeyValueRow(
                key = entry.key,
                value = entry.value,
                valueType = entry.valueType.displayText(),
                isGenerated = false,
            )
        }

        if (rows.isEmpty()) {
            return ""
        }

        return buildString {
            append("<h4 class=\"api-subsection-title\">Headers</h4>")
            append("<div class=\"api-table-wrapper\"><table class=\"api-table\">")
            append("<thead><tr><th>Key</th><th>Value</th><th>Value Type</th></tr></thead><tbody>")
            rows.forEach { row ->
                append("<tr")
                if (row.isGenerated) {
                    append(" class=\"api-generated-row\"")
                }
                append(">")
                append(renderCell(row.key))
                append(renderCell(row.value))
                append(renderCell(row.valueType))
                append("</tr>")
            }
            append("</tbody></table></div>")
        }
    }

    private fun renderRequestBody(body: UserRequestBody?, method: String): String {
        val shouldShowEmptyBody = method.uppercase() != "GET"
        if (body == null && !shouldShowEmptyBody) {
            return ""
        }

        return buildString {
            append("<h4 class=\"api-subsection-title\">Body</h4>")
            when (body) {
                null -> append("<div class=\"api-empty\">Empty body.</div>")
                is StringBody -> {
                    val value = normalizeMultilineContent(body.value)
                    if (value.isBlank()) {
                        append("<div class=\"api-empty\">Empty body.</div>")
                    } else {
                        append("<pre class=\"api-code\">")
                        append(escapeHtml(value))
                        append("</pre>")
                    }
                }

                is GraphqlBody -> {
                    append("<p class=\"api-paragraph\"><strong>Operation Name:</strong> ")
                    append(escapeHtml(body.operationName ?: "-"))
                    append("</p>")

                    append("<p class=\"api-paragraph\"><strong>Document</strong></p>")
                    append("<pre class=\"api-code\">")
                    append(escapeHtml(normalizeMultilineContent(body.document)))
                    append("</pre>")

                    append("<p class=\"api-paragraph\"><strong>Variables</strong></p>")
                    val variables = normalizeMultilineContent(body.variables)
                    if (variables.isBlank()) {
                        append("<div class=\"api-empty\">Empty variables.</div>")
                    } else {
                        append("<pre class=\"api-code\">")
                        append(escapeHtml(variables))
                        append("</pre>")
                    }
                }

                is FormUrlEncodedBody -> {
                    val table = renderUserKeyValuePairsTable("Form URL-Encoded", body.value)
                    append(if (table.isBlank()) "<div class=\"api-empty\">Empty body.</div>" else table)
                }

                is MultipartBody -> {
                    val table = renderUserKeyValuePairsTable("Multipart", body.value)
                    append(if (table.isBlank()) "<div class=\"api-empty\">Empty body.</div>" else table)
                }

                is FileBody -> {
                    append("<div class=\"api-table-wrapper\"><table class=\"api-table\">")
                    append("<thead><tr><th>File Path</th></tr></thead><tbody>")
                    append(renderSimpleTableRow(body.filePath ?: ""))
                    append("</tbody></table></div>")
                }
            }
        }
    }

    private fun renderPreFlight(preFlight: com.sunnychung.application.multiplatform.hellohttp.model.PreFlightSpec): String {
        if (!preFlight.isNotEmpty()) {
            return ""
        }
        return buildString {
            append("<h4 class=\"api-subsection-title\">Pre-Flight</h4>")
            if (preFlight.executeCode.isNotBlank()) {
                append("<p class=\"api-paragraph\"><strong>Execute Code</strong></p>")
                append("<pre class=\"api-code\">")
                append(escapeHtml(normalizeMultilineContent(preFlight.executeCode)))
                append("</pre>")
            }
            append(renderUserKeyValuePairsTable("Update Variables From Header", preFlight.updateVariablesFromHeader))
            append(renderUserKeyValuePairsTable("Update Variables From Query Parameters", preFlight.updateVariablesFromQueryParameters))
            append(renderUserKeyValuePairsTable("Update Variables From Body", preFlight.updateVariablesFromBody))
            append(renderUserKeyValuePairsTable("Update Variables From GraphQL Variables", preFlight.updateVariablesFromGraphqlVariables))
        }
    }

    private fun renderPostFlight(postFlight: com.sunnychung.application.multiplatform.hellohttp.model.PostFlightSpec): String {
        if (postFlight.updateVariablesFromHeader.isEmpty() && postFlight.updateVariablesFromBody.isEmpty()) {
            return ""
        }
        return buildString {
            append("<h4 class=\"api-subsection-title\">Post-Flight</h4>")
            append(renderUserKeyValuePairsTable("Update Variables From Header", postFlight.updateVariablesFromHeader))
            append(renderUserKeyValuePairsTable("Update Variables From Body", postFlight.updateVariablesFromBody))
        }
    }

    private fun renderResponseDetails(response: UserResponse?, requestMethod: String): String {
        val shouldShowEmptyContent = requestMethod.uppercase() != "GET"
        if (response == null) {
            return ""
        }
        return buildString {
            append("<h3 class=\"api-section-title api-response-title\">Response</h3>")
            append("<div class=\"api-no-env-resolve\">")
            append("<div class=\"api-meta-grid\">")
            append(renderMetaCard("Status", listOfNotNull(response.statusCode?.toString(), response.statusText).joinToString(separator = " ").ifBlank { "-" }))
            append(renderMetaCard("Application", response.application.displayText()))
            append(renderMetaCard("Protocol", response.protocol?.toString() ?: "-"))
            append(renderMetaCard("Is Error", response.isError.toString()))
            append(renderMetaCard("Start", response.startAt?.let(::formatInstant) ?: "-"))
            append(renderMetaCard("End", response.endAt?.let(::formatInstant) ?: "-"))
            append("</div>")

            response.errorMessage?.takeIf { it.isNotBlank() }?.let {
                append("<p class=\"api-paragraph\"><strong>Error:</strong> ")
                append(escapeHtml(it))
                append("</p>")
            }

            response.postFlightErrorMessage?.takeIf { it.isNotBlank() }?.let {
                append("<p class=\"api-paragraph\"><strong>Post-Flight Error:</strong> ")
                append(escapeHtml(it))
                append("</p>")
            }

            response.closeReason?.takeIf { it.isNotBlank() }?.let {
                append("<p class=\"api-paragraph\"><strong>Close Reason:</strong> ")
                append(escapeHtml(it))
                append("</p>")
            }

            append(renderStringPairTable("Response Headers", response.headers.orEmpty()))
            append(renderResponseBody(response, shouldShowEmptySection = shouldShowEmptyContent))
            append(renderPayloadExchanges(
                payloadExchanges = response.payloadExchanges.orEmpty(),
                shouldShowEmptySection = shouldShowEmptyContent,
            ))
            append("</div>")
        }
    }

    private fun renderStringPairTable(title: String, pairs: List<Pair<String, String>>): String {
        if (pairs.isEmpty()) {
            return ""
        }
        return buildString {
            append("<h5 class=\"api-subsection-title\">")
            append(escapeHtml(title))
            append("</h5>")
            append("<div class=\"api-table-wrapper\"><table class=\"api-table\">")
            append("<thead><tr><th>Key</th><th>Value</th></tr></thead><tbody>")
            pairs.forEach { pair ->
                append("<tr>")
                append(renderCell(pair.first))
                append(renderCell(pair.second))
                append("</tr>")
            }
            append("</tbody></table></div>")
        }
    }

    private fun renderResponseBody(response: UserResponse, shouldShowEmptySection: Boolean): String {
        val bodyBytes = response.body?.takeIf { it.isNotEmpty() }
            ?: response.payloadExchanges
                ?.asSequence()
                ?.filter { it.type == PayloadMessage.Type.IncomingData }
                ?.lastOrNull()
                ?.data

        if ((bodyBytes == null || bodyBytes.isEmpty()) && !shouldShowEmptySection) {
            return ""
        }

        return buildString {
            append("<h5 class=\"api-subsection-title\">Body</h5>")
            if (bodyBytes == null || bodyBytes.isEmpty()) {
                append("<div class=\"api-empty\">Empty body.</div>")
                return@buildString
            }
            append("<pre class=\"api-code\">")
            append(escapeHtml(normalizeMultilineContent(bodyBytes.decodeToString())))
            append("</pre>")
        }
    }

    private fun renderPayloadExchanges(payloadExchanges: List<PayloadMessage>, shouldShowEmptySection: Boolean): String {
        if (payloadExchanges.isEmpty() && !shouldShowEmptySection) {
            return ""
        }

        return buildString {
            append("<h5 class=\"api-subsection-title\">Streaming Payload Exchanges</h5>")
            if (payloadExchanges.isEmpty()) {
                append("<div class=\"api-empty\">Empty payload.</div>")
                return@buildString
            }
            append("<div class=\"api-table-wrapper\"><table class=\"api-table api-stream-table\">")
            append("<thead><tr><th class=\"api-col-time\">Time</th><th class=\"api-col-type\">Type</th><th class=\"api-col-data\">Data</th></tr></thead><tbody>")
            payloadExchanges.forEach { message ->
                append("<tr>")
                append("<td class=\"api-col-time\">")
                append(escapeHtml(formatInstant(message.instant)))
                append("</td>")
                append("<td class=\"api-col-type\">")
                append(escapeHtml(message.type.displayText()))
                append("</td>")
                append("<td class=\"api-col-data\">")
                val text = normalizeMultilineContent(message.data?.decodeToString() ?: "")
                if (message.type == PayloadMessage.Type.IncomingData || message.type == PayloadMessage.Type.OutgoingData) {
                    append("<code class=\"api-stream-data\">")
                    append(escapeHtml(text))
                    append("</code>")
                } else {
                    append(escapeHtml(text))
                }
                append("</td>")
                append("</tr>")
            }
            append("</tbody></table></div>")
        }
    }

    private fun requestPathText(entry: RequestEntry): String {
        val folderPathText = if (entry.folderPath.isEmpty()) {
            "(root)"
        } else {
            entry.folderPath.joinToString(separator = " / ")
        }
        return "Tree Path: $folderPathText"
    }

    private fun requestTitle(request: UserRequestTemplate): String {
        return request.name.ifBlank { "(Unnamed Request)" }
    }

    private fun requestSubtitle(request: UserRequestTemplate): String {
        val methodOrProtocol = request.method.ifBlank { request.application.displayText() }
        val endpoint = requestEndpoint(request)
        return "$methodOrProtocol  $endpoint"
    }

    private fun requestEndpoint(request: UserRequestTemplate): String {
        return when (request.application) {
            ProtocolApplication.Grpc -> {
                val grpc = request.grpc
                listOfNotNull(
                    grpc?.service?.takeIf { it.isNotBlank() },
                    grpc?.method?.takeIf { it.isNotBlank() },
                ).joinToString(separator = " / ").ifBlank { "-" }
            }

            else -> request.url.ifBlank { "-" }
        }
    }

    private fun mergeExampleContent(
        request: UserRequestTemplate,
        baseExample: UserRequestExample?,
        currentExample: UserRequestExample,
    ): MergedExampleContent {
        val isBaseExample = request.isExampleBase(currentExample) || baseExample == null || baseExample.id == currentExample.id
        val overrides = currentExample.overrides ?: UserRequestExample.Overrides()

        fun mergeValues(
            baseValues: List<UserKeyValuePair>,
            currentValues: List<UserKeyValuePair>,
            disabledBaseIds: Set<String>,
        ): List<UserKeyValuePair> {
            if (isBaseExample) {
                return currentValues.filter { it.isEnabled }
            }
            val filteredBase = baseValues.filter { it.isEnabled && it.id !in disabledBaseIds }
            val filteredCurrent = currentValues.filter { it.isEnabled }
            return filteredBase + filteredCurrent
        }

        val headers = mergeValues(
            baseValues = baseExample?.headers.orEmpty(),
            currentValues = currentExample.headers,
            disabledBaseIds = overrides.disabledHeaderIds,
        )
        val cookies = mergeValues(
            baseValues = baseExample?.cookies.orEmpty(),
            currentValues = currentExample.cookies,
            disabledBaseIds = overrides.disabledCookieIds,
        )
        val queryParameters = mergeValues(
            baseValues = baseExample?.queryParameters.orEmpty(),
            currentValues = currentExample.queryParameters,
            disabledBaseIds = overrides.disabledQueryParameterIds,
        )
        val variables = mergeValues(
            baseValues = baseExample?.variables.orEmpty(),
            currentValues = currentExample.variables,
            disabledBaseIds = overrides.disabledVariables,
        )

        val preFlight = com.sunnychung.application.multiplatform.hellohttp.model.PreFlightSpec(
            executeCode = if (isBaseExample || overrides.isOverridePreFlightScript) {
                currentExample.preFlight.executeCode
            } else {
                baseExample?.preFlight?.executeCode.orEmpty()
            },
            updateVariablesFromHeader = mergeValues(
                baseValues = baseExample?.preFlight?.updateVariablesFromHeader.orEmpty(),
                currentValues = currentExample.preFlight.updateVariablesFromHeader,
                disabledBaseIds = overrides.disablePreFlightUpdateVarIds,
            ),
            updateVariablesFromQueryParameters = mergeValues(
                baseValues = baseExample?.preFlight?.updateVariablesFromQueryParameters.orEmpty(),
                currentValues = currentExample.preFlight.updateVariablesFromQueryParameters,
                disabledBaseIds = overrides.disablePreFlightUpdateVarIds,
            ),
            updateVariablesFromBody = mergeValues(
                baseValues = baseExample?.preFlight?.updateVariablesFromBody.orEmpty(),
                currentValues = currentExample.preFlight.updateVariablesFromBody,
                disabledBaseIds = overrides.disablePreFlightUpdateVarIds,
            ),
            updateVariablesFromGraphqlVariables = mergeValues(
                baseValues = baseExample?.preFlight?.updateVariablesFromGraphqlVariables.orEmpty(),
                currentValues = currentExample.preFlight.updateVariablesFromGraphqlVariables,
                disabledBaseIds = overrides.disablePreFlightUpdateVarIds,
            ),
        )
        val postFlight = com.sunnychung.application.multiplatform.hellohttp.model.PostFlightSpec(
            updateVariablesFromHeader = mergeValues(
                baseValues = baseExample?.postFlight?.updateVariablesFromHeader.orEmpty(),
                currentValues = currentExample.postFlight.updateVariablesFromHeader,
                disabledBaseIds = overrides.disablePostFlightUpdateVarIds,
            ),
            updateVariablesFromBody = mergeValues(
                baseValues = baseExample?.postFlight?.updateVariablesFromBody.orEmpty(),
                currentValues = currentExample.postFlight.updateVariablesFromBody,
                disabledBaseIds = overrides.disablePostFlightUpdateVarIds,
            ),
        )

        val mergedBody = mergeBody(
            baseExample = baseExample,
            currentExample = currentExample,
            isBaseExample = isBaseExample,
            overrides = overrides,
            mergeValues = ::mergeBodyKeyValuePairs,
        )

        val documentation = if (isBaseExample || currentExample.overrides?.isOverrideDocumentation == true) {
            currentExample.documentation
        } else {
            baseExample?.documentation.orEmpty()
        }

        return MergedExampleContent(
            id = currentExample.id,
            name = currentExample.name,
            contentType = currentExample.contentType,
            headers = headers,
            cookies = cookies,
            queryParameters = queryParameters,
            variables = variables,
            body = mergedBody,
            preFlight = preFlight,
            postFlight = postFlight,
            documentation = documentation,
        )
    }

    private fun mergeBody(
        baseExample: UserRequestExample?,
        currentExample: UserRequestExample,
        isBaseExample: Boolean,
        overrides: UserRequestExample.Overrides,
        mergeValues: (
            base: List<UserKeyValuePair>,
            current: List<UserKeyValuePair>,
            disabledBaseIds: Set<String>,
            isBase: Boolean,
        ) -> List<UserKeyValuePair>,
    ): UserRequestBody? {
        val selectedBody = currentExample.body
        val baseBody = baseExample?.body
        if (selectedBody == null) {
            return if (!isBaseExample && !overrides.isOverrideBody) baseBody else null
        }

        return when (selectedBody) {
            is FormUrlEncodedBody -> {
                FormUrlEncodedBody(
                    mergeValues(
                        (baseBody as? FormUrlEncodedBody)?.value.orEmpty(),
                        selectedBody.value,
                        overrides.disabledBodyKeyValueIds,
                        isBaseExample,
                    )
                )
            }

            is MultipartBody -> {
                MultipartBody(
                    mergeValues(
                        (baseBody as? MultipartBody)?.value.orEmpty(),
                        selectedBody.value,
                        overrides.disabledBodyKeyValueIds,
                        isBaseExample,
                    )
                )
            }

            is GraphqlBody -> {
                val baseGraphqlBody = baseBody as? GraphqlBody
                GraphqlBody(
                    document = if (isBaseExample || overrides.isOverrideBodyContent) {
                        selectedBody.document
                    } else {
                        baseGraphqlBody?.document.orEmpty()
                    },
                    variables = if (isBaseExample || overrides.isOverrideBodyVariables) {
                        selectedBody.variables
                    } else {
                        baseGraphqlBody?.variables.orEmpty()
                    },
                    operationName = selectedBody.operationName,
                )
            }

            else -> {
                if (isBaseExample || overrides.isOverrideBody) {
                    selectedBody
                } else {
                    baseBody
                }
            }
        }
    }

    private fun mergeBodyKeyValuePairs(
        base: List<UserKeyValuePair>,
        current: List<UserKeyValuePair>,
        disabledBaseIds: Set<String>,
        isBase: Boolean,
    ): List<UserKeyValuePair> {
        if (isBase) {
            return current.filter { it.isEnabled }
        }
        return base.filter { it.isEnabled && it.id !in disabledBaseIds } +
            current.filter { it.isEnabled }
    }

    private fun collectRequestTree(
        treeObjects: List<TreeObject>,
        requestById: Map<String, UserRequestTemplate>,
        occurrenceByRequestId: MutableMap<String, Int>,
        folderPath: List<String>,
    ): Pair<List<LinkedHashMap<String, Any>>, List<RequestEntry>> {
        val nodes = mutableListOf<LinkedHashMap<String, Any>>()
        val entries = mutableListOf<RequestEntry>()

        treeObjects.forEach { treeObject ->
            when (treeObject) {
                is TreeFolder -> {
                    val nextFolderPath = folderPath + treeObject.name
                    val (childNodes, childEntries) = collectRequestTree(
                        treeObjects = treeObject.childs,
                        requestById = requestById,
                        occurrenceByRequestId = occurrenceByRequestId,
                        folderPath = nextFolderPath,
                    )
                    if (childNodes.isNotEmpty()) {
                        nodes += mapOfNotEmpty(
                            "type" to "folder",
                            "label" to treeObject.name,
                            "searchText" to nextFolderPath.joinToString(separator = " "),
                            "children" to childNodes,
                        )
                    }
                    entries += childEntries
                }

                is TreeRequest -> {
                    val request = requestById[treeObject.id] ?: return@forEach
                    val occurrence = (occurrenceByRequestId[request.id] ?: 0) + 1
                    occurrenceByRequestId[request.id] = occurrence
                    val itemId = "request-${request.id}-$occurrence"
                    val requestLabel = requestTitle(request)

                    nodes += mapOfNotEmpty(
                        "type" to "request",
                        "itemId" to itemId,
                        "label" to requestLabel,
                        "method" to request.method,
                        "application" to request.application.displayText(),
                        "searchText" to listOf(
                            requestLabel,
                            request.method,
                            request.url,
                            request.application.displayText(),
                            folderPath.joinToString(separator = " "),
                        ).joinToString(separator = " "),
                    )
                    entries += RequestEntry(
                        itemId = itemId,
                        request = request,
                        folderPath = folderPath,
                        occurrence = occurrence,
                    )
                }
            }
        }

        return nodes to entries
    }

    private fun renderMetaCard(label: String, value: String): String {
        return buildString {
            append("<div class=\"api-meta-card\">")
            append("<span class=\"api-meta-label\">")
            append(escapeHtml(label))
            append("</span>")
            append("<p class=\"api-meta-value\">")
            append(escapeHtml(value))
            append("</p>")
            append("</div>")
        }
    }

    private fun renderMetaCardGridOrEmpty(label: String, value: String): String {
        return "<div class=\"api-meta-grid\">${renderMetaCard(label, value)}</div>"
    }

    private fun renderCell(value: String): String {
        return "<td>${escapeHtml(value)}</td>"
    }

    private fun renderSimpleTableRow(key: String, value: String): String {
        return "<tr><td>${escapeHtml(key)}</td><td>${escapeHtml(value)}</td></tr>"
    }

    private fun renderSimpleTableRow(value: String): String {
        return "<tr><td>${escapeHtml(value)}</td></tr>"
    }

    private fun createExportDirectoryName(projectName: String, subprojectName: String, dateTimeText: String): String {
        return listOf(
            "HelloHTTP",
            "api-doc",
            sanitizeNameForFile(projectName),
            sanitizeNameForFile(subprojectName),
            dateTimeText,
        ).joinToString(separator = "_")
    }

    private fun sanitizeNameForFile(raw: String): String {
        return raw
            .replace("[^A-Za-z0-9]+".toRegex(), "-")
            .trim('-')
            .ifBlank { "Unnamed" }
    }

    private fun createUniqueDirectory(parentDirectory: File, initialName: String): File {
        var candidate = File(parentDirectory, initialName)
        var suffix = 2
        while (candidate.exists()) {
            candidate = File(parentDirectory, "$initialName-$suffix")
            suffix += 1
        }
        return candidate
    }

    private fun formatInstant(instant: KInstant): String {
        return instant.atLocalZoneOffset().format("yyyy-MM-dd HH:mm:ss (Z)")
    }

    private fun yesNoDefault(value: Boolean?, defaultValue: Boolean): String {
        return when (value) {
            true -> "Yes"
            false -> "No"
            null -> if (defaultValue) "Yes" else "No"
        }
    }

    private fun normalizeMultilineContent(text: String): String {
        val lines = text.split("\n")
        if (lines.size <= 1) {
            return text
        }
        val nonBlankTailLines = lines.drop(1).filter { it.isNotBlank() }
        if (nonBlankTailLines.isEmpty()) {
            return text
        }
        val tailIndent = nonBlankTailLines.minOf { line ->
            line.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
        }
        if (tailIndent <= 0) {
            return text
        }
        val normalizedLines = buildList {
            add(lines.first())
            lines.drop(1).forEach { line ->
                add(if (line.length >= tailIndent) line.drop(tailIndent) else line.trimStart())
            }
        }
        return normalizedLines.joinToString(separator = "\n")
    }

    private fun collectEnvironmentSummaries(environments: List<Environment>): List<LinkedHashMap<String, Any>> {
        return environments.map { environment ->
            val variables = linkedMapOf<String, String>()
            environment.variables
                .filter { it.isEnabled }
                .forEach { variables[it.key] = it.value }
            mapOfNotEmpty(
                "id" to environment.id,
                "name" to environment.name,
                "variables" to variables,
            )
        }
    }

    private fun escapeHtml(text: String): String {
        val builder = StringBuilder(text.length)
        text.forEach { ch ->
            when (ch) {
                '&' -> builder.append("&amp;")
                '<' -> builder.append("&lt;")
                '>' -> builder.append("&gt;")
                '"' -> builder.append("&quot;")
                '\'' -> builder.append("&#39;")
                else -> builder.append(ch)
            }
        }
        return builder.toString()
    }

    private fun escapeHtmlAttribute(text: String): String = escapeHtml(text)

    private fun mapOfNotEmpty(vararg pairs: Pair<String, Any?>): LinkedHashMap<String, Any> {
        val output = linkedMapOf<String, Any>()
        pairs.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is Collection<*> -> if (value.isNotEmpty()) output[key] = value
                is Map<*, *> -> if (value.isNotEmpty()) output[key] = value
                else -> output[key] = value
            }
        }
        return output
    }

    private data class RequestEntry(
        val itemId: String,
        val request: UserRequestTemplate,
        val folderPath: List<String>,
        val occurrence: Int,
    )

    private data class DisplayKeyValueRow(
        val key: String,
        val value: String,
        val valueType: String,
        val isGenerated: Boolean,
    )

    private data class MergedExampleContent(
        val id: String,
        val name: String,
        val contentType: com.sunnychung.application.multiplatform.hellohttp.model.ContentType,
        val headers: List<UserKeyValuePair>,
        val cookies: List<UserKeyValuePair>,
        val queryParameters: List<UserKeyValuePair>,
        val variables: List<UserKeyValuePair>,
        val body: UserRequestBody?,
        val preFlight: com.sunnychung.application.multiplatform.hellohttp.model.PreFlightSpec,
        val postFlight: com.sunnychung.application.multiplatform.hellohttp.model.PostFlightSpec,
        val documentation: String,
    )
}

private fun ProtocolApplication.displayText(): String {
    return when (this) {
        ProtocolApplication.Http -> "HTTP"
        ProtocolApplication.WebSocket -> "WebSocket"
        ProtocolApplication.Grpc -> "gRPC"
        ProtocolApplication.Graphql -> "GraphQL"
    }
}

private fun HttpConfig.HttpProtocolVersion.displayText(): String {
    return when (this) {
        HttpConfig.HttpProtocolVersion.Http1Only -> "HTTP/1 Only"
        HttpConfig.HttpProtocolVersion.Http2Only -> "HTTP/2 Only"
        HttpConfig.HttpProtocolVersion.Negotiate -> "Negotiate"
    }
}

private fun com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType.displayText(): String {
    return when (this) {
        com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType.String -> "String"
        com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType.File -> "File"
    }
}

private fun PayloadMessage.Type.displayText(): String {
    return name
        .replace("Data", "")
        .replace("([a-z])([A-Z])".toRegex(), "$1 $2")
}
