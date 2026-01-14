package com.chibychibystore.service.impl

import com.chibychibystore.data.model.Promotion
import com.chibychibystore.data.model.PromotionType
import com.chibychibystore.service.PromoService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromoServiceImpl @Inject constructor() : PromoService {

    // Internal list of promotions acting as a simple engine/repository
    private val promotions = listOf(
        Promotion(
            id = "DEFAULT_PROMO",
            name = "Diskon Belanja > 100rb",
            description = "Dapatkan diskon 5% untuk setiap pembelian di atas Rp 100.000",
            type = PromotionType.PERCENTAGE,
            value = 0.05,
            minPurchaseAmount = 100_000.0,
            isActive = true
        )
    )

    override fun calculateDiscount(subtotal: Double): Double {
        val applicablePromotions = promotions.filter { promo ->
            promo.isActive && subtotal >= promo.minPurchaseAmount
        }

        // Apply the best discount (highest amount)
        return applicablePromotions.maxOfOrNull { promo ->
            calculateDiscountForPromo(subtotal, promo)
        } ?: 0.0
    }

    private fun calculateDiscountForPromo(subtotal: Double, promo: Promotion): Double {
        var discount = when (promo.type) {
            PromotionType.PERCENTAGE -> subtotal * promo.value
            PromotionType.FIXED_AMOUNT -> promo.value
        }

        if (promo.maxDiscountAmount != null) {
            discount = discount.coerceAtMost(promo.maxDiscountAmount)
        }

        return discount
    }
}
