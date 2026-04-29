package com.chibychibystore.data.backup

import com.chibychibystore.data.local.entity.*
import kotlinx.serialization.Serializable

/**
 * Root container for backup data.
 *
 * Backup format versions:
 *  - "1.0": Penjualan.totalAmount stored the raw item subtotal (the old
 *    PenjualanRepository.createPenjualan overwrote the caller-supplied value).
 *    Tax and discount were stored separately.
 *  - "2.0": Penjualan.totalAmount stores the post-tax/discount amount actually
 *    paid (`max(0, subtotal + tax - discount)`), matching MIGRATION_11_12.
 *
 * The default value reflects the legacy "1.0" semantics so that backup files
 * written by older app versions (which don't include a `version` field) parse
 * with the correct interpretation. RestoreServiceImpl uses this version to
 * decide whether to apply the totalAmount backfill on import.
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
    val expenses: List<Pengeluaran>,
    // Default to empty list so older backup files (created before these tables existed)
    // still deserialize successfully. Penjualan rows reference these tables via
    // shiftId / pelangganId foreign keys, so they MUST be restored before sales to
    // avoid FK violations and silent data loss.
    val shifts: List<Shift> = emptyList(),
    val customers: List<Pelanggan> = emptyList(),
    // Per-warehouse stock rows. Without these, a full restore (which calls
    // clearAllTables) leaves `stok_gudang` empty while `Produk.stockQuantity`
    // is restored from backup, breaking the consistency that
    // PurchaseService / InventoryAuditService / sales rely on. Default to empty
    // list for backward compatibility with older backup files.
    val stocks: List<StokGudang> = emptyList(),
    // Inventory audit history. Compliance/audit-trail data; preserved across
    // backup/restore cycles. Default to empty for older backup files.
    val audits: List<StokOpname> = emptyList(),
    val auditItems: List<ItemStokOpname> = emptyList()
)
