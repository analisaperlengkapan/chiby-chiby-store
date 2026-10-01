package com.chibychibystore.service.impl

import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.local.entity.PromotionType
import com.chibychibystore.repository.PromotionRepository
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
    fun `savePromotion rejects an end date before the start date`() = runTest {
        val promo = Promotion(
            name = "Backwards",
            description = "Desc",
            type = PromotionType.FIXED_AMOUNT,
            value = 1_000.0,
            startDate = Date(2_000_000L),
            endDate = Date(1_000_000L)
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
}
