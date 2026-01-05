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
    val users: List<Pengguna>,
    val categories: List<Kategori>,
    val warehouses: List<Gudang>,
    val products: List<Produk>,
    val suppliers: List<Pemasok>,
    val sales: List<Penjualan>,
    val saleItems: List<ItemPenjualan>,
    val purchases: List<Pembelian>,
    val purchaseItems: List<ItemPembelian>,
    val expenses: List<Pengeluaran>
)
