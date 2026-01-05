package com.chibychibystore.service

import kotlinx.serialization.Serializable
data class RestoreResult(
    val success: Boolean,
    val recordsRestored: Map<String, Int>,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

@Serializable
data class BackupPreview(
    val version: String? = null,
    val createdAt: Long? = null,
    val recordCounts: Map<String, Int>? = null,
    val sizeBytes: Long? = null
)

@Serializable
data class BackupValidationResult(
    val isValid: Boolean,
    val version: String? = null,
    val createdAt: Long? = null,
    val recordCounts: Map<String, Int>? = null,
    val errors: List<String> = emptyList()
)
