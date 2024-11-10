package com.sunnychung.application.multiplatform.hellohttp.helper

import com.sunnychung.application.multiplatform.hellohttp.constant.UserFunctions
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate

/**
 * This class should not be cached. This class is invalid upon changes to the Environment object.
 */
class VariableResolver(
    environment: Environment?,
    userRequestTemplate: UserRequestTemplate,
    requestExampleId: String,
    private val resolveVariableMode: UserRequestTemplate.ResolveVariableMode = UserRequestTemplate.ExpandByEnvironment
) {
    private val variables = userRequestTemplate.getAllVariables(requestExampleId, environment)

    fun resolve(subject: String): String {
        var s = subject
        when (resolveVariableMode) {
            is UserRequestTemplate.ExpandByEnvironment -> {
                variables.forEach {
                    s = s.replace("\${{${it.key}}}", it.value)
                }
                UserFunctions.forEach {
                    s = s.replace("\$((${it.key}))", it.value.function())
                }
            }
            is UserRequestTemplate.ReplaceAsString -> s = s.replace("\\\$\\{\\{([^{}]+)\\}\\}".toRegex(), resolveVariableMode.replacement)
        }
        return s
    }
}
