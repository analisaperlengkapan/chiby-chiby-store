package com.chibychibystore.service

import com.chibychibystore.data.model.BackupFile
import com.chibychibystore.data.model.BackupMetadata
import com.chibychibystore.data.model.RestorePreview
import java.io.File

interface BackupService {
    /**
     * Membuat backup lengkap dari semua data bisnis
     * @return Result dengan informasi file backup yang dibuat
     */
    suspend fun createBackup(): Result<BackupFile>

    /**
     * Membuat backup terjadwal berdasarkan frekuensi
     * @param frequency Frekuensi backup (DAILY, WEEKLY, MONTHLY)
     * @return Result boolean menunjukkan keberhasilan
     */
    suspend fun scheduleBackup(frequency: String): Result<Boolean>

    /**
     * Mendapatkan riwayat backup yang tersedia
     * @return Result dengan list file backup
     */
    suspend fun getBackupHistory(): Result<List<BackupFile>>

    /**
     * Memvalidasi integritas file backup
     * @param file File backup yang akan divalidasi
     * @return Result boolean menunjukkan validitas file
     */
    suspend fun validateBackup(file: File): Result<Boolean>

    /**
     * Mendapatkan metadata dari file backup tanpa mendekripsi
     * @param file File backup
     * @return Result dengan metadata backup
     */
    suspend fun getBackupMetadata(file: File): Result<BackupMetadata>
}

interface RestoreService {
    /**
     * Merestore data dari file backup
     * @param file File backup yang akan direstore
     * @return Result boolean menunjukkan keberhasilan
     */
    suspend fun restoreFromBackup(file: File): Result<Boolean>

    /**
     * Merestore data parsial berdasarkan rentang tanggal
     * @param file File backup
     * @param startDate Tanggal mulai (format YYYY-MM-DD)
     * @param endDate Tanggal akhir (format YYYY-MM-DD)
     * @return Result boolean menunjukkan keberhasilan
     */
    suspend fun partialRestore(file: File, startDate: String, endDate: String): Result<Boolean>

    /**
     * Mendapatkan preview isi backup sebelum restore
     * @param file File backup
     * @return Result dengan preview data yang akan direstore
     */
    suspend fun getRestorePreview(file: File): Result<RestorePreview>
}