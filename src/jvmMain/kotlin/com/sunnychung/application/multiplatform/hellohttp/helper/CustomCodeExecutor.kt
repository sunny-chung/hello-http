package com.sunnychung.application.multiplatform.hellohttp.helper

import co.touchlab.kermit.Severity
import com.sunnychung.application.multiplatform.hellohttp.model.CustomCodeClasses
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.FileBody
import com.sunnychung.application.multiplatform.hellohttp.model.FormUrlEncodedBody
import com.sunnychung.application.multiplatform.hellohttp.model.GraphqlBody
import com.sunnychung.application.multiplatform.hellohttp.model.HttpRequest
import com.sunnychung.application.multiplatform.hellohttp.model.MultipartBody
import com.sunnychung.application.multiplatform.hellohttp.model.StringBody
import com.sunnychung.application.multiplatform.hellohttp.model.toDataType
import com.sunnychung.application.multiplatform.hellohttp.util.log
import com.sunnychung.lib.multiplatform.kotlite.KotliteInterpreter
import com.sunnychung.lib.multiplatform.kotlite.model.DelegatedValue
import com.sunnychung.lib.multiplatform.kotlite.model.ExecutionEnvironment
import com.sunnychung.lib.multiplatform.kotlite.model.GlobalProperty
import com.sunnychung.lib.multiplatform.kotlite.model.NullValue
import com.sunnychung.lib.multiplatform.kotlite.model.SourcePosition
import com.sunnychung.lib.multiplatform.kotlite.setKotliteLogMinLevel
import com.sunnychung.lib.multiplatform.kotlite.stdlib.AllStdLibModules

class CustomCodeExecutor(val code: String) {

    fun executePreFlight(request: HttpRequest, environment: Environment?) {
        if (code.isBlank()) return

        log.d { "Execute Pre Flight code -- Start" }

        val bodyTypeArgument = when (request.body) {
            null -> "Nothing"
            is FileBody -> "FileBody"
            is FormUrlEncodedBody -> "FormUrlEncodedBody"
            is GraphqlBody -> "GraphqlBody"
            is MultipartBody -> "MultipartBody"
            is StringBody -> "StringBody"
        }

        try {
            setKotliteLogMinLevel(Severity.Debug)
            KotliteInterpreter(
                filename = "User Script",
                code = code,
                executionEnvironment = ExecutionEnvironment().apply {
                    install(AllStdLibModules())
                    install(CustomCodeClasses.HelloHTTPUtilModule)
                    install(CustomCodeClasses.HelloHTTPPreFlightModule)

                    registerGlobalProperty(GlobalProperty(
                        position = SourcePosition("HelloHTTP", 1, 1),
                        declaredName = "request",
                        type = "MutableRequest<$bodyTypeArgument>",
                        isMutable = false,
                        getter = { interpreter ->
                            val symbolTable = interpreter.symbolTable()
                            DelegatedValue(
                                value = request,
                                clazz = CustomCodeClasses.RequestClass.MutableClazz,
                                typeArguments = listOf(
                                    bodyTypeArgument.toDataType(symbolTable)
                                ),
                                symbolTable = symbolTable,
                            )
                        }
                    ))

                    registerGlobalProperty(GlobalProperty(
                        position = SourcePosition("HelloHTTP", 1, 1),
                        declaredName = "environment",
                        type = "Environment?",
                        isMutable = false,
                        getter = { interpreter ->
                            val symbolTable = interpreter.symbolTable()
                            DelegatedValue(
                                value = environment ?: return@GlobalProperty NullValue,
                                clazz = CustomCodeClasses.EnvironmentClass.clazz,
                                symbolTable = symbolTable,
                            )
                        }
                    ))
                }
            ).eval()
        } catch (e: Throwable) {
            log.i(e) { "Execute user script fail" }
            throw e
        }

        log.d { "Execute Pre Flight code -- End" }
    }
}
