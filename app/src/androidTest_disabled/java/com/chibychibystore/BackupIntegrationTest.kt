package com.chibychibystore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.backup.BackupData
import com.chibychibystore.service.BackupService
import com.chibychibystore.service.RestoreService
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.inject.Inject

/**
 * Integration test for backup and restore functionality
 */
@HiltAndroidTest
class BackupIntegrationTest : BaseIntegrationTest() {

    @Inject
    lateinit var backupService: BackupService

    @Inject
    lateinit var restoreService: RestoreService

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testFullBackupAndRestoreCycle() = runBlocking {
        // Setup test data
        seedTestData()

        // Create backup
        val backupResult = backupService.createBackup()
        assertTrue("Backup should succeed", backupResult.isSuccess)

        val backupInfo = backupResult.getOrThrow()
        assertNotNull("Backup info should not be null", backupInfo)
        assertTrue("Backup file should exist", File(backupInfo.filePath).exists())

        // Validate backup
        val validationResult = backupService.validateBackup(backupInfo.filePath)
        assertTrue("Backup validation should succeed", validationResult.isSuccess)

        val validation = validationResult.getOrThrow()
        assertTrue("Backup should be valid", validation.isValid)
        assertEquals("Should have 1 user", 1, validation.recordCounts["pengguna"])
        assertEquals("Should have 1 category", 1, validation.recordCounts["kategori"])
        assertEquals("Should have 1 warehouse", 1, validation.recordCounts["gudang"])
        assertEquals("Should have 1 product", 1, validation.recordCounts["produk"])
        assertEquals("Should have 1 supplier", 1, validation.recordCounts["pemasok"])

        // Clear existing data
        clearTestData()

        // Verify data is cleared
        val usersAfterClear = penggunaRepository.getAllPengguna().firstOrNull()
        assertTrue("Users should be empty after clear", usersAfterClear?.isEmpty() ?: true)

        // Restore from backup
        val restoreResult = restoreService.restoreFromBackup(backupInfo.filePath)
        assertTrue("Restore should succeed", restoreResult.isSuccess)

        val restoreInfo = restoreResult.getOrThrow()
        assertTrue("Restore should be successful", restoreInfo.success)
        assertEquals("Should restore 1 user", 1, restoreInfo.recordsRestored["pengguna"])
        assertEquals("Should restore 1 category", 1, restoreInfo.recordsRestored["kategori"])
        assertEquals("Should restore 1 warehouse", 1, restoreInfo.recordsRestored["gudang"])
        assertEquals("Should restore 1 product", 1, restoreInfo.recordsRestored["produk"])
        assertEquals("Should restore 1 supplier", 1, restoreInfo.recordsRestored["pemasok"])

        // Verify data is restored
        val usersAfterRestore = penggunaRepository.getAllPengguna().firstOrNull()
        assertNotNull("Users should exist after restore", usersAfterRestore)
        assertEquals("Should have 1 user after restore", 1, usersAfterRestore?.size)

        // Cleanup
        File(backupInfo.filePath).delete()
    }

    @Test
    fun testBackupHistory() = runBlocking {
        // Setup test data
        seedTestData()

        // Create multiple backups
        val backup1 = backupService.createBackup().getOrThrow()
        val backup2 = backupService.createBackup().getOrThrow()

        // Get backup history
        val historyResult = backupService.getBackupHistory()
        assertTrue("Getting backup history should succeed", historyResult.isSuccess)

        val history = historyResult.getOrThrow()
        assertTrue("Should have at least 2 backups", history.size >= 2)

        // Verify backups are ordered by date (newest first)
        assertTrue("First backup should be newer than second",
            history[0].createdAt >= history[1].createdAt)

        // Cleanup
        File(backup1.filePath).delete()
        File(backup2.filePath).delete()
    }

    @Test
    fun testBackupPreview() = runBlocking {
        // Setup test data
        seedTestData()

        // Create backup
        val backupInfo = backupService.createBackup().getOrThrow()

        // Get backup preview
        val previewResult = restoreService.previewBackup(backupInfo.filePath)
        assertTrue("Getting backup preview should succeed", previewResult.isSuccess)

        val preview = previewResult.getOrThrow()
        assertEquals("Version should be 1.0", "1.0", preview.version)
        assertEquals("Should have 1 user", 1, preview.recordCounts["pengguna"])
        assertEquals("Should have 1 category", 1, preview.recordCounts["kategori"])
        assertEquals("Should have 1 warehouse", 1, preview.recordCounts["gudang"])
        assertEquals("Should have 1 product", 1, preview.recordCounts["produk"])
        assertEquals("Should have 1 supplier", 1, preview.recordCounts["pemasok"])

        // Cleanup
        File(backupInfo.filePath).delete()
    }

    @Test
    fun testDeleteBackup() = runBlocking {
        // Setup test data
        seedTestData()

        // Create backup
        val backupInfo = backupService.createBackup().getOrThrow()
        assertTrue("Backup file should exist", File(backupInfo.filePath).exists())

        // Delete backup
        val deleteResult = backupService.deleteBackup(backupInfo.id)
        assertTrue("Delete backup should succeed", deleteResult.isSuccess)

        // Verify file is deleted
        assertFalse("Backup file should not exist after delete", File(backupInfo.filePath).exists())

        // Verify backup is removed from history
        val historyResult = backupService.getBackupHistory()
        assertTrue("Getting backup history should succeed", historyResult.isSuccess)

        val history = historyResult.getOrThrow()
        val deletedBackup = history.find { it.id == backupInfo.id }
        assertNull("Deleted backup should not be in history", deletedBackup)
    }

    @Test
    fun testInvalidBackupValidation() = runBlocking {
        // Try to validate non-existent file
        val validationResult = backupService.validateBackup("/non/existent/file.enc")
        assertTrue("Validation should succeed but return invalid", validationResult.isSuccess)

        val validation = validationResult.getOrThrow()
        assertFalse("Validation should fail for non-existent file", validation.isValid)
        assertTrue("Should have errors", validation.errors.isNotEmpty())
    }
}
