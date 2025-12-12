package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "penjualan",
    indices = [
        Index(value = ["saleDate"]),
        Index(value = ["cashierId"])
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
    val saleDate: Date,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val cashierId: Long,
    val createdAt: Date = Date()
)

enum class PaymentMethod {
    CASH,
    CARD
}