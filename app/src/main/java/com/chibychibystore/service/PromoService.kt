package com.chibychibystore.service

interface PromoService {
    /**
     * Calculates the discount based on the subtotal.
     * Current rule: 5% discount if subtotal >= 100,000.
     */
    fun calculateDiscount(subtotal: Double): Double
}
