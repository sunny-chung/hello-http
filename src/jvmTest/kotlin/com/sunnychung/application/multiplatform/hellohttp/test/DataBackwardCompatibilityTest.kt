package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.util.UUID

class DataBackwardCompatibilityTest {

    internal fun currentAppVersionExcludingLabel(): String =
        AppContext.MetadataManager.version.substringBefore("-")

    val baseArchiveDataDir = File("test-data-archive", currentAppVersionExcludingLabel())

    /**
     * Read data files from $projectDir/test-data-archive/$version/app-data/,
     * and generate a backup file to $projectDir/test-data-archive/$version/app-data-backup.dump.
     *
     * Other test cases in this test depend on the execution product of this test case.
     */
    @Test
    fun `data files for current app version can be read successfully, and can be converted to backup dump file`() {
        val inputDataDir = File(baseArchiveDataDir, "app-data")
        if (!inputDataDir.exists() || !inputDataDir.isDirectory) {
            return
        }
        val dataFiles = inputDataDir.list()
        if (dataFiles.isNullOrEmpty()) {
            return
        }

        // copy all data files to a temporary build directory
        val tempBaseDataDir = File("build", "temp-test-run-${UUID.randomUUID()}")
        if (tempBaseDataDir.exists()) {
            tempBaseDataDir.deleteRecursively()
        }
        val previousAppContext = AppContext.instance
        try {
            tempBaseDataDir.mkdirs()
            dataFiles.forEach {
                File(inputDataDir, it).copyRecursively(File(tempBaseDataDir, it))
            }

            AppContext.instance = AppContext() // use a new context
            AppContext.dataDir = tempBaseDataDir
            val backupDestination = File(baseArchiveDataDir, "app-data-backup.dump")
            runBlocking {
                AppContext.AutoBackupManager.backupNow(backupDestination)
            }
            if (!backupDestination.isFile) {
                throw RuntimeException("Backup file cannot be created")
            }
        } finally {
            tempBaseDataDir.deleteRecursively()
            AppContext.instance = previousAppContext
        }
    }
}
