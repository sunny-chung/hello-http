package com.sunnychung.application.multiplatform.hellohttp.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sunnychung.application.multiplatform.hellohttp.util.emptyToNull
import com.sunnychung.lib.multiplatform.kotlite.Parser
import com.sunnychung.lib.multiplatform.kotlite.extension.fullClassName
import com.sunnychung.lib.multiplatform.kotlite.lexer.Lexer
import com.sunnychung.lib.multiplatform.kotlite.model.AnyType
import com.sunnychung.lib.multiplatform.kotlite.model.CustomFunctionDefinition
import com.sunnychung.lib.multiplatform.kotlite.model.CustomFunctionParameter
import com.sunnychung.lib.multiplatform.kotlite.model.DataType
import com.sunnychung.lib.multiplatform.kotlite.model.DelegatedValue
import com.sunnychung.lib.multiplatform.kotlite.model.DoubleValue
import com.sunnychung.lib.multiplatform.kotlite.model.ExtensionProperty
import com.sunnychung.lib.multiplatform.kotlite.model.GlobalProperty
import com.sunnychung.lib.multiplatform.kotlite.model.IntValue
import com.sunnychung.lib.multiplatform.kotlite.model.LibraryModule
import com.sunnychung.lib.multiplatform.kotlite.model.ListValue
import com.sunnychung.lib.multiplatform.kotlite.model.NullValue
import com.sunnychung.lib.multiplatform.kotlite.model.ObjectType
import com.sunnychung.lib.multiplatform.kotlite.model.PairValue
import com.sunnychung.lib.multiplatform.kotlite.model.ProvidedClassDefinition
import com.sunnychung.lib.multiplatform.kotlite.model.RuntimeValue
import com.sunnychung.lib.multiplatform.kotlite.model.SourcePosition
import com.sunnychung.lib.multiplatform.kotlite.model.StringValue
import com.sunnychung.lib.multiplatform.kotlite.model.SymbolTable
import com.sunnychung.lib.multiplatform.kotlite.model.TypeParameter
import com.sunnychung.lib.multiplatform.kotlite.model.UnitValue
import com.sunnychung.lib.multiplatform.kotlite.stdlib.byte.ByteArrayValue
import com.sunnychung.lib.multiplatform.kotlite.stdlib.collections.MapValue
import java.io.File
import java.security.MessageDigest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object CustomCodeClasses {

    object RequestClass {
        val NonMutableClazz = ProvidedClassDefinition(
            position = SourcePosition("HelloHTTP", 1, 1),
            fullQualifiedName = "Request",
            typeParameters = listOf(TypeParameter("BODY", typeUpperBound = null)),
            isInstanceCreationAllowed = false,
            primaryConstructorParameters = emptyList(),
            constructInstance = { _, _, _ -> throw UnsupportedOperationException() },
        )

        val MutableClazz = ProvidedClassDefinition(
            position = SourcePosition("HelloHTTP", 2, 1),
            fullQualifiedName = "MutableRequest",
            typeParameters = listOf(TypeParameter("BODY", typeUpperBound = null)),
            isInstanceCreationAllowed = false,
            primaryConstructorParameters = emptyList(),
            constructInstance = { _, _, _ -> throw UnsupportedOperationException() },
            superClassInvocationString = "Request<BODY>()",
        )

        val properties = listOf(
            ExtensionProperty(
                receiver = "Request<*>",
                declaredName = "url",
                type = "String",
                getter = { interpreter, receiver, typeArgs ->
                    StringValue(
                        value = (receiver as DelegatedValue<HttpRequest>).value.url,
                        symbolTable = interpreter.symbolTable(),
                    )
                }
            ),
            ExtensionProperty(
                receiver = "Request<*>",
                declaredName = "method",
                type = "String",
                getter = { interpreter, receiver, typeArgs ->
                    StringValue(
                        value = (receiver as DelegatedValue<HttpRequest>).value.method,
                        symbolTable = interpreter.symbolTable(),
                    )
                }
            ),
            ExtensionProperty(
                receiver = "Request<*>",
                declaredName = "headers",
                type = "List<Pair<String, String>>",
                getter = { interpreter, receiver, typeArgs ->
                    val symbolTable = interpreter.symbolTable()
                    (receiver as DelegatedValue<HttpRequest>).value.headers.toRuntimeValue1(
                        symbolTable,
                        receiver.type()
                    )
                }
            ),
            ExtensionProperty(
                receiver = "Request<*>",
                declaredName = "queryParameters",
                type = "List<Pair<String, String>>",
                getter = { interpreter, receiver, typeArgs ->
                    val symbolTable = interpreter.symbolTable()
                    (receiver as DelegatedValue<HttpRequest>).value.queryParameters.toRuntimeValue1(
                        symbolTable,
                        receiver.type()
                    )
                }
            ),
            ExtensionProperty(
                receiver = "Request<BODY>",
                declaredName = "body",
                typeParameters = listOf(TypeParameter("BODY", null)),
                type = "BODY?",
                getter = { interpreter, receiver, typeArgs ->
                    val symbolTable = interpreter.symbolTable()
                    when (val body = (receiver as DelegatedValue<HttpRequest>).value.body
                        ?: return@ExtensionProperty NullValue
                    ) {
                        is FileBody -> DelegatedValue(body, FileBodyClass.clazz, symbolTable = symbolTable)
                        is FormUrlEncodedBody -> DelegatedValue(
                            body,
                            FormUrlEncodedBodyClass.clazz,
                            symbolTable = symbolTable
                        )

                        is GraphqlBody -> DelegatedValue(body, GraphqlBodyClass.clazz, symbolTable = symbolTable)
                        is MultipartBody -> DelegatedValue(
                            body,
                            MultipartBodyClass.clazz,
                            symbolTable = symbolTable
                        )

                        is StringBody -> DelegatedValue(body, StringBodyClass.clazz, symbolTable = symbolTable)
                    }
                }
            ),
        )

        val functions = listOf(
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "Request<*>",
                functionName = "getResolvedUri",
                returnType = "String",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    StringValue(
                        (receiver as DelegatedValue<HttpRequest>).value.getResolvedUri().toString(),
                        interpreter.symbolTable(),
                    )
                }
            )
        )

        val mutableFunctions = listOf(
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "MutableRequest<*>",
                functionName = "addHeader",
                returnType = "Unit",
                parameterTypes = listOf(
                    CustomFunctionParameter(name = "key", type = "String"),
                    CustomFunctionParameter(name = "value", type = "String"),
                ),
                executable = { interpreter, receiver, args, typeArgs ->
                    (receiver as DelegatedValue<HttpRequest>).value.addHeader(
                        key = (args[0] as StringValue).value,
                        value = (args[1] as StringValue).value,
                    )
                    UnitValue
                },
            ),
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "MutableRequest<*>",
                functionName = "addQueryParameter",
                returnType = "Unit",
                parameterTypes = listOf(
                    CustomFunctionParameter(name = "key", type = "String"),
                    CustomFunctionParameter(name = "value", type = "String"),
                ),
                executable = { interpreter, receiver, args, typeArgs ->
                    (receiver as DelegatedValue<HttpRequest>).value.addQueryParameter(
                        key = (args[0] as StringValue).value,
                        value = (args[1] as StringValue).value,
                    )
                    UnitValue
                },
            ),
        )

    }

    object UserKeyValuePairClass {
        val clazz = ProvidedClassDefinition(
            position = SourcePosition("HelloHTTP", 3, 1),
            fullQualifiedName = "UserKeyValuePair",
            typeParameters = emptyList(),
            isInstanceCreationAllowed = false,
            primaryConstructorParameters = emptyList(),
            constructInstance = { _, _, _ -> throw UnsupportedOperationException() },
        )

        val properties = listOf(
            ExtensionProperty(
                receiver = "UserKeyValuePair",
                declaredName = "key",
                type = "String",
                getter = { interpreter, receiver, typeArgs ->
                    StringValue((receiver as DelegatedValue<UserKeyValuePair>).value.key, interpreter.symbolTable())
                }
            ),
            ExtensionProperty(
                receiver = "UserKeyValuePair",
                declaredName = "value",
                type = "String",
                getter = { interpreter, receiver, typeArgs ->
                    StringValue(
                        (receiver as DelegatedValue<UserKeyValuePair>).value.value,
                        interpreter.symbolTable()
                    )
                }
            ),
            ExtensionProperty(
                receiver = "UserKeyValuePair",
                declaredName = "valueType",
                type = "String",
                getter = { interpreter, receiver, typeArgs ->
                    StringValue(
                        (receiver as DelegatedValue<UserKeyValuePair>).value.valueType.toString(),
                        interpreter.symbolTable()
                    )
                }
            ),
        )

        val functions = listOf(
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "UserKeyValuePair",
                functionName = "readValueBytes",
                returnType = "ByteArray?",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    val receiver = (receiver as DelegatedValue<UserKeyValuePair>).value
                    val bytes = when (receiver.valueType) {
                        FieldValueType.String -> receiver.value.toByteArray(Charsets.UTF_8)
                        FieldValueType.File -> {
                            val path = receiver.value.trim().emptyToNull() ?: return@CustomFunctionDefinition NullValue
                            File(path).readBytes()
                        }
                    }
                    ByteArrayValue(bytes, interpreter.symbolTable())
                },
            )
        )
    }

    object StringBodyClass {
        val clazz = ProvidedClassDefinition(
            position = SourcePosition("HelloHTTP", 4, 1),
            fullQualifiedName = "StringBody",
            typeParameters = emptyList(),
            isInstanceCreationAllowed = false,
            primaryConstructorParameters = emptyList(),
            constructInstance = { _, _, _ -> throw UnsupportedOperationException() },
        )

        val properties = listOf(
            ExtensionProperty(
                receiver = "StringBody",
                declaredName = "value",
                type = "String",
                getter = { interpreter, receiver, typeArgs ->
                    StringValue((receiver as DelegatedValue<StringBody>).value.value, interpreter.symbolTable())
                }
            ),
        )

    }

    object FormUrlEncodedBodyClass {
        val clazz = ProvidedClassDefinition(
            position = SourcePosition("HelloHTTP", 4, 1),
            fullQualifiedName = "FormUrlEncodedBody",
            typeParameters = emptyList(),
            isInstanceCreationAllowed = false,
            primaryConstructorParameters = emptyList(),
            constructInstance = { _, _, _ -> throw UnsupportedOperationException() },
        )

        val properties = listOf(
            ExtensionProperty(
                receiver = "FormUrlEncodedBody",
                declaredName = "value",
                type = "List<FormUrlEncodedBody>",
                getter = { interpreter, receiver, typeArgs ->
                    (receiver as DelegatedValue<FormUrlEncodedBody>).value.value.toRuntimeValue2(
                        interpreter.symbolTable(),
                        receiver.type()
                    )
                }
            ),
        )
    }

    object MultipartBodyClass {
        val clazz = ProvidedClassDefinition(
            position = SourcePosition("HelloHTTP", 4, 1),
            fullQualifiedName = "MultipartBody",
            typeParameters = emptyList(),
            isInstanceCreationAllowed = false,
            primaryConstructorParameters = emptyList(),
            constructInstance = { _, _, _ -> throw UnsupportedOperationException() },
        )

        val properties = listOf(
            ExtensionProperty(
                receiver = "MultipartBody",
                declaredName = "value",
                type = "List<MultipartBody>",
                getter = { interpreter, receiver, typeArgs ->
                    (receiver as DelegatedValue<MultipartBody>).value.value.toRuntimeValue2(
                        interpreter.symbolTable(),
                        receiver.type()
                    )
                }
            ),
        )
    }

    object FileBodyClass {
        val clazz = ProvidedClassDefinition(
            position = SourcePosition("HelloHTTP", 4, 1),
            fullQualifiedName = "FileBody",
            typeParameters = emptyList(),
            isInstanceCreationAllowed = false,
            primaryConstructorParameters = emptyList(),
            constructInstance = { _, _, _ -> throw UnsupportedOperationException() },
        )

        val properties = listOf(
            ExtensionProperty(
                receiver = "FileBody",
                declaredName = "filePath",
                type = "String?",
                getter = { interpreter, receiver, typeArgs ->
                    StringValue(
                        (receiver as DelegatedValue<FileBody>).value.filePath ?: return@ExtensionProperty NullValue,
                        interpreter.symbolTable()
                    )
                }
            ),
        )

        val functions = listOf(
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "FileBody",
                functionName = "readBytes",
                returnType = "ByteArray?",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    val path = (receiver as DelegatedValue<FileBody>).value.filePath
                        ?: return@CustomFunctionDefinition NullValue
                    ByteArrayValue(File(path).readBytes(), interpreter.symbolTable())
                },
            )
        )

    }

    object GraphqlBodyClass {
        val clazz = ProvidedClassDefinition(
            position = SourcePosition("HelloHTTP", 4, 1),
            fullQualifiedName = "GraphqlBody",
            typeParameters = emptyList(),
            isInstanceCreationAllowed = false,
            primaryConstructorParameters = emptyList(),
            constructInstance = { _, _, _ -> throw UnsupportedOperationException() },
        )

        val properties = listOf(
            ExtensionProperty(
                receiver = "GraphqlBody",
                declaredName = "document",
                type = "String",
                getter = { interpreter, receiver, typeArgs ->
                    StringValue(
                        (receiver as DelegatedValue<GraphqlBody>).value.document,
                        interpreter.symbolTable()
                    )
                }
            ),
            ExtensionProperty(
                receiver = "GraphqlBody",
                declaredName = "variables",
                type = "String",
                getter = { interpreter, receiver, typeArgs ->
                    StringValue(
                        (receiver as DelegatedValue<GraphqlBody>).value.variables,
                        interpreter.symbolTable()
                    )
                }
            ),
            ExtensionProperty(
                receiver = "GraphqlBody",
                declaredName = "operationName",
                type = "String?",
                getter = { interpreter, receiver, typeArgs ->
                    StringValue(
                        (receiver as DelegatedValue<GraphqlBody>).value.operationName
                            ?: return@ExtensionProperty NullValue,
                        interpreter.symbolTable()
                    )
                }
            ),
        )
    }

    object Encoding {
        @OptIn(ExperimentalEncodingApi::class)
        val functions = listOf(
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "ByteArray",
                functionName = "encodeToBase64String",
                returnType = "String",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    val bytes = (receiver as DelegatedValue<ByteArray>).value
                    val encoded = Base64.encode(bytes)
                    StringValue(encoded, interpreter.symbolTable())
                },
            ),
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "ByteArray",
                functionName = "encodeToBase64UrlString",
                returnType = "String",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    val bytes = (receiver as DelegatedValue<ByteArray>).value
                    val encoded = Base64.UrlSafe.encode(bytes)
                    StringValue(encoded, interpreter.symbolTable())
                },
            ),
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "ByteArray",
                functionName = "encodeToHexString",
                returnType = "String",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    val bytes = (receiver as DelegatedValue<ByteArray>).value
                    val encoded = bytes.toHexString()
                    StringValue(encoded, interpreter.symbolTable())
                },
            ),

            // JSON
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "String",
                functionName = "decodeJsonStringToMap",
                returnType = "Map<Any?, Any?>",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    val symbolTable = interpreter.symbolTable()
                    val string = (receiver as StringValue).value
                    val decoded = jacksonObjectMapper().readValue(string, HashMap::class.java).toRuntimeValue(symbolTable)
                    MapValue(decoded, AnyType(isNullable = true), AnyType(isNullable = true), symbolTable)
                },
            ),
        )
    }

    object Hashing {
        val functions = listOf(
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "ByteArray",
                functionName = "toSha1Hash",
                returnType = "ByteArray",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    val bytes = (receiver as DelegatedValue<ByteArray>).value
                    val hashed = MessageDigest.getInstance("SHA-1")
                        .let {
                            it.update(bytes)
                            it.digest()
                        }
                    ByteArrayValue(hashed, interpreter.symbolTable())
                },
            ),
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "ByteArray",
                functionName = "toSha256Hash",
                returnType = "ByteArray",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    val bytes = (receiver as DelegatedValue<ByteArray>).value
                    val hashed = MessageDigest.getInstance("SHA-256")
                        .let {
                            it.update(bytes)
                            it.digest()
                        }
                    ByteArrayValue(hashed, interpreter.symbolTable())
                },
            ),
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "ByteArray",
                functionName = "toSha512Hash",
                returnType = "ByteArray",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    val bytes = (receiver as DelegatedValue<ByteArray>).value
                    val hashed = MessageDigest.getInstance("SHA-512")
                        .let {
                            it.update(bytes)
                            it.digest()
                        }
                    ByteArrayValue(hashed, interpreter.symbolTable())
                },
            ),
            CustomFunctionDefinition(
                position = SourcePosition("HelloHTTP", 1, 1),
                receiverType = "ByteArray",
                functionName = "toMd5Hash",
                returnType = "ByteArray",
                parameterTypes = emptyList(),
                executable = { interpreter, receiver, args, typeArgs ->
                    val bytes = (receiver as DelegatedValue<ByteArray>).value
                    val hashed = MessageDigest.getInstance("MD5")
                        .let {
                            it.update(bytes)
                            it.digest()
                        }
                    ByteArrayValue(hashed, interpreter.symbolTable())
                },
            ),
        )
    }

    val HelloHTTPUtilModule = object : LibraryModule("HelloHTTP-Util") {
        override val classes: List<ProvidedClassDefinition> = emptyList()
        override val functions: List<CustomFunctionDefinition> = Encoding.functions + Hashing.functions
        override val globalProperties: List<GlobalProperty> = emptyList()
        override val properties: List<ExtensionProperty> = emptyList()
    }

    val HelloHTTPPreFlightModule = object : LibraryModule("HelloHTTP-PreFlight") {
        override val classes: List<ProvidedClassDefinition> = listOf(
            RequestClass.NonMutableClazz,
            RequestClass.MutableClazz,
            UserKeyValuePairClass.clazz,
            StringBodyClass.clazz,
            FormUrlEncodedBodyClass.clazz,
            MultipartBodyClass.clazz,
            FileBodyClass.clazz,
            GraphqlBodyClass.clazz,
        )
        override val functions: List<CustomFunctionDefinition> = UserKeyValuePairClass.functions +
                FileBodyClass.functions +
                RequestClass.functions +
                RequestClass.mutableFunctions
        override val globalProperties: List<GlobalProperty> = emptyList()
        override val properties: List<ExtensionProperty> = RequestClass.properties +
                UserKeyValuePairClass.properties +
                StringBodyClass.properties +
                FormUrlEncodedBodyClass.properties +
                MultipartBodyClass.properties +
                FileBodyClass.properties +
                GraphqlBodyClass.properties
    }
}

private fun List<Pair<String, String>>.toRuntimeValue1(symbolTable: SymbolTable, listType: DataType): RuntimeValue {
    return ListValue(
        value = this.map {
            PairValue(
                value = StringValue(it.first, symbolTable) to StringValue(it.second, symbolTable),
                typeA = symbolTable.StringType,
                typeB = symbolTable.StringType,
                symbolTable = symbolTable,
            )
        },
        symbolTable = symbolTable,
        typeArgument = (listType as ObjectType).arguments.first(),
    )
}

private fun List<UserKeyValuePair>.toRuntimeValue2(symbolTable: SymbolTable, listType: DataType): RuntimeValue {
    return ListValue(
        value = this.map {
            DelegatedValue(
                value = it,
                clazz = CustomCodeClasses.UserKeyValuePairClass.clazz,
                symbolTable = symbolTable,
            )
        },
        symbolTable = symbolTable,
        typeArgument = (listType as ObjectType).arguments.first(),
    )
}

private fun ByteArray.toHexString() = buildString {
    forEach { b ->
        append(String.format("%02x", b))
    }
}

private fun Any?.toRuntimeValue(symbolTable: SymbolTable): RuntimeValue {
    return when (this) {
        null -> NullValue
        is Int -> IntValue(this, symbolTable)
        is Float -> DoubleValue(this.toDouble(), symbolTable)
        is Double -> DoubleValue(this, symbolTable)
        is String -> StringValue(this, symbolTable)
        is List<*> -> ListValue(map { it.toRuntimeValue(symbolTable) }, AnyType(isNullable = true), symbolTable = symbolTable)
        is Map<*, *> -> MapValue(toRuntimeValue(symbolTable), AnyType(isNullable = true), AnyType(isNullable = true), symbolTable)
        else -> throw UnsupportedOperationException("Unsupported type $fullClassName while converting to RuntimeValue")
    }
}

private fun Map<*, *>.toRuntimeValue(symbolTable: SymbolTable): Map<RuntimeValue, RuntimeValue> {
    return buildMap {
        forEach {
            put(it.key.toRuntimeValue(symbolTable), it.value.toRuntimeValue(symbolTable))
        }
    }
}

fun String.toDataType(symbolTable: SymbolTable) =
    Parser(Lexer("HelloHTTP", this)).type()
        .let { symbolTable.assertToDataType(it) }
