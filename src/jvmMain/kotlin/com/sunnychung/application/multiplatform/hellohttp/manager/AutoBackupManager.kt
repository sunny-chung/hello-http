package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.ProjectAndEnvironmentsDI
import com.sunnychung.application.multiplatform.hellohttp.document.UserPreferenceDI
import com.sunnychung.application.multiplatform.hellohttp.exporter.DataDumpExporter
import com.sunnychung.application.multiplatform.hellohttp.model.DEFAULT_BACKUP_RETENTION_DAYS
import com.sunnychung.lib.multiplatform.kdatetime.KDuration
import com.sunnychung.lib.multiplatform.kdatetime.KFixedTimeUnit
import com.sunnychung.lib.multiplatform.kdatetime.KInstant
import com.sunnychung.lib.multiplatform.kdatetime.KZonedInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import net.harawata.appdirs.AppDirsFactory
import java.io.File

/**
 * Backup when:
 * - the application starts
 * - every 6 hours while the application is running
 */
class AutoBackupManager {

    val backupInterval = KDuration.of(6, KFixedTimeUnit.Hour)
    private val timerFlow = flow {
        while (currentCoroutineContext().isActive) {
            delay(backupInterval.toMilliseconds())
            emit(Unit)
            yield()
        }
    }

    init {
        timerFlow.onEach {
            backupNow()
        }
            .launchIn(CoroutineScope(Dispatchers.IO))
    }

    fun backupDir(): File {
        val appDir = AppDirsFactory.getInstance().getUserDataDir("Hello HTTP", null, null)
        return File(appDir, "backups")
    }

    suspend fun backupNow() {
        val allProjects = AppContext.ProjectCollectionRepository.read(ProjectAndEnvironmentsDI())!!.projects
        if (allProjects.isEmpty()) {
            return
        }

        val dateTimeString = KZonedInstant.nowAtLocalZoneOffset().format("yyyy-MM-dd--HH-mm-ss")
        val dir = backupDir()
        dir.mkdirs()
        val file = File(dir, "Hello-HTTP_backup_$dateTimeString.dump")

        DataDumpExporter().exportToFile(allProjects, file)

        removeOldBackups()
    }

    fun removeOldBackups() {
        val dir = backupDir()

        val userPreferenceRepository = AppContext.UserPreferenceRepository
        val userPreference = runBlocking { // TODO don't use runBlocking
            userPreferenceRepository.read(UserPreferenceDI())!!.preference
        }
        val retentionDays = userPreference.backupRetentionDays ?: DEFAULT_BACKUP_RETENTION_DAYS
        val deleteBeforeInstant = KInstant.now() - KDuration.of(retentionDays, KFixedTimeUnit.Day)

        dir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".dump") && file.isFile && file.lastModified() < deleteBeforeInstant.toEpochMilliseconds()) {
                file.delete()
            }
        }
    }
}
