package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "item_penjualan",
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Penjualan::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Produk::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ItemPenjualan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    // Store cost price at the time of sale for accurate historical COGS reporting
    val costPrice: Double = 0.0
)
