package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.chibychibystore.data.serialization.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

enum class PromotionType {
    PERCENTAGE,
    FIXED_AMOUNT
}

@Serializable
@Entity(tableName = "promotion")
data class Promotion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val type: PromotionType,
    val value: Double,
    val minPurchaseAmount: Double = 0.0,
    val maxDiscountAmount: Double? = null,
    val isActive: Boolean = true,
    @Serializable(with = DateSerializer::class)
    val startDate: Date? = null,
    @Serializable(with = DateSerializer::class)
    val endDate: Date? = null,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date(),
    @Serializable(with = DateSerializer::class)
    val updatedAt: Date = Date()
)
