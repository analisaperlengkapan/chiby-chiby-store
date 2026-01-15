package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.local.entity.PromotionType
import com.chibychibystore.repository.PromotionRepository
import com.chibychibystore.service.PromoService
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromoServiceImpl @Inject constructor(
    private val promotionRepository: PromotionRepository
) : PromoService {

    override suspend fun calculateDiscount(subtotal: Double): Double {
        // Fetch active promotions for today
        val promotions = promotionRepository.getActivePromotionsForDate(Date()).first()

        val applicablePromotions = promotions.filter { promo ->
             subtotal >= promo.minPurchaseAmount
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
