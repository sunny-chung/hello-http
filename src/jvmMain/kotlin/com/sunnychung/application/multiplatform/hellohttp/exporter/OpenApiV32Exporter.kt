package com.sunnychung.application.multiplatform.hellohttp.exporter

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.helper.VariableResolver
import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.Project
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.Subproject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeFolder
import com.sunnychung.application.multiplatform.hellohttp.model.TreeObject
import com.sunnychung.application.multiplatform.hellohttp.model.TreeRequest
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserResponse
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.lib.multiplatform.kdatetime.KDateTimeFormat
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import graphql.language.OperationDefinition
import java.io.File

class OpenApiV32Exporter {

    private val jsonWriter = jacksonObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)

    suspend fun exportToFile(project: Project, file: File) {
        val requestsBySubprojectId = project.subprojects.associate { subproject ->
            val requests = AppContext.RequestCollectionRepository.read(RequestsDI(subprojectId = subproject.id))
                ?.requests
                ?: emptyList()
            subproject.id to requests
        }
        val responsesBySubprojectId = project.subprojects.associate { subproject ->
            val responses = AppContext.ResponseCollectionRepository.read(ResponsesDI(subprojectId = subproject.id))
                ?.responsesByRequestExampleId
                ?: emptyMap()
            subproject.id to responses
        }
        jsonWriter.writeValue(file, exportAsDocument(project, requestsBySubprojectId, responsesBySubprojectId))
    }

    suspend fun exportAsJson(project: Project): String {
        val requestsBySubprojectId = project.subprojects.associate { subproject ->
            val requests = AppContext.RequestCollectionRepository.read(RequestsDI(subprojectId = subproject.id))
                ?.requests
                ?: emptyList()
            subproject.id to requests
        }
        val responsesBySubprojectId = project.subprojects.associate { subproject ->
            val responses = AppContext.ResponseCollectionRepository.read(ResponsesDI(subprojectId = subproject.id))
                ?.responsesByRequestExampleId
                ?: emptyMap()
            subproject.id to responses
        }
        return jsonWriter.writeValueAsString(exportAsDocument(project, requestsBySubprojectId, responsesBySubprojectId))
    }

    fun exportAsJson(project: Project, requestsBySubprojectId: Map<String, List<UserRequestTemplate>>): String {
        return jsonWriter.writeValueAsString(
            exportAsDocument(
                project = project,
                requestsBySubprojectId = requestsBySubprojectId,
                responsesBySubprojectId = emptyMap(),
            )
        )
    }

    fun exportAsJson(
        project: Project,
        requestsBySubprojectId: Map<String, List<UserRequestTemplate>>,
        responsesBySubprojectId: Map<String, Map<String, UserResponse>>,
    ): String {
        return jsonWriter.writeValueAsString(
            exportAsDocument(
                project = project,
                requestsBySubprojectId = requestsBySubprojectId,
                responsesBySubprojectId = responsesBySubprojectId,
            )
        )
    }

    internal fun exportAsDocument(
        project: Project,
        requestsBySubprojectId: Map<String, List<UserRequestTemplate>>,
        responsesBySubprojectId: Map<String, Map<String, UserResponse>>,
    ): LinkedHashMap<String, Any> {
        val paths = linkedMapOf<String, LinkedHashMap<String, Any>>()
        val tags = linkedMapOf<String, LinkedHashMap<String, Any>>()
        val skippedRequests = mutableListOf<LinkedHashMap<String, Any>>()
        val collisions = mutableListOf<LinkedHashMap<String, Any>>()

        project.subprojects.forEach { subproject ->
            val requestsById = requestsBySubprojectId[subproject.id]
                .orEmpty()
                .associateBy { it.id }
            val responsesByRequestExampleId = responsesBySubprojectId[subproject.id].orEmpty()

            flattenTreeRequests(subproject.treeObjects).forEach { location ->
                val request = requestsById[location.requestId]
                if (request == null) {
                    skippedRequests += mapOfNotEmpty(
                        "subprojectId" to subproject.id,
                        "requestId" to location.requestId,
                        "reason" to "Request object is missing.",
                    )
                    return@forEach
                }

                when (
                    val operationResult = buildOperation(
                        project = project,
                        subproject = subproject,
                        folderPath = location.folderPath,
                        request = request,
                        responsesByRequestExampleId = responsesByRequestExampleId,
                    )
                ) {
                    is BuildOperationResult.Skipped -> {
                        skippedRequests += mapOfNotEmpty(
                            "subprojectId" to subproject.id,
                            "requestId" to request.id,
                            "requestName" to request.name,
                            "reason" to operationResult.reason,
                            "details" to operationResult.details,
                        )
                    }

                    is BuildOperationResult.Success -> {
                        val pathItem = paths.getOrPut(operationResult.path) { linkedMapOf() }
                        if (pathItem.containsKey(operationResult.method)) {
                            val existingRequestId = (((pathItem[operationResult.method] as? Map<*, *>)
                                ?.get("x-hello-http") as? Map<*, *>)
                                ?.get("requestId") as? String)
                            collisions += mapOfNotEmpty(
                                "path" to operationResult.path,
                                "method" to operationResult.method,
                                "keptRequestId" to existingRequestId,
                                "droppedRequestId" to request.id,
                                "droppedRequestName" to request.name,
                                "subprojectId" to subproject.id,
                            )
                        } else {
                            pathItem[operationResult.method] = operationResult.operation
                            operationResult.tags.forEach { tag ->
                                tags.getOrPut(tag) {
                                    mapOfNotEmpty("name" to tag)
                                }
                            }
                        }
                    }
                }
            }
        }

        val servers = buildServers(project, requestsBySubprojectId)

        val document = mapOfNotEmpty(
//            "openapi" to "3.2.0",
            "openapi" to "3.1.0",
            "info" to mapOfNotEmpty(
                "title" to project.name,
                "version" to AppContext.MetadataManager.version,
            ),
            "servers" to servers,
            "tags" to tags.values.toList(),
            "paths" to paths,
            "x-hello-http" to mapOfNotEmpty(
                "projectId" to project.id,
                "exportedAt" to KDateTimeFormat.ISO8601_DATETIME.format(KInstant.now()),
                "subprojectConfigurations" to project.subprojects.map { subproject ->
                    mapOfNotEmpty(
                        "subprojectId" to subproject.id,
                        "subprojectName" to subproject.name,
                        "configuration" to mapOfNotEmpty(
                            "outboundPayloadStorageLimit" to subproject.configuration.outboundPayloadStorageLimit,
                            "inboundPayloadStorageLimit" to subproject.configuration.inboundPayloadStorageLimit,
                            "accumulatedOutboundDataStorageLimitPerCall" to subproject.configuration.accumulatedOutboundDataStorageLimitPerCall,
                            "accumulatedInboundDataStorageLimitPerCall" to subproject.configuration.accumulatedInboundDataStorageLimitPerCall,
                            "isCookieEnabled" to subproject.configuration.isCookieEnabled(),
                        ),
                    )
                },
                "skippedRequests" to skippedRequests,
                "collisions" to collisions,
            ),
        )
        document["paths"] = paths
        return document
    }

    private fun buildOperation(
        project: Project,
        subproject: Subproject,
        folderPath: List<String>,
        request: UserRequestTemplate,
        responsesByRequestExampleId: Map<String, UserResponse>,
    ): BuildOperationResult {
        if (request.examples.isEmpty()) {
            return BuildOperationResult.Skipped("No request examples are available.")
        }

        val method = when (request.application) {
            ProtocolApplication.Http -> request.method.trim().lowercase().takeIf { it in OPENAPI_HTTP_METHODS }
                ?: return BuildOperationResult.Skipped(
                    "Unsupported HTTP method `${request.method}` for OpenAPI path item."
                )

            ProtocolApplication.Graphql -> "post"
            else -> return BuildOperationResult.Skipped(
                "Unsupported protocol `${request.application}`. Only HTTP and GraphQL are exported to OpenAPI paths."
            )
        }

        val parsedUrl = parseRawUrl(request.url)
        val path = parsedUrl.path

        val baseExample = request.examples.first()
        val resolvedExamples = mutableListOf<ResolvedRequestExample>()
        val skippedExampleNames = mutableListOf<String>()

        request.examples.forEachIndexed { index, example ->
            val exampleKey = createExampleKey(example.name, index, resolvedExamples)
            val resolvedExample = resolveRequestExample(
                request = request,
                baseExample = baseExample,
                selectedExample = example,
                urlQueryParameters = parsedUrl.queryPairs,
                exampleKey = exampleKey,
            )

            if (resolvedExample.isGraphqlSubscription) {
                skippedExampleNames += example.name
            } else {
                resolvedExamples += resolvedExample
            }
        }

        if (resolvedExamples.isEmpty()) {
            val details = if (skippedExampleNames.isEmpty()) {
                null
            } else {
                "Skipped examples: ${skippedExampleNames.joinToString(", ")}"
            }
            return BuildOperationResult.Skipped(
                reason = "No exportable examples. GraphQL subscriptions are not represented in OpenAPI paths.",
                details = details,
            )
        }

        val pathParameterNames = PATH_PARAMETER_REGEX.findAll(path)
            .map { it.groupValues[1] }
            .toList()

        val documentedExampleKeys = mutableSetOf<String>()
        val operationDescription = resolvedExamples
            .firstOrNull { it.isBase && it.hasOwnDocumentation }
            ?.documentation
            ?.takeIf { it.isNotBlank() }
            ?.also {
                documentedExampleKeys += resolvedExamples.first { example -> example.isBase }.key
            }

        val requestBody = collectRequestBody(
            examples = resolvedExamples,
            documentedExampleKeys = documentedExampleKeys,
        )
        val parameters = collectOperationParameters(
            pathParameterNames = pathParameterNames,
            examples = resolvedExamples,
            documentedExampleKeys = documentedExampleKeys,
        )
        val responses = collectResponses(
            examples = resolvedExamples,
            responsesByRequestExampleId = responsesByRequestExampleId,
        )
        val operationTags = listOfNotNull(
            subproject.name,
            folderPath
                .takeIf { it.isNotEmpty() }
                ?.joinToString(" / ")
                ?.let { "${subproject.name} / $it" },
        )

        val operation = mapOfNotEmpty(
            "operationId" to createOperationId(project, subproject, request),
            "summary" to request.name.takeIf { it.isNotBlank() },
            "description" to operationDescription,
            "tags" to operationTags,
            "parameters" to parameters,
            "requestBody" to requestBody,
            "responses" to responses,
            "x-hello-http" to mapOfNotEmpty(
                "projectId" to project.id,
                "subprojectId" to subproject.id,
                "requestId" to request.id,
                "requestName" to request.name,
                "application" to request.application.name,
                "folderPath" to folderPath,
                "skippedExamples" to skippedExampleNames,
                "examples" to resolvedExamples.map { it.toExtensionObject() },
            ),
        )

        return BuildOperationResult.Success(
            path = path,
            method = method,
            operation = operation,
            tags = operationTags,
        )
    }

    private fun collectOperationParameters(
        pathParameterNames: List<String>,
        examples: List<ResolvedRequestExample>,
        documentedExampleKeys: MutableSet<String>,
    ): List<LinkedHashMap<String, Any>> {
        val accumulators = linkedMapOf<ParameterIdentifier, ParameterAccumulator>()

        pathParameterNames.forEach { pathParameterName ->
            val identifier = ParameterIdentifier(pathParameterName, "path")
            accumulators[identifier] = ParameterAccumulator(
                name = pathParameterName,
                location = "path",
                isRequired = true,
            )
        }

        examples.forEach { example ->
            example.queryParameters
                .toLatestValueMap()
                .forEach { (name, value) ->
                    addParameterExample(
                        accumulators = accumulators,
                        name = name,
                        location = "query",
                        value = value,
                        example = example,
                        description = claimDocumentation(example, documentedExampleKeys),
                    )
                }
            example.headers
                .toLatestValueMap()
                .forEach { (name, value) ->
                    if (name.equals("Content-Type", ignoreCase = true)) return@forEach
                    addParameterExample(
                        accumulators = accumulators,
                        name = name,
                        location = "header",
                        value = value,
                        example = example,
                        description = claimDocumentation(example, documentedExampleKeys),
                    )
                }
            example.cookies
                .toLatestValueMap()
                .forEach { (name, value) ->
                    addParameterExample(
                        accumulators = accumulators,
                        name = name,
                        location = "cookie",
                        value = value,
                        example = example,
                        description = claimDocumentation(example, documentedExampleKeys),
                    )
                }

            pathParameterNames.forEach { parameterName ->
                example.variables[parameterName]?.let { value ->
                    accumulators[ParameterIdentifier(parameterName, "path")]
                        ?.examples
                        ?.put(
                            example.key,
                            createExampleObject(
                                summary = example.name,
                                description = claimDocumentation(example, documentedExampleKeys),
                                value = value,
                            )
                        )
                }
            }
        }

        return accumulators.values.map { accumulator ->
            mapOfNotEmpty(
                "name" to accumulator.name,
                "in" to accumulator.location,
                "required" to accumulator.isRequired.takeIf { it },
                "schema" to stringSchema(),
                "examples" to accumulator.examples,
            )
        }
    }

    private fun addParameterExample(
        accumulators: LinkedHashMap<ParameterIdentifier, ParameterAccumulator>,
        name: String,
        location: String,
        value: String,
        example: ResolvedRequestExample,
        description: String?,
    ) {
        if (name.isBlank()) return
        val identifier = ParameterIdentifier(name, location)
        val accumulator = accumulators.getOrPut(identifier) {
            ParameterAccumulator(
                name = name,
                location = location,
                isRequired = location == "path",
            )
        }
        accumulator.examples[example.key] = createExampleObject(
            summary = example.name,
            description = description,
            value = value,
        )
    }

    private fun collectRequestBody(
        examples: List<ResolvedRequestExample>,
        documentedExampleKeys: MutableSet<String>,
    ): LinkedHashMap<String, Any>? {
        val mediaTypeAccumulators = linkedMapOf<String, MediaTypeAccumulator>()

        examples.forEach { example ->
            val body = example.body
            if (body != null) {
                val mediaTypeAccumulator = mediaTypeAccumulators.getOrPut(body.mediaType) {
                    MediaTypeAccumulator()
                }
                mediaTypeAccumulator.schema = mergeSchemas(mediaTypeAccumulator.schema, body.schema)
                mediaTypeAccumulator.examples[example.key] = createExampleObject(
                    summary = example.name,
                    description = claimDocumentation(example, documentedExampleKeys),
                    value = body.value,
                )
            }
        }

        val examplesWithoutBodyAndParameters = examples.filter {
            it.body == null && it.headers.isEmpty() && it.cookies.isEmpty() && it.queryParameters.isEmpty()
        }

        if (mediaTypeAccumulators.isEmpty() && examplesWithoutBodyAndParameters.isNotEmpty()) {
            mediaTypeAccumulators["text/plain"] = MediaTypeAccumulator(schema = stringSchema())
        }

        if (mediaTypeAccumulators.isNotEmpty() && examplesWithoutBodyAndParameters.isNotEmpty()) {
            val defaultMediaType = mediaTypeAccumulators.keys.first()
            val defaultAccumulator = mediaTypeAccumulators[defaultMediaType]!!
            examplesWithoutBodyAndParameters.forEach { example ->
                if (defaultAccumulator.examples.containsKey(example.key)) return@forEach
                defaultAccumulator.examples[example.key] = createExampleObject(
                    summary = example.name,
                    description = claimDocumentation(example, documentedExampleKeys),
                    value = "",
                )
            }
        }

        if (mediaTypeAccumulators.isEmpty()) {
            return null
        }

        val contents = linkedMapOf<String, LinkedHashMap<String, Any>>()
        mediaTypeAccumulators.forEach { (mediaType, accumulator) ->
            contents[mediaType] = mapOfNotEmpty(
                "schema" to accumulator.schema,
                "examples" to accumulator.examples,
            )
        }

        return mapOfNotEmpty(
            "content" to contents,
        )
    }

    private fun collectResponses(
        examples: List<ResolvedRequestExample>,
        responsesByRequestExampleId: Map<String, UserResponse>,
    ): LinkedHashMap<String, Any> {
        val responseAccumulators = linkedMapOf<String, ResponseAccumulator>()

        examples.forEach { example ->
            val response = responsesByRequestExampleId[example.id] ?: return@forEach
            val statusCodeKey = response.statusCode?.toString() ?: "default"
            val responseAccumulator = responseAccumulators.getOrPut(statusCodeKey) {
                ResponseAccumulator(
                    description = response.statusText
                        ?.takeIf { it.isNotBlank() }
                        ?: if (response.isError) {
                            response.errorMessage?.takeIf { it.isNotBlank() } ?: "Error response"
                        } else {
                            "Response"
                        }
                )
            }

            val bodyBytes = extractResponseBody(response)
            if (bodyBytes != null && bodyBytes.isNotEmpty()) {
                val mediaType = response.headers
                    ?.asSequence()
                    ?.firstOrNull { it.first.equals("content-type", ignoreCase = true) }
                    ?.second
                    ?.substringBefore(';')
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "text/plain"
                val bodyText = bodyBytes.decodeToString()
                val bodyValue: Any = if (isJsonMediaType(mediaType)) {
                    parseJsonOrFallbackToString(bodyText)
                } else {
                    bodyText
                }

                val mediaTypeAccumulator = responseAccumulator.content.getOrPut(mediaType) {
                    MediaTypeAccumulator()
                }
                mediaTypeAccumulator.schema = mergeSchemas(mediaTypeAccumulator.schema, inferSchemaFromValue(bodyValue))
                mediaTypeAccumulator.examples[example.key] = createExampleObject(
                    summary = example.name,
                    description = null,
                    value = bodyValue,
                )
            }
        }

        if (responseAccumulators.isEmpty()) {
            return mapOfNotEmpty(
                "default" to mapOfNotEmpty(
                    "description" to "Response",
                )
            )
        }

        val responses = linkedMapOf<String, Any>()
        responseAccumulators.forEach { (statusCode, responseAccumulator) ->
            val responseObject = mapOfNotEmpty(
                "description" to responseAccumulator.description,
            )
            if (responseAccumulator.content.isNotEmpty()) {
                val responseMediaTypes = linkedMapOf<String, LinkedHashMap<String, Any>>()
                responseAccumulator.content.forEach { (mediaType, mediaTypeAccumulator) ->
                    responseMediaTypes[mediaType] = mapOfNotEmpty(
                        "schema" to mediaTypeAccumulator.schema,
                        "examples" to mediaTypeAccumulator.examples,
                    )
                }
                responseObject["content"] = responseMediaTypes
            }
            responses[statusCode] = responseObject
        }

        return responses
    }

    private fun extractResponseBody(response: UserResponse): ByteArray? {
        val body = response.body
        if (body != null && body.isNotEmpty()) {
            return body
        }
        return response.payloadExchanges
            ?.asSequence()
            ?.filter { it.type == com.sunnychung.application.multiplatform.hellohttp.model.PayloadMessage.Type.IncomingData }
            ?.lastOrNull()
            ?.data
    }

    private fun resolveRequestExample(
        request: UserRequestTemplate,
        baseExample: UserRequestExample,
        selectedExample: UserRequestExample,
        urlQueryParameters: List<ResolvedKeyValue>,
        exampleKey: String,
    ): ResolvedRequestExample {
        val isBaseExample = request.isExampleBase(selectedExample)
        val override = selectedExample.overrides

        val headers = mergeKeyValuePairs(
            baseValues = baseExample.headers,
            selectedValues = selectedExample.headers,
            disabledBaseIds = override?.disabledHeaderIds,
            isBaseExample = isBaseExample,
        ).toMutableList()

        val cookies = mergeKeyValuePairs(
            baseValues = baseExample.cookies,
            selectedValues = selectedExample.cookies,
            disabledBaseIds = override?.disabledCookieIds,
            isBaseExample = isBaseExample,
        )

        val queryParameters = (urlQueryParameters + mergeKeyValuePairs(
            baseValues = baseExample.queryParameters,
            selectedValues = selectedExample.queryParameters,
            disabledBaseIds = override?.disabledQueryParameterIds,
            isBaseExample = isBaseExample,
        ))

        val body = resolveBody(request, baseExample, selectedExample, isBaseExample)
        var isGraphqlSubscription = false
        var resolvedBody = body

        if (request.application == ProtocolApplication.Graphql) {
            val graphqlBody = body as? GraphqlResolvedBody
            isGraphqlSubscription = graphqlBody?.isSubscription == true
            if (!isGraphqlSubscription) {
                headers.removeAll {
                    it.key.equals("Content-Type", ignoreCase = true) ||
                        it.key.equals("Accept", ignoreCase = true)
                }
                headers += ResolvedKeyValue("Content-Type", "application/json")
                headers += ResolvedKeyValue("Accept", GRAPHQL_ACCEPT_HEADER)
                resolvedBody = ResolvedBody(
                    mediaType = "application/json",
                    value = mapOfNotEmpty(
                        "query" to graphqlBody?.document.orEmpty(),
                        "variables" to graphqlBody?.variables,
                        "operationName" to graphqlBody?.operationName,
                    ),
                    schema = mapOfNotEmpty(
                        "type" to "object",
                        "properties" to mapOfNotEmpty(
                            "query" to stringSchema(),
                            "variables" to mapOfNotEmpty(
                                "type" to "object",
                                "additionalProperties" to true,
                            ),
                            "operationName" to stringSchema(),
                        ),
                    ),
                )
            } else {
                resolvedBody = null
            }
        }

        val documentation = when {
            isBaseExample -> selectedExample.documentation
            selectedExample.overrides?.isOverrideDocumentation == true -> selectedExample.documentation
            else -> baseExample.documentation
        }
        val hasOwnDocumentation = if (isBaseExample) {
            selectedExample.documentation.isNotBlank()
        } else {
            selectedExample.overrides?.isOverrideDocumentation == true &&
                selectedExample.documentation.isNotBlank()
        }

        val preFlightExecuteCode = when {
            isBaseExample -> selectedExample.preFlight.executeCode
            selectedExample.overrides?.isOverridePreFlightScript != false -> selectedExample.preFlight.executeCode
            else -> baseExample.preFlight.executeCode
        }

        val preFlightUpdateFromHeader = mergeKeyValuePairs(
            baseValues = baseExample.preFlight.updateVariablesFromHeader,
            selectedValues = selectedExample.preFlight.updateVariablesFromHeader,
            disabledBaseIds = selectedExample.overrides?.disablePreFlightUpdateVarIds,
            isBaseExample = isBaseExample,
        )
        val preFlightUpdateFromQuery = mergeKeyValuePairs(
            baseValues = baseExample.preFlight.updateVariablesFromQueryParameters,
            selectedValues = selectedExample.preFlight.updateVariablesFromQueryParameters,
            disabledBaseIds = selectedExample.overrides?.disablePreFlightUpdateVarIds,
            isBaseExample = isBaseExample,
        )
        val preFlightUpdateFromBody = mergeKeyValuePairs(
            baseValues = baseExample.preFlight.updateVariablesFromBody,
            selectedValues = selectedExample.preFlight.updateVariablesFromBody,
            disabledBaseIds = selectedExample.overrides?.disablePreFlightUpdateVarIds,
            isBaseExample = isBaseExample,
        )
        val preFlightUpdateFromGraphqlVariables = mergeKeyValuePairs(
            baseValues = baseExample.preFlight.updateVariablesFromGraphqlVariables,
            selectedValues = selectedExample.preFlight.updateVariablesFromGraphqlVariables,
            disabledBaseIds = selectedExample.overrides?.disablePreFlightUpdateVarIds,
            isBaseExample = isBaseExample,
        )
        val postFlightUpdateFromHeader = mergeKeyValuePairs(
            baseValues = baseExample.postFlight.updateVariablesFromHeader,
            selectedValues = selectedExample.postFlight.updateVariablesFromHeader,
            disabledBaseIds = selectedExample.overrides?.disablePostFlightUpdateVarIds,
            isBaseExample = isBaseExample,
        )
        val postFlightUpdateFromBody = mergeKeyValuePairs(
            baseValues = baseExample.postFlight.updateVariablesFromBody,
            selectedValues = selectedExample.postFlight.updateVariablesFromBody,
            disabledBaseIds = selectedExample.overrides?.disablePostFlightUpdateVarIds,
            isBaseExample = isBaseExample,
        )

        return ResolvedRequestExample(
            id = selectedExample.id,
            key = exampleKey,
            name = selectedExample.name,
            isBase = isBaseExample,
            documentation = documentation,
            hasOwnDocumentation = hasOwnDocumentation,
            headers = headers,
            cookies = cookies,
            queryParameters = queryParameters,
            body = resolvedBody,
            variables = request.getExampleVariablesOnly(selectedExample.id),
            scripts = ResolvedScriptData(
                preFlightExecuteCode = preFlightExecuteCode,
                preFlightUpdateFromHeader = preFlightUpdateFromHeader,
                preFlightUpdateFromQuery = preFlightUpdateFromQuery,
                preFlightUpdateFromBody = preFlightUpdateFromBody,
                preFlightUpdateFromGraphqlVariables = preFlightUpdateFromGraphqlVariables,
                postFlightUpdateFromHeader = postFlightUpdateFromHeader,
                postFlightUpdateFromBody = postFlightUpdateFromBody,
            ),
            isGraphqlSubscription = isGraphqlSubscription,
        )
    }

    private fun resolveBody(
        request: UserRequestTemplate,
        baseExample: UserRequestExample,
        selectedExample: UserRequestExample,
        isBaseExample: Boolean,
    ): ResolvedBody? {
        return when (request.application) {
            ProtocolApplication.Graphql -> resolveGraphqlBody(
                baseExample = baseExample,
                selectedExample = selectedExample,
                isBaseExample = isBaseExample,
            )

            else -> resolveNonGraphqlBody(
                request = request,
                baseExample = baseExample,
                selectedExample = selectedExample,
                isBaseExample = isBaseExample,
            )
        }
    }

    private fun resolveGraphqlBody(
        baseExample: UserRequestExample,
        selectedExample: UserRequestExample,
        isBaseExample: Boolean,
    ): GraphqlResolvedBody {
        val baseBody = baseExample.body as? GraphqlBody
        val selectedBody = selectedExample.body as? GraphqlBody ?: baseBody
        val useSelectedContent = isBaseExample || selectedExample.overrides?.isOverrideBodyContent != false
        val useSelectedVariables = isBaseExample || selectedExample.overrides?.isOverrideBodyVariables == true
        val contentSource = if (useSelectedContent) selectedBody else baseBody
        val variablesSource = if (useSelectedVariables) selectedBody else baseBody
        val operationName = selectedBody?.operationName?.takeIf { it.isNotBlank() }
        val document = contentSource?.document ?: ""

        val operationType = contentSource
            ?.getOperation(isThrowError = false)
            ?.operation
        val isSubscription = operationType == OperationDefinition.Operation.SUBSCRIPTION

        val bodyValue = mapOfNotEmpty(
            "query" to document,
            "variables" to parseGraphqlVariablesOrFallbackToString(variablesSource?.variables ?: ""),
            "operationName" to operationName,
        )

        return GraphqlResolvedBody(
            isSubscription = isSubscription,
            document = document,
            variables = bodyValue["variables"],
            operationName = operationName,
            value = bodyValue,
            schema = mapOfNotEmpty(
                "type" to "object",
                "properties" to mapOfNotEmpty(
                    "query" to stringSchema(),
                    "variables" to mapOfNotEmpty(
                        "type" to "object",
                        "additionalProperties" to true,
                    ),
                    "operationName" to stringSchema(),
                ),
            ),
        )
    }

    private fun resolveNonGraphqlBody(
        request: UserRequestTemplate,
        baseExample: UserRequestExample,
        selectedExample: UserRequestExample,
        isBaseExample: Boolean,
    ): ResolvedBody? {
        val override = selectedExample.overrides
        val selectedBody = selectedExample.body
        val body = when (selectedBody) {
            is FormUrlEncodedBody -> FormUrlEncodedBody(
                mergeKeyValuePairs(
                    baseValues = (baseExample.body as? FormUrlEncodedBody)?.value.orEmpty(),
                    selectedValues = selectedBody.value,
                    disabledBaseIds = override?.disabledBodyKeyValueIds,
                    isBaseExample = isBaseExample,
                ).map {
                    UserKeyValuePair(
                        id = "",
                        key = it.key,
                        value = it.value,
                        valueType = it.valueType,
                        isEnabled = true,
                    )
                }
            )

            is MultipartBody -> MultipartBody(
                mergeKeyValuePairs(
                    baseValues = (baseExample.body as? MultipartBody)?.value.orEmpty(),
                    selectedValues = selectedBody.value,
                    disabledBaseIds = override?.disabledBodyKeyValueIds,
                    isBaseExample = isBaseExample,
                ).map {
                    UserKeyValuePair(
                        id = "",
                        key = it.key,
                        value = it.value,
                        valueType = it.valueType,
                        isEnabled = true,
                    )
                }
            )

            else -> if (!isBaseExample && override?.isOverrideBody == false) {
                baseExample.body
            } else {
                selectedBody
            }
        } ?: return null

        return when (body) {
            is StringBody -> {
                val mediaType = inferMediaType(selectedExample.contentType, selectedExample.headers)
                val bodyValue: Any = if (isJsonMediaType(mediaType)) {
                    parseJsonOrFallbackToString(body.value)
                } else {
                    body.value
                }
                ResolvedBody(
                    mediaType = mediaType,
                    value = bodyValue,
                    schema = inferSchemaFromValue(bodyValue),
                )
            }

            is FormUrlEncodedBody -> {
                val entries = body.value
                    .map {
                        ResolvedKeyValue(
                            key = it.key,
                            value = it.value,
                            valueType = it.valueType,
                        )
                    }
                ResolvedBody(
                    mediaType = "application/x-www-form-urlencoded",
                    value = entries.toLatestValueMap(),
                    schema = buildSchemaFromBodyEntries(entries, isMultipart = false),
                )
            }

            is MultipartBody -> {
                val entries = body.value
                    .map {
                        ResolvedKeyValue(
                            key = it.key,
                            value = it.value,
                            valueType = it.valueType,
                        )
                    }
                ResolvedBody(
                    mediaType = "multipart/form-data",
                    value = entries.toLatestValueMap(),
                    schema = buildSchemaFromBodyEntries(entries, isMultipart = true),
                )
            }

            is FileBody -> {
                val mediaType = inferMediaType(selectedExample.contentType, selectedExample.headers)
                ResolvedBody(
                    mediaType = mediaType,
                    value = body.filePath.orEmpty(),
                    schema = binarySchema(),
                )
            }

            is GraphqlBody -> {
                val bodyValue = mapOfNotEmpty(
                    "query" to body.document,
                    "variables" to parseGraphqlVariablesOrFallbackToString(body.variables),
                    "operationName" to body.operationName,
                )
                ResolvedBody(
                    mediaType = "application/json",
                    value = bodyValue,
                    schema = inferSchemaFromValue(bodyValue),
                )
            }
        }
    }

    private fun buildSchemaFromBodyEntries(
        entries: List<ResolvedKeyValue>,
        isMultipart: Boolean,
    ): LinkedHashMap<String, Any> {
        val properties = linkedMapOf<String, LinkedHashMap<String, Any>>()
        entries.forEach { entry ->
            if (entry.key.isBlank()) return@forEach
            properties[entry.key] = if (isMultipart && entry.valueType == FieldValueType.File) {
                binarySchema()
            } else {
                stringSchema()
            }
        }

        return mapOfNotEmpty(
            "type" to "object",
            "properties" to properties,
        )
    }

    private fun mergeKeyValuePairs(
        baseValues: List<UserKeyValuePair>,
        selectedValues: List<UserKeyValuePair>,
        disabledBaseIds: Set<String>?,
        isBaseExample: Boolean,
    ): List<ResolvedKeyValue> {
        val basePart = if (isBaseExample) {
            emptyList()
        } else {
            baseValues
                .filter { it.isEnabled && (disabledBaseIds == null || it.id !in disabledBaseIds) }
                .map {
                    ResolvedKeyValue(
                        key = it.key,
                        value = it.value,
                        valueType = it.valueType,
                    )
                }
        }
        val selectedPart = selectedValues
            .filter { it.isEnabled }
            .map {
                ResolvedKeyValue(
                    key = it.key,
                    value = it.value,
                    valueType = it.valueType,
                )
            }
        return basePart + selectedPart
    }

    private fun buildServers(
        project: Project,
        requestsBySubprojectId: Map<String, List<UserRequestTemplate>>,
    ): List<LinkedHashMap<String, Any>> {
        val servers = mutableListOf<LinkedHashMap<String, Any>>()
        val existing = mutableSetOf<String>()
        project.subprojects.forEach { subproject ->
            val requests = requestsBySubprojectId[subproject.id].orEmpty()
            subproject.environments.forEach { environment ->
                val url = SERVER_URL_VARIABLE_KEYS
                    .asSequence()
                    .mapNotNull { key ->
                        environment.variables.firstOrNull {
                            it.isEnabled && it.key.equals(key, ignoreCase = true)
                        }?.value
                    }
                    .mapNotNull { rawUrl ->
                        extractBaseUrlFromRawUrl(rawUrl)
                    }
                    .firstOrNull()
                    ?: requests.asSequence()
                        .filter { it.application in setOf(ProtocolApplication.Http, ProtocolApplication.Graphql) }
                        .mapNotNull { request ->
                            val exampleId = request.examples.firstOrNull()?.id ?: return@mapNotNull null
                            val resolvedUrl = VariableResolver(environment, request, exampleId).resolve(request.url)
                            extractBaseUrlFromRawUrl(resolvedUrl)
                        }
                        .firstOrNull()

                if (url != null) {
                    val key = "${subproject.id}/${environment.id}/$url"
                    if (existing.add(key)) {
                        servers += mapOfNotEmpty(
                            "url" to url,
                            "description" to "${subproject.name} / ${environment.name}",
                        )
                    }
                }
            }
        }
        return servers
    }

    private fun extractBaseUrlFromRawUrl(rawUrl: String): String? {
        val parsedUrl = parseRawUrl(rawUrl)
        val scheme = parsedUrl.scheme ?: return null
        val host = parsedUrl.host ?: return null
        if (scheme.isBlank() || host.isBlank()) return null
        return buildString {
            append(convertHelloVariableToOpenApiSyntax(scheme))
            append("://")
            append(convertHelloVariableToOpenApiSyntax(host))
            parsedUrl.port?.takeIf { it.isNotBlank() }?.let {
                append(':')
                append(it)
            }
        }
    }

    private fun flattenTreeRequests(
        treeObjects: List<TreeObject>,
        currentFolders: List<String> = emptyList(),
    ): List<TreeRequestLocation> {
        val output = mutableListOf<TreeRequestLocation>()
        treeObjects.forEach { treeObject ->
            when (treeObject) {
                is TreeRequest -> output += TreeRequestLocation(
                    requestId = treeObject.id,
                    folderPath = currentFolders,
                )

                is TreeFolder -> output += flattenTreeRequests(
                    treeObjects = treeObject.childs,
                    currentFolders = currentFolders + treeObject.name,
                )
            }
        }
        return output
    }

    private fun parseRawUrl(rawUrl: String): ParsedUrl {
        val match = URL_REGEX.matchEntire(rawUrl.trim())
        if (match == null) {
            return ParsedUrl(
                scheme = null,
                host = null,
                port = null,
                path = "/",
                queryPairs = emptyList(),
            )
        }

        val scheme = match.groupValues[1].ifBlank { null }
        val host = match.groupValues[2].ifBlank { null }
        val port = match.groupValues[3].ifBlank { null }
        val rawPath = match.groupValues[4]
        val rawQuery = match.groupValues[5]

        val path = normalizePath(convertHelloVariableToOpenApiSyntax(rawPath))
        val queryPairs = parseQueryPairs(rawQuery)

        return ParsedUrl(
            scheme = scheme,
            host = host,
            port = port,
            path = path,
            queryPairs = queryPairs,
        )
    }

    private fun parseQueryPairs(rawQuery: String): List<ResolvedKeyValue> {
        if (rawQuery.isBlank()) return emptyList()
        return rawQuery
            .split('&')
            .mapNotNull { entry ->
                if (entry.isBlank()) return@mapNotNull null
                val key = entry.substringBefore('=')
                val value = entry.substringAfter('=', "")
                key
                    .takeIf { it.isNotBlank() }
                    ?.let {
                        ResolvedKeyValue(
                            key = key,
                            value = value,
                        )
                    }
            }
    }

    private fun normalizePath(rawPath: String): String {
        val trimmedPath = rawPath.trim()
            .removePrefix("/")
            .removeSuffix("/")
        return if (trimmedPath.isBlank()) "/" else "/$trimmedPath"
    }

    private fun convertHelloVariableToOpenApiSyntax(subject: String): String {
        return subject.replace(HELLO_HTTP_VARIABLE_REGEX, "{$1}")
    }

    private fun inferMediaType(contentType: ContentType, headers: List<UserKeyValuePair>): String {
        val mediaTypeFromHeader = headers
            .asReversed()
            .firstOrNull { it.isEnabled && it.key.equals("content-type", ignoreCase = true) }
            ?.value
            ?.substringBefore(';')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return mediaTypeFromHeader ?: when (contentType) {
            ContentType.Json -> "application/json"
            ContentType.FormUrlEncoded -> "application/x-www-form-urlencoded"
            ContentType.Multipart -> "multipart/form-data"
            ContentType.BinaryFile -> "application/octet-stream"
            ContentType.Graphql -> "application/json"
            else -> "text/plain"
        }
    }

    private fun isJsonMediaType(mediaType: String): Boolean {
        val lowerMediaType = mediaType.lowercase()
        return lowerMediaType.contains("json") || lowerMediaType.endsWith("+json")
    }

    private fun parseJsonOrFallbackToString(rawBody: String): Any {
        return try {
            jsonWriter.readTree(rawBody)
        } catch (_: Throwable) {
            rawBody
        }
    }

    private fun parseGraphqlVariablesOrFallbackToString(rawVariables: String): Any? {
        if (rawVariables.isBlank()) {
            return null
        }
        return try {
            jsonWriter.readTree(rawVariables)
        } catch (_: Throwable) {
            rawVariables
        }
    }

    private fun inferSchemaFromValue(value: Any?): LinkedHashMap<String, Any> {
        return when (value) {
            is JsonNode -> inferSchemaFromJsonNode(value)
            is Map<*, *> -> mapOfNotEmpty(
                "type" to "object",
                "additionalProperties" to true,
            )
            is Collection<*> -> mapOfNotEmpty(
                "type" to "array",
                "items" to mapOfNotEmpty("type" to "string"),
            )
            is Number -> mapOfNotEmpty("type" to "number")
            is Boolean -> mapOfNotEmpty("type" to "boolean")
            else -> stringSchema()
        }
    }

    private fun inferSchemaFromJsonNode(node: JsonNode): LinkedHashMap<String, Any> {
        return when {
            node.isObject -> mapOfNotEmpty(
                "type" to "object",
                "additionalProperties" to true,
            )
            node.isArray -> mapOfNotEmpty(
                "type" to "array",
                "items" to mapOfNotEmpty("type" to "string"),
            )
            node.isIntegralNumber -> mapOfNotEmpty("type" to "integer")
            node.isFloatingPointNumber -> mapOfNotEmpty("type" to "number")
            node.isBoolean -> mapOfNotEmpty("type" to "boolean")
            node.isNull -> stringSchema()
            else -> stringSchema()
        }
    }

    private fun mergeSchemas(
        original: LinkedHashMap<String, Any>?,
        incoming: LinkedHashMap<String, Any>,
    ): LinkedHashMap<String, Any> {
        if (original == null) return incoming
        if (original == incoming) return original

        val originalType = original["type"] as? String
        val incomingType = incoming["type"] as? String
        if (originalType == "object" && incomingType == "object") {
            val originalProperties = (original["properties"] as? Map<*, *>)
                ?.entries
                ?.filter { it.key is String && it.value is Map<*, *> }
                ?.associate { it.key as String to (it.value as Map<*, *>) }
                ?.let { LinkedHashMap(it) }
                ?: linkedMapOf()
            val incomingProperties = (incoming["properties"] as? Map<*, *>)
                ?.entries
                ?.filter { it.key is String && it.value is Map<*, *> }
                ?.associate { it.key as String to (it.value as Map<*, *>) }
                ?: emptyMap()
            originalProperties.putAll(incomingProperties)
            val mergedSchema = LinkedHashMap(original)
            mergedSchema["properties"] = originalProperties
            return mergedSchema
        }
        return original
    }

    private fun createExampleObject(
        summary: String,
        description: String?,
        value: Any?,
    ): LinkedHashMap<String, Any> {
        return mapOfNotEmpty(
            "summary" to summary.takeIf { it.isNotBlank() },
            "description" to description?.takeIf { it.isNotBlank() },
            "value" to value,
        )
    }

    private fun claimDocumentation(
        example: ResolvedRequestExample,
        documentedExampleKeys: MutableSet<String>,
    ): String? {
        if (!example.hasOwnDocumentation || example.documentation.isBlank()) {
            return null
        }
        return if (documentedExampleKeys.add(example.key)) {
            example.documentation
        } else {
            null
        }
    }

    private fun createExampleKey(
        name: String,
        index: Int,
        existingExamples: List<ResolvedRequestExample>,
    ): String {
        val initialKey = name.lowercase()
            .replace("[^a-z0-9._-]+".toRegex(), "-")
            .trim('-')
            .ifBlank { "example-${index + 1}" }
        val existingKeys = existingExamples.map { it.key }.toSet()
        if (initialKey !in existingKeys) {
            return initialKey
        }
        var suffix = 2
        while ("$initialKey-$suffix" in existingKeys) {
            suffix += 1
        }
        return "$initialKey-$suffix"
    }

    private fun createOperationId(
        project: Project,
        subproject: Subproject,
        request: UserRequestTemplate,
    ): String {
        val candidate = "${project.name}_${subproject.name}_${request.name}_${request.id}"
        return candidate.replace("[^A-Za-z0-9._-]+".toRegex(), "_")
            .trim('_')
            .ifBlank { request.id }
    }

    private fun List<ResolvedKeyValue>.toLatestValueMap(): LinkedHashMap<String, String> {
        val output = linkedMapOf<String, String>()
        forEach {
            if (it.key.isBlank()) return@forEach
            output[it.key] = it.value
        }
        return output
    }

    private fun ResolvedRequestExample.toExtensionObject(): LinkedHashMap<String, Any> {
        val bodyObject = body?.let {
            mapOfNotEmpty(
                "mediaType" to it.mediaType,
                "value" to it.value,
            )
        } ?: mapOfNotEmpty(
            "isEmpty" to true,
            "value" to "",
        )

        return mapOfNotEmpty(
            "id" to id,
            "key" to key,
            "name" to name,
            "isBase" to isBase,
            "headers" to headers.map { mapOfNotEmpty("key" to it.key, "value" to it.value) },
            "cookies" to cookies.map { mapOfNotEmpty("key" to it.key, "value" to it.value) },
            "queryParameters" to queryParameters.map { mapOfNotEmpty("key" to it.key, "value" to it.value) },
            "body" to bodyObject,
            "scripts" to mapOfNotEmpty(
                "preFlight" to mapOfNotEmpty(
                    "executeCode" to scripts.preFlightExecuteCode,
                    "updateVariablesFromHeader" to scripts.preFlightUpdateFromHeader.map { mapOfNotEmpty("key" to it.key, "value" to it.value) },
                    "updateVariablesFromQuery" to scripts.preFlightUpdateFromQuery.map { mapOfNotEmpty("key" to it.key, "value" to it.value) },
                    "updateVariablesFromBody" to scripts.preFlightUpdateFromBody.map { mapOfNotEmpty("key" to it.key, "value" to it.value) },
                    "updateVariablesFromGraphqlVariables" to scripts.preFlightUpdateFromGraphqlVariables.map { mapOfNotEmpty("key" to it.key, "value" to it.value) },
                ),
                "postFlight" to mapOfNotEmpty(
                    "updateVariablesFromHeader" to scripts.postFlightUpdateFromHeader.map { mapOfNotEmpty("key" to it.key, "value" to it.value) },
                    "updateVariablesFromBody" to scripts.postFlightUpdateFromBody.map { mapOfNotEmpty("key" to it.key, "value" to it.value) },
                ),
            ),
        )
    }

    private fun mapOfNotEmpty(vararg pairs: Pair<String, Any?>): LinkedHashMap<String, Any> {
        val output = linkedMapOf<String, Any>()
        pairs.forEach { (key, value) ->
            when (value) {
                null -> {}
                is Collection<*> -> if (value.isNotEmpty()) output[key] = value
                is Map<*, *> -> if (value.isNotEmpty()) output[key] = value
                else -> output[key] = value
            }
        }
        return output
    }

    private fun stringSchema(): LinkedHashMap<String, Any> =
        mapOfNotEmpty("type" to "string")

    private fun binarySchema(): LinkedHashMap<String, Any> =
        mapOfNotEmpty(
            "type" to "string",
            "format" to "binary",
        )

    private sealed interface BuildOperationResult {
        data class Success(
            val path: String,
            val method: String,
            val operation: LinkedHashMap<String, Any>,
            val tags: List<String>,
        ) : BuildOperationResult

        data class Skipped(
            val reason: String,
            val details: String? = null,
        ) : BuildOperationResult
    }

    private data class TreeRequestLocation(
        val requestId: String,
        val folderPath: List<String>,
    )

    private data class ParsedUrl(
        val scheme: String?,
        val host: String?,
        val port: String?,
        val path: String,
        val queryPairs: List<ResolvedKeyValue>,
    )

    private data class ParameterIdentifier(
        val name: String,
        val location: String,
    )

    private data class ParameterAccumulator(
        val name: String,
        val location: String,
        val isRequired: Boolean,
        val examples: LinkedHashMap<String, LinkedHashMap<String, Any>> = linkedMapOf(),
    )

    private data class MediaTypeAccumulator(
        var schema: LinkedHashMap<String, Any>? = null,
        val examples: LinkedHashMap<String, LinkedHashMap<String, Any>> = linkedMapOf(),
    )

    private data class ResponseAccumulator(
        val description: String,
        val content: LinkedHashMap<String, MediaTypeAccumulator> = linkedMapOf(),
    )

    private data class ResolvedKeyValue(
        val key: String,
        val value: String,
        val valueType: FieldValueType = FieldValueType.String,
    )

    private data class ResolvedScriptData(
        val preFlightExecuteCode: String,
        val preFlightUpdateFromHeader: List<ResolvedKeyValue>,
        val preFlightUpdateFromQuery: List<ResolvedKeyValue>,
        val preFlightUpdateFromBody: List<ResolvedKeyValue>,
        val preFlightUpdateFromGraphqlVariables: List<ResolvedKeyValue>,
        val postFlightUpdateFromHeader: List<ResolvedKeyValue>,
        val postFlightUpdateFromBody: List<ResolvedKeyValue>,
    )

    private open class ResolvedBody(
        val mediaType: String,
        val value: Any?,
        val schema: LinkedHashMap<String, Any>,
    )

    private class GraphqlResolvedBody(
        val isSubscription: Boolean,
        val document: String,
        val variables: Any?,
        val operationName: String?,
        value: Any?,
        schema: LinkedHashMap<String, Any>,
    ) : ResolvedBody(
        mediaType = "application/json",
        value = value,
        schema = schema,
    )

    private data class ResolvedRequestExample(
        val id: String,
        val key: String,
        val name: String,
        val isBase: Boolean,
        val documentation: String,
        val hasOwnDocumentation: Boolean,
        val headers: List<ResolvedKeyValue>,
        val cookies: List<ResolvedKeyValue>,
        val queryParameters: List<ResolvedKeyValue>,
        val body: ResolvedBody?,
        val variables: Map<String, String>,
        val scripts: ResolvedScriptData,
        val isGraphqlSubscription: Boolean,
    )

    companion object {
        private val URL_REGEX = "^(?:([^:/?#]+)://)?([^/?#:]*)(?::([0-9]{1,5}))?/?([^?#]*)(?:\\?([^#]*))?.*$".toRegex()
        private val HELLO_HTTP_VARIABLE_REGEX = "\\\$\\{\\{([^{}]+)\\}\\}".toRegex()
        private val PATH_PARAMETER_REGEX = "\\{([^{}]+)\\}".toRegex()
        private val OPENAPI_HTTP_METHODS = setOf("get", "put", "post", "delete", "options", "head", "patch", "trace")
        private val SERVER_URL_VARIABLE_KEYS = listOf("prefix", "baseUrl", "base_url", "url")
        private const val GRAPHQL_ACCEPT_HEADER = "application/graphql-response+json; charset=utf-8, application/json; charset=utf-8"
    }
}
