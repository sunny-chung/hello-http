package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.error.MultipleProcessError
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption

class SingleInstanceProcessService {

    lateinit var dataDir: File

    private var processLock: FileLock? = null

    fun enforce() {
        dataDir.mkdirs()
        val lockFile = File(dataDir, "pid")
        if (!lockFile.exists()) {
            lockFile.createNewFile()
        }
        val fc = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        processLock = fc.tryLock()
        if (processLock == null) {
            throw MultipleProcessError()
        }
    }
}
