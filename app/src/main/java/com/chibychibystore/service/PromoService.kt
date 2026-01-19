package com.chibychibystore.service

interface PromoService {
    /**
     * Calculates the discount based on the subtotal.
     * Uses active promotions from the database.
     */
    suspend fun calculateDiscount(subtotal: Double): Double
}
