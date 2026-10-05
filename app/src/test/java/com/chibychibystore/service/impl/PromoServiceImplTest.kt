package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.local.entity.PromotionType
import com.chibychibystore.repository.PromotionRepository
import com.chibychibystore.util.CalendarDates
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.LocalDate
import java.util.Calendar
import java.util.Date

class PromoServiceImplTest {

    @Mock
    private lateinit var promotionRepository: PromotionRepository

    private lateinit var promoService: PromoServiceImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        promoService = PromoServiceImpl(promotionRepository)
    }

    @Test
    fun `calculateDiscount returns 0 when no promotions active`() = runTest {
        `when`(promotionRepository.getActivePromotionsForDate(any()))
            .thenReturn(flowOf(emptyList()))

        val subtotal = 100_000.0
        val discount = promoService.calculateDiscount(subtotal)
        assertEquals(0.0, discount, 0.001)
    }

    @Test
    fun `calculateDiscount applies percentage discount`() = runTest {
        val promo = Promotion(
            name = "Test Promo",
            description = "Desc",
            type = PromotionType.PERCENTAGE,
            value = 0.1, // 10%
            minPurchaseAmount = 50_000.0
        )
        `when`(promotionRepository.getActivePromotionsForDate(any()))
            .thenReturn(flowOf(listOf(promo)))

        val subtotal = 100_000.0
        val discount = promoService.calculateDiscount(subtotal)
        assertEquals(10_000.0, discount, 0.001)
    }

    @Test
    fun `calculateDiscount applies fixed amount discount`() = runTest {
        val promo = Promotion(
            name = "Fixed Promo",
            description = "Desc",
            type = PromotionType.FIXED_AMOUNT,
            value = 5_000.0,
            minPurchaseAmount = 50_000.0
        )
        `when`(promotionRepository.getActivePromotionsForDate(any()))
            .thenReturn(flowOf(listOf(promo)))

        val subtotal = 60_000.0
        val discount = promoService.calculateDiscount(subtotal)
        assertEquals(5_000.0, discount, 0.001)
    }

    @Test
    fun `calculateDiscount respects max discount amount`() = runTest {
        val promo = Promotion(
            name = "Capped Promo",
            description = "Desc",
            type = PromotionType.PERCENTAGE,
            value = 0.5, // 50%
            minPurchaseAmount = 0.0,
            maxDiscountAmount = 20_000.0
        )
        `when`(promotionRepository.getActivePromotionsForDate(any()))
            .thenReturn(flowOf(listOf(promo)))

        val subtotal = 100_000.0
        // 50% of 100k is 50k, but max is 20k
        val discount = promoService.calculateDiscount(subtotal)
        assertEquals(20_000.0, discount, 0.001)
    }

    @Test
    fun `calculateDiscount never exceeds the subtotal`() = runTest {
        // A row that predates validation (e.g. a percentage stored as 150) must
        // not discount more than the customer owes.
        val promo = Promotion(
            name = "Legacy Broken Promo",
            description = "Desc",
            type = PromotionType.PERCENTAGE,
            value = 1.5,
            minPurchaseAmount = 0.0
        )
        `when`(promotionRepository.getActivePromotionsForDate(any()))
            .thenReturn(flowOf(listOf(promo)))

        val subtotal = 100_000.0
        assertEquals(100_000.0, promoService.calculateDiscount(subtotal), 0.001)
    }

    @Test
    fun `savePromotion rejects a percentage above 100 percent`() = runTest {
        val promo = Promotion(
            name = "Too Big",
            description = "Desc",
            type = PromotionType.PERCENTAGE,
            value = 1.5
        )

        val result = promoService.savePromotion(promo)

        assertTrue(result.isFailure)
        verify(promotionRepository, never()).insertPromotion(any())
        verify(promotionRepository, never()).updatePromotion(any())
    }

    @Test
    fun `savePromotion inserts a valid new promotion`() = runTest {
        val promo = Promotion(
            name = "Valid",
            description = "Desc",
            type = PromotionType.PERCENTAGE,
            value = 0.25
        )
        `when`(promotionRepository.insertPromotion(promo)).thenReturn(42L)

        val result = promoService.savePromotion(promo)

        assertEquals(42L, result.getOrNull())
        verify(promotionRepository).insertPromotion(promo)
    }

    @Test
    fun `savePromotion normalises a picker day to its UTC day boundaries`() = runTest {
        // Material's picker reports UTC midnight of the chosen day. Storing that
        // instant verbatim would make a same-day period zero-length, and reading
        // it with a local Calendar (the old bug) would put the boundary in the
        // previous day on a device west of UTC. The period is stored as the UTC
        // boundaries of the picked day, so it means the same day everywhere.
        val pickerDay = CalendarDates.utcDayMarker(LocalDate.of(2026, 10, 1))
        val promo = Promotion(
            name = "Same Day",
            description = "Desc",
            type = PromotionType.PERCENTAGE,
            value = 0.1,
            startDate = pickerDay,
            endDate = pickerDay
        )
        `when`(promotionRepository.insertPromotion(any())).thenReturn(1L)

        val result = promoService.savePromotion(promo)

        assertTrue(result.isSuccess)
        val saved = org.mockito.kotlin.argumentCaptor<Promotion>()
        verify(promotionRepository).insertPromotion(saved.capture())
        assertEquals(CalendarDates.startOfUtcDay(pickerDay), saved.firstValue.startDate)
        assertEquals(CalendarDates.endOfUtcDay(pickerDay), saved.firstValue.endDate)
        // The stored period must cover the whole picked day, so the end is strictly
        // after the day it starts on.
        assertTrue(saved.firstValue.endDate!!.after(saved.firstValue.startDate!!))
    }

    @Test
    fun `savePromotion stores the same period regardless of the device zone`() = runTest {
        // The finding: boundaries used the zone at save time, so a device that
        // changed zone could display a different day. The stored millis must not
        // depend on the zone at all.
        val pickerDay = CalendarDates.utcDayMarker(LocalDate.of(2026, 10, 1))
        val promo = Promotion(
            name = "Zone Free",
            description = "Desc",
            type = PromotionType.FIXED_AMOUNT,
            value = 5_000.0,
            startDate = pickerDay,
            endDate = pickerDay
        )
        `when`(promotionRepository.insertPromotion(any())).thenReturn(1L)

        val original = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"))
            promoService.savePromotion(promo)
            val inUtc = org.mockito.kotlin.argumentCaptor<Promotion>()
            verify(promotionRepository).insertPromotion(inUtc.capture())

            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"))
            promoService.savePromotion(promo)
            val inLa = org.mockito.kotlin.argumentCaptor<Promotion>()
            verify(promotionRepository, org.mockito.kotlin.times(2)).insertPromotion(inLa.capture())

            assertEquals(inUtc.firstValue.startDate, inLa.secondValue.startDate)
            assertEquals(inUtc.firstValue.endDate, inLa.secondValue.endDate)
        } finally {
            java.util.TimeZone.setDefault(original)
        }
    }

    @Test
    fun `savePromotion stores a period that brackets the whole picked UTC day`() = runTest {
        // Zone-independent check: the stored window is exactly the picked UTC day,
        // so it also brackets the query boundary used when applying a discount.
        val pickerDay = CalendarDates.utcDayMarker(LocalDate.of(2026, 10, 1))
        val promo = Promotion(
            name = "Window",
            description = "Desc",
            type = PromotionType.FIXED_AMOUNT,
            value = 5_000.0,
            startDate = pickerDay,
            endDate = pickerDay
        )
        `when`(promotionRepository.insertPromotion(any())).thenReturn(1L)

        promoService.savePromotion(promo)

        val saved = org.mockito.kotlin.argumentCaptor<Promotion>()
        verify(promotionRepository).insertPromotion(saved.capture())
        val stored = saved.firstValue
        assertEquals(LocalDate.of(2026, 10, 1), CalendarDates.utcDay(stored.startDate!!))
        assertEquals(LocalDate.of(2026, 10, 1), CalendarDates.utcDay(stored.endDate!!))
        // startDate <= query <= endDate for a same-day promotion at the UTC start
        // of that day (the DAO's comparison).
        val query = CalendarDates.startOfUtcDay(pickerDay)
        assertTrue(!stored.startDate.after(query))
        assertTrue(!stored.endDate.before(query))
    }

    @Test
    fun `a same-day promotion applies on its final day west of UTC`() = runTest {
        // Reproduces the reported bug: in UTC-7 the old end-of-day landed on the
        // previous local day, so an October 1 promotion was unavailable all of
        // October 1. With UTC boundaries the stored window covers the day.
        val original = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("America/Los_Angeles"))
            val pickerDay = CalendarDates.utcDayMarker(LocalDate.of(2026, 10, 1))
            val promo = Promotion(
                name = "Final Day",
                description = "Desc",
                type = PromotionType.FIXED_AMOUNT,
                value = 5_000.0,
                startDate = pickerDay,
                endDate = pickerDay
            )
            `when`(promotionRepository.insertPromotion(any())).thenReturn(1L)
            promoService.savePromotion(promo)
            val saved = org.mockito.kotlin.argumentCaptor<Promotion>()
            verify(promotionRepository).insertPromotion(saved.capture())
            val stored = saved.firstValue

            // Midday on the picked day, expressed in UTC, must fall in the window.
            val middayUtc = CalendarDates.utcDayMarker(LocalDate.of(2026, 10, 1))
                .let { Date(it.time + 12 * 60 * 60 * 1000L) }
            assertTrue("start must not be after midday UTC", !stored.startDate!!.after(middayUtc))
            assertTrue("end must not be before midday UTC", !stored.endDate!!.before(middayUtc))
        } finally {
            java.util.TimeZone.setDefault(original)
        }
    }

    @Test
    fun `calculateDiscount queries the day-marker of today's local calendar date in Jakarta`() = runTest {
        // The finding: at 01:00 local on 1 October in Jakarta the UTC instant is
        // still 30 September 18:00, so deriving the day from "now" in UTC opened
        // 30 September and dropped the October 1 promotion until 07:00 local.
        `when`(promotionRepository.getActivePromotionsForDate(any())).thenReturn(flowOf(emptyList()))

        val zone = java.time.ZoneId.of("Asia/Jakarta")
        withZone(zone) {
            promoService.now = { localEpoch(2026, 10, 1, 1, 0, zone) }

            promoService.calculateDiscount(50_000.0)

            val boundary = org.mockito.kotlin.argumentCaptor<Date>()
            verify(promotionRepository).getActivePromotionsForDate(boundary.capture())
            assertEquals(
                "the query must open the local day, not the UTC day",
                CalendarDates.utcDayMarker(LocalDate.of(2026, 10, 1)),
                boundary.firstValue
            )
        }
    }

    @Test
    fun `calculateDiscount queries the day-marker of today's local calendar date west of UTC`() = runTest {
        // The symmetric case: at 01:00 local on 1 October in Los Angeles the UTC
        // instant is 08:00 on 1 October, so a UTC-derived day happens to agree —
        // but the intended local day is what must be queried and stored.
        `when`(promotionRepository.getActivePromotionsForDate(any())).thenReturn(flowOf(emptyList()))

        val zone = java.time.ZoneId.of("America/Los_Angeles")
        withZone(zone) {
            promoService.now = { localEpoch(2026, 10, 1, 1, 0, zone) }

            promoService.calculateDiscount(50_000.0)

            val boundary = org.mockito.kotlin.argumentCaptor<Date>()
            verify(promotionRepository).getActivePromotionsForDate(boundary.capture())
            assertEquals(
                CalendarDates.utcDayMarker(LocalDate.of(2026, 10, 1)),
                boundary.firstValue
            )
        }
    }

    @Test
    fun `a same-day promotion is active for the whole local day in Jakarta`() = runTest {
        // Save a 1 October period, then query at 01:00 and at 23:00 local on that
        // day. Both must fall inside the stored window; the old UTC-derived day
        // missed everything before 07:00 local.
        val zone = java.time.ZoneId.of("Asia/Jakarta")
        withZone(zone) {
            val pickerDay = CalendarDates.utcDayMarker(LocalDate.of(2026, 10, 1))
            val promo = Promotion(
                name = "Sehari",
                description = "Desc",
                type = PromotionType.FIXED_AMOUNT,
                value = 5_000.0,
                startDate = pickerDay,
                endDate = pickerDay
            )
            `when`(promotionRepository.insertPromotion(any())).thenReturn(1L)
            promoService.savePromotion(promo)
            val saved = org.mockito.kotlin.argumentCaptor<Promotion>()
            verify(promotionRepository).insertPromotion(saved.capture())
            val stored = saved.firstValue

            for (hour in intArrayOf(1, 12, 23)) {
                val query = CalendarDates.utcDayMarker(
                    CalendarDates.localDate(Date(localEpoch(2026, 10, 1, hour, 0, zone)))
                )
                assertTrue(
                    "start must not be after the query at ${hour}:00 local",
                    !stored.startDate!!.after(query)
                )
                assertTrue(
                    "end must not be before the query at ${hour}:00 local",
                    !stored.endDate!!.before(query)
                )
            }
        }
    }

    @Test
    fun `savePromotion accepts a same-day window`() = runTest {
        val day = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val promo = Promotion(
            name = "Same Day",
            description = "Desc",
            type = PromotionType.FIXED_AMOUNT,
            value = 5_000.0,
            startDate = day,
            endDate = day
        )
        `when`(promotionRepository.insertPromotion(any())).thenReturn(1L)

        assertTrue(promoService.savePromotion(promo).isSuccess)
    }

    @Test
    fun `savePromotion rejects an end date before the start date`() = runTest {
        // A full day apart, since a same-day window is valid.
        val promo = Promotion(
            name = "Backwards",
            description = "Desc",
            type = PromotionType.FIXED_AMOUNT,
            value = 1_000.0,
            startDate = Date(2 * 24 * 60 * 60 * 1000L),
            endDate = Date(1 * 24 * 60 * 60 * 1000L)
        )

        val result = promoService.savePromotion(promo)

        assertTrue(result.isFailure)
        verify(promotionRepository, never()).insertPromotion(any())
    }

    @Test
    fun `calculateDiscount picks best discount among multiple`() = runTest {
        val promo1 = Promotion(
            id = 1,
            name = "Promo 1",
            description = "Desc",
            type = PromotionType.FIXED_AMOUNT,
            value = 5_000.0
        )
        val promo2 = Promotion(
            id = 2,
            name = "Promo 2",
            description = "Desc",
            type = PromotionType.FIXED_AMOUNT,
            value = 10_000.0
        )
        `when`(promotionRepository.getActivePromotionsForDate(any()))
            .thenReturn(flowOf(listOf(promo1, promo2)))

        val subtotal = 100_000.0
        val discount = promoService.calculateDiscount(subtotal)
        assertEquals(10_000.0, discount, 0.001)
    }

    private companion object {
        /** Epoch millis for a wall-clock time in [zone] (for the injected clock). */
        fun localEpoch(year: Int, month: Int, day: Int, hour: Int, minute: Int, zone: java.time.ZoneId): Long =
            java.time.LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

        /** Runs suspending [block] with the JVM default zone set to [zone]. */
        suspend fun withZone(zone: java.time.ZoneId, block: suspend () -> Unit) {
            val original = java.util.TimeZone.getDefault()
            try {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(zone))
                block()
            } finally {
                java.util.TimeZone.setDefault(original)
            }
        }
    }
}
