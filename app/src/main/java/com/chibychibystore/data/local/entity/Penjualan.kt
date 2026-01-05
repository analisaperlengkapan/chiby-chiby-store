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
        Index(value = ["paymentMethod"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Pengguna::class,
            parentColumns = ["id"],
            childColumns = ["cashierId"],
            onDelete = ForeignKey.CASCADE
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
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date(),
    // Whether this sale has been refunded. Default false for existing records.
    val isRefunded: Boolean = false
)

@Serializable
enum class PaymentMethod {
    CASH,
    CARD
}
