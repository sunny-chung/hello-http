package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.DocumentIdentifier
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import net.harawata.appdirs.AppDirsFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalSerializationApi::class)
class PersistenceManager {

    private val fileManager by lazy { AppContext.FileManager }

    internal val documentCaches = ConcurrentHashMap<DocumentIdentifier, Any>()
    internal val documentLocks = ConcurrentHashMap<DocumentIdentifier, Mutex>()

    private val codec = Cbor {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private fun dataDir() = File(AppDirsFactory.getInstance().getUserDataDir("Hello HTTP", null, null))

    private fun dataFile(relativePath: String): File {
        val path = dataDir().absolutePath + File.separator + relativePath.replace("/", File.separator)
        return File(path)
    }

    internal suspend inline fun <T> writeToFile(relativePath: String, serializer: KSerializer<T>, document: T) {
        val file = dataFile(relativePath)
        file.parentFile.mkdirs()
        val bytes = codec.encodeToByteArray(serializer, document)
        fileManager.writeToFile(
            file = file,
            content = bytes
        )
    }

    internal suspend inline fun <T> readFile(relativePath: String, serializer: KSerializer<T>): T? {
        val file = dataFile(relativePath)
        if (!file.isFile) return null
        val bytes = fileManager.readFromFile(file)
        return codec.decodeFromByteArray(serializer, bytes)
    }

    internal fun deleteFile(relativePath: String) {
        val file = dataFile(relativePath)
        if (file.isFile) {
            file.delete()
        }
    }




}
