package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "item_stok_opname",
    indices = [
        Index(value = ["auditId"]),
        Index(value = ["productId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = StokOpname::class,
            parentColumns = ["id"],
            childColumns = ["auditId"],
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
data class ItemStokOpname(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val auditId: Long,
    val productId: Long,
    val expectedQuantity: Int,
    val actualQuantity: Int,
    val difference: Int, // actual - expected
    val reason: String? = null
)
