package com.chibychibystore.data.backup

import com.chibychibystore.data.local.entity.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

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
    val pengguna: List<User>,
    val kategori: List<Category>,
    val gudang: List<Warehouse>,
    val product: List<Product>,
    val pemasok: List<Supplier>,
    val penjualan: List<Sale>,
    val itemPenjualan: List<SaleItem>,
    val pembelian: List<Purchase>,
    val itemPembelian: List<PurchaseItem>,
    val pengeluaran: List<Expense>
)
