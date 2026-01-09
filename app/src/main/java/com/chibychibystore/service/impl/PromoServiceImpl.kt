package com.chibychibystore.service.impl

import com.chibychibystore.service.PromoService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromoServiceImpl @Inject constructor() : PromoService {

    override fun calculateDiscount(subtotal: Double): Double {
        // Simple discount rule: 5% discount for subtotal >= 100,000 (Rp)
        return if (subtotal >= 100_000.0) {
            subtotal * 0.05
        } else {
            0.0
        }
    }
}
