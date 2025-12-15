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
        Index(value = ["createdBy"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Pemasok::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Pengguna::class,
            parentColumns = ["id"],
            childColumns = ["createdBy"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Pembelian(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierId: Long,
    @Serializable(with = DateSerializer::class)
    val purchaseDate: Date,
    val totalAmount: Double,
    val createdBy: Long,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date()
)