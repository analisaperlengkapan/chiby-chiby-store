package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.local.entity.PromotionType
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
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
        val best = applicablePromotions.maxOfOrNull { promo ->
            calculateDiscountForPromo(subtotal, promo)
        } ?: 0.0

        // A discount can never exceed the subtotal it applies to. Promotions are
        // validated on save, but this guards any row already in the database.
        return best.coerceIn(0.0, subtotal)
    }

    override suspend fun savePromotion(promotion: Promotion): Result<Long> {
        return try {
            validate(promotion)
            val id = if (promotion.id == 0L) {
                promotionRepository.insertPromotion(promotion)
            } else {
                promotionRepository.updatePromotion(promotion)
                promotion.id
            }
            Result.success(id)
        } catch (e: ChibyChibyException.ValidationError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("savePromotion", e))
        }
    }

    /**
     * A percentage value is stored as a fraction (0.1 == 10%), so anything above
     * 1.0 would discount more than the whole subtotal. Non-percentage amounts and
     * the optional cap must simply be non-negative.
     */
    private fun validate(promotion: Promotion) {
        if (promotion.name.isBlank()) {
            throw ChibyChibyException.ValidationError("name", "Nama promosi tidak boleh kosong")
        }
        if (promotion.value < 0.0) {
            throw ChibyChibyException.ValidationError("value", "Nilai promosi tidak boleh negatif")
        }
        if (promotion.type == PromotionType.PERCENTAGE && promotion.value > 1.0) {
            throw ChibyChibyException.ValidationError("value", "Persentase promosi maksimal 100%")
        }
        if (promotion.minPurchaseAmount < 0.0) {
            throw ChibyChibyException.ValidationError("minPurchaseAmount", "Minimum pembelian tidak boleh negatif")
        }
        if (promotion.maxDiscountAmount != null && promotion.maxDiscountAmount < 0.0) {
            throw ChibyChibyException.ValidationError("maxDiscountAmount", "Maksimal diskon tidak boleh negatif")
        }
        if (promotion.startDate != null && promotion.endDate != null &&
            promotion.endDate.before(promotion.startDate)
        ) {
            throw ChibyChibyException.ValidationError("endDate", "Tanggal berakhir harus setelah tanggal mulai")
        }
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
