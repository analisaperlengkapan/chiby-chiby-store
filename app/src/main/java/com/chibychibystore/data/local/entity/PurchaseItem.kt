package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "item_pembelian",
    indices = [
        Index(value = ["purchaseId"]),
        Index(value = ["productId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Purchase::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PurchaseItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)
