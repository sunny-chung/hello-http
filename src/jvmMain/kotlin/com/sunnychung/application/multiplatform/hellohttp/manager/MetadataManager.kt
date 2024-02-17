package com.sunnychung.application.multiplatform.hellohttp.manager

import java.util.Properties

class MetadataManager {
    val properties = Properties()
    var hasReadProperties = false
    val propertiesLock = Any()

    private fun checkOrLoadProperties() {
        synchronized(propertiesLock) {
            if (!hasReadProperties) {
                val inputStream = javaClass.getResourceAsStream("/build.properties")
                properties.load(inputStream)
                hasReadProperties = true
            }
        }
    }

    val version: String
        get() {
            checkOrLoadProperties()
            return properties.getProperty("version")
        }

    val gitCommitHash: String
        get() {
            checkOrLoadProperties()
            return properties.getProperty("git.commit")
        }
}
