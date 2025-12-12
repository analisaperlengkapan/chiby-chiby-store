package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Enum untuk kategori pengeluaran retail
 */
enum class ExpenseCategory(val displayName: String) {
    // Cost of Goods Sold
    INVENTORY_PURCHASES("Pembelian Inventory"),

    // Operating Expenses
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
        fun fromDisplayName(displayName: String): ExpenseCategory? {
            return values().find { it.displayName == displayName }
        }

        // Group categories for reporting
        val COGS_CATEGORIES = setOf(INVENTORY_PURCHASES)
        val OPERATING_EXPENSE_CATEGORIES = values().toSet() - COGS_CATEGORIES
    }
}

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
    val expenseDate: Date,
    val category: ExpenseCategory,
    val amount: Double,
    val description: String? = null,
    val approvedBy: Long? = null,
    val createdBy: Long,
    val createdAt: Date = Date()
)