package com.sunnychung.application.multiplatform.hellohttp.helper

import com.sunnychung.application.multiplatform.hellohttp.constant.UserFunctions
import com.sunnychung.application.multiplatform.hellohttp.model.Environment
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate

/**
 * This class should not be cached. This class is invalid upon changes to the Environment object.
 */
class VariableResolver(
    environment: Environment?,
    private val resolveVariableMode: UserRequestTemplate.ResolveVariableMode = UserRequestTemplate.ExpandByEnvironment
) {
    private val environmentVariables = environment?.variables
        ?.filter { it.isEnabled }
        ?.associate { it.key to it.value }
        ?: emptyMap()

    fun resolve(subject: String): String {
        var s = subject
        when (resolveVariableMode) {
            is UserRequestTemplate.ExpandByEnvironment -> {
                environmentVariables.forEach {
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
