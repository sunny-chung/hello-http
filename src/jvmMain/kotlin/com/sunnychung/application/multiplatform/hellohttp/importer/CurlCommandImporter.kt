package com.sunnychung.application.multiplatform.hellohttp.importer

import com.sunnychung.application.multiplatform.hellohttp.model.ContentType
import com.sunnychung.application.multiplatform.hellohttp.model.FieldValueType
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserKeyValuePair
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestBody
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.model.isValidHttpMethod
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class CurlCommandImporter {

    fun parseRequest(command: String): UserRequestTemplate {
        return parseRequests(command).first()
    }

    fun parseRequests(command: String): List<UserRequestTemplate> {
        val tokens = tokenizeCommand(command)
        if (tokens.isEmpty()) {
            throw IllegalArgumentException("The command is empty.")
        }

        val curlTokenIndices = tokens.mapIndexedNotNull { index, token ->
            if (isCurlToken(token)) index else null
        }
        if (curlTokenIndices.isEmpty()) {
            throw IllegalArgumentException("The command does not contain curl.")
        }

        return curlTokenIndices.mapIndexed { index, curlTokenIndex ->
            val nextCurlTokenIndex = curlTokenIndices.getOrNull(index + 1) ?: tokens.size
            parseRequestTokens(tokens = tokens.subList(curlTokenIndex, nextCurlTokenIndex))
        }
    }

    private fun parseRequestTokens(tokens: List<String>): UserRequestTemplate {
        if (tokens.isEmpty()) {
            throw IllegalArgumentException("The command is empty.")
        }

        val parsed = parseArguments(tokens.drop(1))

        val url = parsed.url ?: throw IllegalArgumentException("The curl command does not include a URL.")
        val method = parsed.explicitMethod ?: when {
            parsed.isGetMode -> "GET"
            parsed.formEntries.isNotEmpty() || parsed.dataEntries.isNotEmpty() -> "POST"
            else -> "GET"
        }

        val queryParameters = parseQueryParametersFromUrl(url) + if (parsed.isGetMode) {
            parsed.dataEntries.map { it.toKeyValuePair(isDecodeUrl = it.type == DataOptionType.UrlEncoded) }
        } else {
            emptyList()
        }

        val bodyAndType = if (parsed.isGetMode) {
            BodyAndType(
                body = null,
                contentType = ContentType.None,
            )
        } else {
            determineBodyAndContentType(parsed = parsed)
        }

        return UserRequestTemplate(
            id = uuidString(),
            name = generateRequestName(url = url),
            application = ProtocolApplication.Http,
            method = method,
            url = url,
            examples = listOf(
                UserRequestExample(
                    id = uuidString(),
                    name = "Base",
                    headers = parsed.headers,
                    cookies = parsed.cookies,
                    queryParameters = queryParameters,
                    contentType = bodyAndType.contentType,
                    body = bodyAndType.body,
                )
            ),
        )
    }

    private fun determineBodyAndContentType(parsed: ParsedCurlCommand): BodyAndType {
        if (parsed.formEntries.isNotEmpty()) {
            return BodyAndType(
                body = MultipartBody(parsed.formEntries),
                contentType = ContentType.Multipart,
            )
        }

        if (parsed.dataEntries.isEmpty()) {
            return BodyAndType(
                body = null,
                contentType = ContentType.None,
            )
        }

        if (parsed.dataEntries.all { it.type == DataOptionType.UrlEncoded }) {
            return BodyAndType(
                body = FormUrlEncodedBody(parsed.dataEntries.map { it.toKeyValuePair(isDecodeUrl = true) }),
                contentType = ContentType.FormUrlEncoded,
            )
        }

        if (parsed.dataEntries.size == 1) {
            val onlyDataValue = parsed.dataEntries.first().value
            if (onlyDataValue.startsWith("@")) {
                return BodyAndType(
                    body = FileBody(stripWrappingQuotes(onlyDataValue.removePrefix("@"))),
                    contentType = ContentType.BinaryFile,
                )
            }
        }

        val joinedData = parsed.dataEntries.joinToString("&") { it.value }
        val stringBody = StringBody(joinedData)

        if (hasHeader(parsed.headers, "content-type") {
                it.contains("application/x-www-form-urlencoded", ignoreCase = true)
            }) {
            val entries = joinedData.split("&")
                .filter { it.isNotBlank() }
                .map { parseKeyValueValue(it, isDecodeUrl = false) }
            return BodyAndType(
                body = FormUrlEncodedBody(entries),
                contentType = ContentType.FormUrlEncoded,
            )
        }

        val isJson = hasHeader(parsed.headers, "content-type") {
            it.contains("application/json", ignoreCase = true)
        } || looksLikeJson(joinedData)
        return BodyAndType(
            body = stringBody,
            contentType = if (isJson) ContentType.Json else ContentType.Raw,
        )
    }

    private fun hasHeader(headers: List<UserKeyValuePair>, key: String, predicate: (String) -> Boolean): Boolean {
        return headers.any { it.key.equals(key, ignoreCase = true) && predicate(it.value) }
    }

    private fun parseArguments(arguments: List<String>): ParsedCurlCommand {
        val headers = mutableListOf<UserKeyValuePair>()
        val cookies = mutableListOf<UserKeyValuePair>()
        val dataEntries = mutableListOf<DataOption>()
        val formEntries = mutableListOf<UserKeyValuePair>()

        var method: String? = null
        var url: String? = null
        var isGetMode = false

        var index = 0
        fun consumeOptionValue(option: String, inlineValue: String?): String {
            if (inlineValue != null) {
                return inlineValue
            }
            if (index + 1 >= arguments.size) {
                throw IllegalArgumentException("Missing value for option $option.")
            }
            index += 1
            return arguments[index]
        }

        while (index < arguments.size) {
            val token = arguments[index]
            when {
                token == "--" -> {
                    if (index + 1 < arguments.size && url == null) {
                        url = arguments[index + 1]
                    }
                    break
                }

                token.startsWith("--") -> {
                    val optionAndValue = token.split("=", limit = 2)
                    val option = optionAndValue[0]
                    val inlineValue = optionAndValue.getOrNull(1)
                    when (option) {
                        "--request" -> {
                            method = consumeOptionValue(option, inlineValue).normalizeHttpMethod()
                        }

                        "--url" -> {
                            url = consumeOptionValue(option, inlineValue)
                        }

                        "--header" -> {
                            parseHeader(consumeOptionValue(option, inlineValue))?.let { headers += it }
                        }

                        "--cookie" -> {
                            cookies += parseCookies(consumeOptionValue(option, inlineValue))
                        }

                        "--user-agent" -> {
                            headers += UserKeyValuePair(
                                key = "User-Agent",
                                value = consumeOptionValue(option, inlineValue),
                            )
                        }

                        "--data", "--data-ascii", "--data-raw" -> {
                            dataEntries += DataOption(
                                type = DataOptionType.Raw,
                                value = consumeOptionValue(option, inlineValue),
                            )
                        }

                        "--data-binary" -> {
                            dataEntries += DataOption(
                                type = DataOptionType.Binary,
                                value = consumeOptionValue(option, inlineValue),
                            )
                        }

                        "--data-urlencode" -> {
                            dataEntries += DataOption(
                                type = DataOptionType.UrlEncoded,
                                value = consumeOptionValue(option, inlineValue),
                            )
                        }

                        "--form" -> {
                            parseForm(
                                value = consumeOptionValue(option, inlineValue),
                                isForceString = false,
                            )?.let { formEntries += it }
                        }

                        "--form-string" -> {
                            parseForm(
                                value = consumeOptionValue(option, inlineValue),
                                isForceString = true,
                            )?.let { formEntries += it }
                        }

                        "--get" -> {
                            isGetMode = true
                        }

                        "--verbose", "--compressed", "--insecure", "--location", "--silent", "--show-error" -> {
                            // ignore
                        }
                    }
                }

                token.startsWith("-") && token.length > 1 -> {
                    when {
                        token == "-G" -> {
                            isGetMode = true
                        }

                        token == "-X" -> {
                            method = consumeOptionValue("-X", null).normalizeHttpMethod()
                        }

                        token.startsWith("-X") -> {
                            method = token.substring(2).normalizeHttpMethod()
                        }

                        token == "-H" -> {
                            parseHeader(consumeOptionValue("-H", null))?.let { headers += it }
                        }

                        token.startsWith("-H") -> {
                            parseHeader(token.substring(2))?.let { headers += it }
                        }

                        token == "-A" -> {
                            headers += UserKeyValuePair(
                                key = "User-Agent",
                                value = consumeOptionValue("-A", null),
                            )
                        }

                        token.startsWith("-A") -> {
                            headers += UserKeyValuePair(
                                key = "User-Agent",
                                value = token.substring(2),
                            )
                        }

                        token == "-b" -> {
                            cookies += parseCookies(consumeOptionValue("-b", null))
                        }

                        token.startsWith("-b") -> {
                            cookies += parseCookies(token.substring(2))
                        }

                        token == "-d" -> {
                            dataEntries += DataOption(
                                type = DataOptionType.Raw,
                                value = consumeOptionValue("-d", null),
                            )
                        }

                        token.startsWith("-d") -> {
                            dataEntries += DataOption(
                                type = DataOptionType.Raw,
                                value = token.substring(2),
                            )
                        }

                        token == "-F" -> {
                            parseForm(
                                value = consumeOptionValue("-F", null),
                                isForceString = false,
                            )?.let { formEntries += it }
                        }

                        token.startsWith("-F") -> {
                            parseForm(
                                value = token.substring(2),
                                isForceString = false,
                            )?.let { formEntries += it }
                        }
                    }
                }

                else -> {
                    if (url == null) {
                        url = token
                    }
                }
            }
            index += 1
        }

        return ParsedCurlCommand(
            explicitMethod = method,
            url = url,
            headers = headers,
            cookies = cookies,
            dataEntries = dataEntries,
            formEntries = formEntries,
            isGetMode = isGetMode,
        )
    }

    private fun parseForm(value: String, isForceString: Boolean): UserKeyValuePair? {
        val separatorIndex = value.indexOf('=')
        if (separatorIndex < 0) {
            return null
        }

        val key = value.substring(0, separatorIndex).trim()
        if (key.isBlank()) {
            return null
        }
        val rawValue = value.substring(separatorIndex + 1)

        val isFile = !isForceString && rawValue.startsWith("@")
        val parsedValue = if (isFile) {
            stripWrappingQuotes(rawValue.removePrefix("@"))
        } else {
            stripWrappingQuotes(rawValue)
        }

        return UserKeyValuePair(
            id = uuidString(),
            key = key,
            value = parsedValue,
            valueType = if (isFile) FieldValueType.File else FieldValueType.String,
            isEnabled = true,
        )
    }

    private fun parseHeader(header: String): UserKeyValuePair? {
        val separatorIndex = header.indexOf(':')
        if (separatorIndex < 0) {
            val key = header.trim()
            if (key.isBlank()) {
                return null
            }
            return UserKeyValuePair(
                id = uuidString(),
                key = key,
                value = "",
                valueType = FieldValueType.String,
                isEnabled = true,
            )
        }

        val key = header.substring(0, separatorIndex).trim()
        if (key.isBlank()) {
            return null
        }
        val value = header.substring(separatorIndex + 1).trimStart()
        return UserKeyValuePair(
            id = uuidString(),
            key = key,
            value = value,
            valueType = FieldValueType.String,
            isEnabled = true,
        )
    }

    private fun parseCookies(cookieHeaderValue: String): List<UserKeyValuePair> {
        return cookieHeaderValue.split(';')
            .mapNotNull { token ->
                val trimmed = token.trim()
                if (trimmed.isBlank()) {
                    return@mapNotNull null
                }
                val separatorIndex = trimmed.indexOf('=')
                if (separatorIndex < 0) {
                    return@mapNotNull null
                }
                UserKeyValuePair(
                    id = uuidString(),
                    key = trimmed.substring(0, separatorIndex).trim(),
                    value = trimmed.substring(separatorIndex + 1).trim(),
                    valueType = FieldValueType.String,
                    isEnabled = true,
                )
            }
    }

    private fun tokenizeCommand(command: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quoteMode = QuoteMode.None

        fun flush() {
            if (current.isNotEmpty()) {
                tokens += current.toString()
                current.clear()
            }
        }

        var index = 0
        while (index < command.length) {
            val c = command[index]
            when (quoteMode) {
                QuoteMode.None -> {
                    when {
                        c.isWhitespace() -> {
                            flush()
                            index += 1
                        }

                        c == '#' && current.isEmpty() -> {
                            while (index < command.length && command[index] != '\n' && command[index] != '\r') {
                                index += 1
                            }
                        }

                        c == '\'' -> {
                            quoteMode = QuoteMode.Single
                            index += 1
                        }

                        c == '"' -> {
                            quoteMode = QuoteMode.Double
                            index += 1
                        }

                        c == '\\' -> {
                            index += appendEscapedCharacter(command, index, current)
                        }

                        else -> {
                            current.append(c)
                            index += 1
                        }
                    }
                }

                QuoteMode.Single -> {
                    if (c == '\'') {
                        quoteMode = QuoteMode.None
                    } else {
                        current.append(c)
                    }
                    index += 1
                }

                QuoteMode.Double -> {
                    when (c) {
                        '"' -> {
                            quoteMode = QuoteMode.None
                            index += 1
                        }

                        '\\' -> {
                            index += appendEscapedCharacter(command, index, current)
                        }

                        else -> {
                            current.append(c)
                            index += 1
                        }
                    }
                }
            }
        }

        if (quoteMode != QuoteMode.None) {
            throw IllegalArgumentException("Unclosed quote in curl command.")
        }

        flush()
        return tokens
    }

    private fun appendEscapedCharacter(command: String, index: Int, output: StringBuilder): Int {
        if (index + 1 >= command.length) {
            return 1
        }
        val next = command[index + 1]
        if (next == '\n') {
            return 2
        }
        if (next == '\r' && index + 2 < command.length && command[index + 2] == '\n') {
            return 3
        }
        output.append(next)
        return 2
    }

    private fun isCurlToken(token: String): Boolean {
        val commandName = token.substringAfterLast('/')
        return commandName == "curl"
    }

    private fun String.normalizeHttpMethod(): String {
        val method = trim().uppercase()
        if (!method.isValidHttpMethod()) {
            throw IllegalArgumentException("Invalid HTTP method in curl command: $method")
        }
        return method
    }

    private fun DataOption.toKeyValuePair(isDecodeUrl: Boolean): UserKeyValuePair {
        return parseKeyValueValue(value = value, isDecodeUrl = isDecodeUrl)
    }

    private fun parseKeyValueValue(value: String, isDecodeUrl: Boolean): UserKeyValuePair {
        val separatorIndex = value.indexOf('=')
        val key = if (separatorIndex >= 0) value.substring(0, separatorIndex) else value
        val fieldValue = if (separatorIndex >= 0) value.substring(separatorIndex + 1) else ""
        return UserKeyValuePair(
            id = uuidString(),
            key = if (isDecodeUrl) key.urlDecode() else key,
            value = if (isDecodeUrl) fieldValue.urlDecode() else fieldValue,
            valueType = FieldValueType.String,
            isEnabled = true,
        )
    }

    private fun parseQueryParametersFromUrl(url: String): List<UserKeyValuePair> {
        val queryStartIndex = url.indexOf('?')
        if (queryStartIndex < 0 || queryStartIndex >= url.lastIndex) {
            return emptyList()
        }
        val fragmentStartIndex = url.indexOf('#', startIndex = queryStartIndex + 1)
        val query = if (fragmentStartIndex >= 0) {
            url.substring(queryStartIndex + 1, fragmentStartIndex)
        } else {
            url.substring(queryStartIndex + 1)
        }
        if (query.isBlank()) {
            return emptyList()
        }
        return query
            .split("&")
            .filter { it.isNotBlank() }
            .map { parseKeyValueValue(value = it, isDecodeUrl = true) }
    }

    private fun String.urlDecode(): String {
        return try {
            URLDecoder.decode(this, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            this
        }
    }

    private fun stripWrappingQuotes(value: String): String {
        if (value.length >= 2) {
            if (value.startsWith('"') && value.endsWith('"')) {
                return value.substring(1, value.lastIndex)
            }
            if (value.startsWith('\'') && value.endsWith('\'')) {
                return value.substring(1, value.lastIndex)
            }
        }
        return value
    }

    private fun looksLikeJson(value: String): Boolean {
        val text = value.trim()
        return (text.startsWith("{") && text.endsWith("}")) || (text.startsWith("[") && text.endsWith("]"))
    }

    private fun generateRequestName(url: String): String {
        val urlLabel = try {
            val uri = URI(url)
            val path = uri.path.orEmpty()
            when {
                path.isNotBlank() && path != "/" -> path.split('/').lastOrNull { it.isNotBlank() } ?: uri.host
                !uri.host.isNullOrBlank() -> uri.host
                else -> url
            }
        } catch (_: Exception) {
            url
        }
        return urlLabel.orEmpty().ifBlank { "Request" }
    }

    private data class ParsedCurlCommand(
        val explicitMethod: String?,
        val url: String?,
        val headers: List<UserKeyValuePair>,
        val cookies: List<UserKeyValuePair>,
        val dataEntries: List<DataOption>,
        val formEntries: List<UserKeyValuePair>,
        val isGetMode: Boolean,
    )

    private data class DataOption(
        val type: DataOptionType,
        val value: String,
    )

    private data class BodyAndType(
        val body: UserRequestBody?,
        val contentType: ContentType,
    )

    private enum class QuoteMode {
        None, Single, Double
    }

    private enum class DataOptionType {
        Raw, Binary, UrlEncoded
    }
}
