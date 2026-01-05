package com.chibychibystore.service

import com.chibychibystore.data.local.entity.*
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk Backup Service
 */
interface BackupService {
    /**
     * Membuat backup lengkap dari semua data bisnis
     */
    suspend fun createBackup(): Result<BackupInfo>

    /**
     * Mendapatkan daftar backup yang tersedia
     */
    suspend fun getBackupHistory(): Result<List<BackupInfo>>

    /**
     * Menghapus backup file
     */
    suspend fun deleteBackup(backupId: String): Result<Unit>

    /**
     * Validasi integritas backup file
     */
    suspend fun validateBackup(backupPath: String): Result<BackupValidationResult>

    /**
     * Observable untuk progress backup
     */
    fun observeBackupProgress(): Flow<BackupProgress>
}

/**
 * Interface untuk Restore Service
 */
interface RestoreService {
    /**
     * Restore data dari backup file
     */
    suspend fun restoreFromBackup(backupPath: String): Result<RestoreResult>

    /**
     * Preview isi backup sebelum restore
     */
    suspend fun previewBackup(backupPath: String): Result<BackupPreview>

    /**
     * Observable untuk progress restore
     */
    fun observeRestoreProgress(): Flow<RestoreProgress>
}

/**
 * Data class untuk informasi backup
 */
data class BackupInfo(
    val id: String,
    val fileName: String,
    val filePath: String,
    val createdAt: Long,
    val sizeBytes: Long,
    val version: String = "1.0",
    val checksum: String
)

/**
 * Data class untuk progress backup
 */
data class BackupProgress(
    val isInProgress: Boolean = false,
    val currentStep: String = "",
    val progress: Float = 0f,
    val totalSteps: Int = 0,
    val currentStepIndex: Int = 0
)

/**
 * Data class untuk progress restore
 */
data class RestoreProgress(
    val isInProgress: Boolean = false,
    val currentStep: String = "",
    val progress: Float = 0f,
    val totalSteps: Int = 0,
    val currentStepIndex: Int = 0
)
