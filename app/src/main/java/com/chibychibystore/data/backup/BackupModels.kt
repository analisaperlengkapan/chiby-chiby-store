package com.chibychibystore.data.backup

import com.chibychibystore.data.local.entity.*
import kotlinx.serialization.Serializable

/**
 * Root container for backup data
 */
@Serializable
data class BackupData(
    val version: String = "1.0",
    val createdAt: Long,
    val metadata: BackupMetadata,
    val data: BackupEntities
)

/**
 * Metadata backup
 */
@Serializable
data class BackupMetadata(
    val appVersion: String = "1.0.0",
    val deviceInfo: String = "Android POS",
    val checksum: String
)

/**
 * Container for all backup entities
 */
@Serializable
data class BackupEntities(
    val users: List<User>,
    val categories: List<Category>,
    val warehouses: List<Warehouse>,
    val products: List<Product>,
    val suppliers: List<Supplier>,
    val sales: List<Sale>,
    val saleItems: List<SaleItem>,
    val purchases: List<Purchase>,
    val purchaseItems: List<PurchaseItem>,
    val expenses: List<Expense>
)
