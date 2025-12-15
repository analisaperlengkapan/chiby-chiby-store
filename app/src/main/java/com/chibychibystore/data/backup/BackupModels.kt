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
    val pengguna: List<Pengguna>,
    val kategori: List<Kategori>,
    val gudang: List<Gudang>,
    val produk: List<Produk>,
    val pemasok: List<Pemasok>,
    val penjualan: List<Penjualan>,
    val itemPenjualan: List<ItemPenjualan>,
    val pembelian: List<Pembelian>,
    val itemPembelian: List<ItemPembelian>,
    val pengeluaran: List<Pengeluaran>
)