package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.local.entity.PromotionType
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.PromotionRepository
import com.chibychibystore.service.PromoService
import com.chibychibystore.util.CalendarDates
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromoServiceImpl @Inject constructor(
    private val promotionRepository: PromotionRepository
) : PromoService {

    /**
     * Wall clock, read through a field so tests can pin "now" and exercise the
     * local-day boundary without waiting. Production uses [System.currentTimeMillis].
     */
    internal var now: () -> Long = System::currentTimeMillis

    override suspend fun calculateDiscount(subtotal: Double): Double {
        // A period is stored as the UTC boundaries of the picked day, and the
        // list shows those days in UTC. The query therefore has to ask for the
        // day-marker of the *current local calendar date*: deriving the day from
        // the current UTC instant would, at local midnight outside UTC, open
        // yesterday's or tomorrow's marker and apply the wrong day's promotions
        // (a Jakarta October 1 promotion would miss its first and last hours).
        val today = CalendarDates.utcDayMarker(CalendarDates.localDate(Date(now())))
        val promotions = promotionRepository.getActivePromotionsForDate(today).first()

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
            // The picker emits UTC midnight of the chosen day. Persist the start
            // and end at that day's boundaries so the period covers the whole of
            // the first and last selected days, regardless of the device zone.
            val normalized = promotion.copy(
                startDate = promotion.startDate?.let { CalendarDates.startOfUtcDay(it) },
                endDate = promotion.endDate?.let { CalendarDates.endOfUtcDay(it) }
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
            CalendarDates.utcDay(promotion.endDate).isBefore(CalendarDates.utcDay(promotion.startDate))
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
