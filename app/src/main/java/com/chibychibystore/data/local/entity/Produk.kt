package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "produk",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["warehouseId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Kategori::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
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
data class Produk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val barcode: String? = null,
    val categoryId: Long,
    val costPrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Int = 0,
    val warehouseId: Long,
    val minStock: Int = 0,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)