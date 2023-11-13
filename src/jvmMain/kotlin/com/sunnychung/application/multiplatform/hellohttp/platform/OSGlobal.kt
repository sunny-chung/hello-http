package com.sunnychung.application.multiplatform.hellohttp.platform

fun isMacOs(): Boolean {
    return System.getProperty("os.name") == "Mac OS X"
}

fun isWindowsOs(): Boolean {
    return System.getProperty("os.name").startsWith("Win")
}

fun currentOS(): OS {
    return if (isMacOs()) MacOS
    else if (isWindowsOs()) WindowsOS
    else LinuxOS
}

sealed interface OS {
    val commandLineEscapeNewLine: String
}

object LinuxOS : OS {
    override val commandLineEscapeNewLine: String = "\\"
}

object MacOS : OS by LinuxOS

object WindowsOS : OS {
    override val commandLineEscapeNewLine: String = "^"
}
