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
    tableName = "produk",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["warehouseId"]),
        Index(value = ["name"]),
        Index(value = ["stockQuantity"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Warehouse::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Product(
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
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date(),
    @Serializable(with = DateSerializer::class)
    val updatedAt: Date = Date()
)
