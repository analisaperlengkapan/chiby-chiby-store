package com.chibychibystore.data.model

enum class PromotionType {
    PERCENTAGE,
    FIXED_AMOUNT
}

data class Promotion(
    val id: String,
    val name: String,
    val description: String,
    val type: PromotionType,
    val value: Double,
    val minPurchaseAmount: Double = 0.0,
    val maxDiscountAmount: Double? = null,
    val isActive: Boolean = true
)
