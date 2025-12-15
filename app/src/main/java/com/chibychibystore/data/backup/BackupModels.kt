package com.chibychibystore.data.backup

import com.chibychibystore.data.local.entity.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

/**
 * Root container untuk backup data
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
 * Container untuk semua entity backup
 */
@Serializable
data class BackupEntities(
    @Contextual val pengguna: List<Pengguna>,
    @Contextual val kategori: List<Kategori>,
    @Contextual val gudang: List<Gudang>,
    @Contextual val produk: List<Produk>,
    @Contextual val pemasok: List<Pemasok>,
    @Contextual val penjualan: List<Penjualan>,
    @Contextual val itemPenjualan: List<ItemPenjualan>,
    @Contextual val pembelian: List<Pembelian>,
    @Contextual val itemPembelian: List<ItemPembelian>,
    @Contextual val pengeluaran: List<Pengeluaran>
)