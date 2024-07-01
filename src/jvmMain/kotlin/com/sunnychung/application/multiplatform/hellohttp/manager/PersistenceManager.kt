package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.DocumentIdentifier
import com.sunnychung.application.multiplatform.hellohttp.document.OperationalDI
import com.sunnychung.application.multiplatform.hellohttp.document.OperationalDocument
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectCollection
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDocument
import com.sunnychung.application.multiplatform.hellohttp.extension.CborStream
import com.sunnychung.application.multiplatform.hellohttp.extension.encodeToStream
import com.sunnychung.application.multiplatform.hellohttp.model.ColourTheme
import com.sunnychung.application.multiplatform.hellohttp.model.OperationalInfo
import com.sunnychung.application.multiplatform.hellohttp.model.UserPreference
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.cbor.Cbor
import java.io.File
import java.io.IOException
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
    private val codecCustomizedWriter = CborStream {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private fun dataDir() = AppContext.dataDir

    private fun dataFile(relativePath: String): File {
        val path = dataDir().absolutePath + File.separator + relativePath.replace("/", File.separator)
        return File(path)
    }

    internal suspend inline fun <T> writeToFile(relativePath: String, serializer: KSerializer<T>, document: T) {
        val file = dataFile(relativePath)
        file.parentFile.mkdirs()
//        val bytes = codec.encodeToByteArray(serializer, document)
//        fileManager.writeToFile(
//            file = file,
//            content = bytes
//        )
        val tmpFile = File(file.parentFile, "${file.name}.tmp")
        if (tmpFile.exists() && !tmpFile.canWrite()) {
            throw IOException("File ${tmpFile.absolutePath} is not writeable")
        }
        fileManager.writeToFile(
            file = tmpFile,
        ) { outStream ->
            codecCustomizedWriter.encodeToStream(serializer, document, outStream)
        }
        if (file.exists() && !file.delete()) {
            throw IOException("File ${file.absolutePath} cannot be deleted for new content")
        }
        if (!tmpFile.renameTo(file)) {
            throw IOException("File ${tmpFile.absolutePath} cannot be renamed to ${file.name}")
        }
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

    suspend fun initialize() {
        // clear cache
        documentCaches.clear()
        documentLocks.clear()

        // initialize cache
        AppContext.ProjectCollectionRepository.readOrCreate(ProjectAndEnvironmentsDI()) { id ->
            ProjectCollection(id = id, projects = mutableListOf())
        }
        AppContext.UserPreferenceRepository.readOrCreate(UserPreferenceDI()) { id ->
            UserPreferenceDocument(id = id, preference = UserPreference(
                colourTheme = ColourTheme.Dark
            ))
        }
        AppContext.OperationalRepository.readOrCreate(OperationalDI()) { id ->
            OperationalDocument(
                id = id,
                data = OperationalInfo(
                    appVersion = AppContext.MetadataManager.version,
                    installationId = uuidString(),
                )
            )
        }
    }


}
