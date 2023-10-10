package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.error.MalformedInputError
import com.sunnychung.application.multiplatform.hellohttp.error.UnsupportedFileSchemaVersionError
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class FileManager {

    protected val fileSchemaVersion: Int = 1
    protected val separatorByte: Byte = ';'.code.toByte()
    protected val magicBytes: ByteArray = "hello-http".toByteArray()

    private val fileLocks = ConcurrentHashMap<String, Mutex>()


    protected suspend fun <R> withLock(file: File, operation: File.() -> R): R {
        val lock = fileLocks.getOrPut(file.absolutePath) { Mutex() }
        return lock.withLock { file.operation() }
    }

    suspend fun writeToFile(file: File, content: ByteArray) {
        withLock(file) {
            file.sink().buffer().use {
                it.write(magicBytes)
                it.write(byteArrayOf(separatorByte))
                it.write(fileSchemaVersion.toString().toByteArray())
                it.write(byteArrayOf(separatorByte))
                it.write(content)
            }
        }
    }

    suspend fun readFromFile(file: File): ByteArray {
        val allBytes: ByteArray
        allBytes = withLock(file) {
            file.source().buffer().use {
                val b = Buffer()
                it.read(b, magicBytes.size.toLong())
                if (!b.readByteArray().contentEquals(magicBytes)) {
                    throw MalformedInputError()
                }
                it.read(b, 1)
                if (b.readByte() != separatorByte) {
                    throw MalformedInputError()
                }
                it.readByteArray()
            }
        }
        var cursor = 0
        while (allBytes[cursor] != separatorByte && cursor <= 8) { // max read 8 bytes
            ++cursor
        }
        if (allBytes[cursor] != separatorByte) {
            throw MalformedInputError()
        }
        val version = try {
            allBytes.copyOfRange(0, cursor).decodeToString().toInt()
        } catch (e: Error) {
            throw MalformedInputError()
        }
        if (version > fileSchemaVersion) {
            throw UnsupportedFileSchemaVersionError()
        }
        return when (version) {
            1 -> allBytes.copyOfRange(++cursor, allBytes.size)
            else -> throw UnsupportedFileSchemaVersionError()
        }
    }
}
