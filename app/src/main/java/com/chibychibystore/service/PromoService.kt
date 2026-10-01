package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.model.Result

interface PromoService {
    /**
     * Calculates the discount based on the subtotal.
     * Uses active promotions from the database.
     */
    suspend fun calculateDiscount(subtotal: Double): Double

    /**
     * Validates and persists a promotion. A new promotion (id == 0) is inserted,
     * otherwise the existing row is updated. Invalid promotions are rejected
     * rather than written to the database.
     */
    suspend fun savePromotion(promotion: Promotion): Result<Long>
}
