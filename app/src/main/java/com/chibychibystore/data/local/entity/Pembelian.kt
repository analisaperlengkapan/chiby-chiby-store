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
    tableName = "pembelian",
    indices = [
        Index(value = ["purchaseDate"]),
        Index(value = ["supplierId"]),
        Index(value = ["receivedBy"]),
        Index(value = ["warehouseId"]),
        Index(value = ["invoiceNumber"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = Pemasok::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Pengguna::class,
            parentColumns = ["id"],
            childColumns = ["receivedBy"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Gudang::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Pembelian(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @Serializable(with = DateSerializer::class)
    val purchaseDate: Date = Date(),
    val supplierId: Long,
    val warehouseId: Long = 1, // Default to 1 for backward compatibility if needed
    val invoiceNumber: String,
    val totalAmount: Double,
    val notes: String? = null,
    val receivedBy: Long? = null,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date(),
    @Serializable(with = DateSerializer::class)
    val updatedAt: Date = Date()
)
