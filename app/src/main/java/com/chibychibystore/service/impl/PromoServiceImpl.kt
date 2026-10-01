package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.local.entity.PromotionType
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.PromotionRepository
import com.chibychibystore.service.PromoService
import kotlinx.coroutines.flow.first
import java.util.Calendar
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
            // The date picker returns midnight for the chosen day. Persist the end
            // date at the end of that day so a promotion ending "today" still
            // applies for the rest of today instead of expiring at 00:00.
            val normalized = promotion.copy(
                endDate = promotion.endDate?.let { endOfDay(it) }
            )
            val id = if (normalized.id == 0L) {
                promotionRepository.insertPromotion(normalized)
            } else {
                promotionRepository.updatePromotion(normalized)
                normalized.id
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
     *
     * `endDate` is normalised to the end of its calendar day: the date picker
     * returns midnight, and the discount query compares full timestamps, so an
     * end date left at 00:00 would make a same-day promotion expire the instant
     * it was saved. Comparing calendar days keeps "endDate after startDate"
     * meaningful for a same-day window.
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
            endOfDay(promotion.endDate).before(startOfDay(promotion.startDate))
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

    /** Midnight at the start of [date]'s calendar day. */
    private fun startOfDay(date: Date): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    /** The last millisecond of [date]'s calendar day. */
    private fun endOfDay(date: Date): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.time
}
