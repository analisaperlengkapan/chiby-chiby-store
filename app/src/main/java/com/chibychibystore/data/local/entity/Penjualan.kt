package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chibychibystore.data.serialization.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
@Entity(
    tableName = "penjualan",
    indices = [
        Index(value = ["saleDate"]),
        Index(value = ["cashierId", "saleDate"]),
        Index(value = ["paymentMethod"]),
        Index(value = ["shiftId"]),
        Index(value = ["pelangganId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Pengguna::class,
            parentColumns = ["id"],
            childColumns = ["cashierId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Shift::class,
            parentColumns = ["id"],
            childColumns = ["shiftId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Pelanggan::class,
            parentColumns = ["id"],
            childColumns = ["pelangganId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Penjualan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @Serializable(with = DateSerializer::class)
    val saleDate: Date,
    val totalAmount: Double,
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val paymentMethod: PaymentMethod,
    val cashierId: Long,
    val shiftId: Long? = null,
    val pelangganId: Long? = null,
    // Warehouse where the sale occurred. Defaults to 1 for backward compatibility.
    val warehouseId: Long = 1,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date(),
    // Whether this sale has been refunded. Default false for existing records.
    val isRefunded: Boolean = false
)

@Serializable
enum class PaymentMethod {
    CASH,
    CARD,
    QRIS
}
