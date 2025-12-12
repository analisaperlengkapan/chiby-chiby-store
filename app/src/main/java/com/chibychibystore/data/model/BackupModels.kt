package com.chibychibystore.data.model

import java.io.File
import java.util.Date

/**
 * Model untuk file backup
 */
data class BackupFile(
    val file: File,
    val fileName: String,
    val createdAt: Date,
    val size: Long,
    val metadata: BackupMetadata
)

/**
 * Metadata dari file backup
 */
data class BackupMetadata(
    val version: String,
    val createdAt: Date,
    val appVersion: String,
    val recordCounts: BackupRecordCounts,
    val checksum: String
)

/**
 * Jumlah record dalam backup
 */
data class BackupRecordCounts(
    val users: Int = 0,
    val categories: Int = 0,
    val warehouses: Int = 0,
    val products: Int = 0,
    val suppliers: Int = 0,
    val sales: Int = 0,
    val saleItems: Int = 0,
    val purchases: Int = 0,
    val purchaseItems: Int = 0,
    val expenses: Int = 0
)

/**
 * Preview data yang akan direstore
 */
data class RestorePreview(
    val metadata: BackupMetadata,
    val conflicts: List<RestoreConflict>,
    val warnings: List<String>
)

/**
 * Konflik data saat restore
 */
data class RestoreConflict(
    val type: ConflictType,
    val entityType: String,
    val entityId: Any,
    val description: String
)

/**
 * Tipe konflik restore
 */
enum class ConflictType {
    DUPLICATE_KEY,
    DATA_INTEGRITY,
    MISSING_DEPENDENCY,
    VERSION_MISMATCH
}

/**
 * Data lengkap untuk backup
 */
data class BackupData(
    val metadata: BackupMetadata,
    val users: List<Any> = emptyList(),
    val categories: List<Any> = emptyList(),
    val warehouses: List<Any> = emptyList(),
    val products: List<Any> = emptyList(),
    val suppliers: List<Any> = emptyList(),
    val sales: List<Any> = emptyList(),
    val saleItems: List<Any> = emptyList(),
    val purchases: List<Any> = emptyList(),
    val purchaseItems: List<Any> = emptyList(),
    val expenses: List<Any> = emptyList()
)