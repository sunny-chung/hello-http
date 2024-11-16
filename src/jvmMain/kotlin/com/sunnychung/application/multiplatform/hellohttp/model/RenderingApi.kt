package com.sunnychung.application.multiplatform.hellohttp.model

import com.sunnychung.application.multiplatform.hellohttp.platform.LinuxOS
import com.sunnychung.application.multiplatform.hellohttp.platform.MacOS
import com.sunnychung.application.multiplatform.hellohttp.platform.OS
import com.sunnychung.application.multiplatform.hellohttp.platform.WindowsOS

enum class RenderingApi(val value: String?) {
    Default(null), OpenGL("OPENGL"), Software("SOFTWARE")
}

fun getApplicableRenderingApiList(os: OS): List<RenderingApi> {
    return when (os) {
        MacOS -> RenderingApi.values().filter { it != RenderingApi.OpenGL }
        LinuxOS -> RenderingApi.values().filter { it != RenderingApi.OpenGL }
        WindowsOS -> RenderingApi.values().toList()
    }
}
