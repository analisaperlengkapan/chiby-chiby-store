package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "stok_gudang",
    indices = [
        Index(value = ["productId", "warehouseId"], unique = true),
        Index(value = ["warehouseId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Produk::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Gudang::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class StokGudang(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val warehouseId: Long,
    val quantity: Int
)
