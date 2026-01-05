package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chibychibystore.data.serialization.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

/**
 * Enum untuk kategori pengeluaran
 */
@Serializable
enum class KategoriPengeluaran(val displayName: String) {
    // Harga Pokok Penjualan
    INVENTORY_PURCHASES("Pembelian Inventory"),

    // Beban Operasional
    RENT_LEASE("Sewa & Sewa Guna Usaha"),
    UTILITIES("Utilitas"),
    SALARIES_WAGES("Gaji & Upah"),
    MARKETING_ADVERTISING("Pemasaran & Iklan"),
    INSURANCE("Asuransi"),
    SUPPLIES_MAINTENANCE("Suplies & Perawatan"),
    DEPRECIATION("Depresiasi"),
    PROFESSIONAL_SERVICES("Jasa Profesional"),
    MISCELLANEOUS("Lain-lain");

    companion object {
        fun fromDisplayName(displayName: String): KategoriPengeluaran? {
            return values().find { it.displayName == displayName }
        }

        // Grup kategori untuk pelaporan
        val COGS_CATEGORIES = setOf(INVENTORY_PURCHASES)
        val OPERATING_EXPENSE_CATEGORIES = values().toSet() - COGS_CATEGORIES
    }
}

@Serializable
@Entity(
    tableName = "pengeluaran",
    indices = [
        Index(value = ["expenseDate"]),
        Index(value = ["category"]),
        Index(value = ["createdBy"]),
        Index(value = ["approvedBy"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Pengguna::class,
            parentColumns = ["id"],
            childColumns = ["approvedBy"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Pengguna::class,
            parentColumns = ["id"],
            childColumns = ["createdBy"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Pengeluaran(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @Serializable(with = DateSerializer::class)
    val expenseDate: Date,
    val category: KategoriPengeluaran,
    val amount: Double,
    val description: String? = null,
    val approvedBy: Long? = null,
    val createdBy: Long,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date()
)
