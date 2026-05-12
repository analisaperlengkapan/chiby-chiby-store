package com.chibychibystore.service.impl
import org.robolectric.annotation.Config

import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.local.entity.PromotionType
import com.chibychibystore.repository.PromotionRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.ArgumentMatchers

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
        `when`(promotionRepository.getActivePromotionsForDate(ArgumentMatchers.any(java.util.Date::class.java)))
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
        `when`(promotionRepository.getActivePromotionsForDate(ArgumentMatchers.any(java.util.Date::class.java)))
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
        `when`(promotionRepository.getActivePromotionsForDate(ArgumentMatchers.any(java.util.Date::class.java)))
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
        `when`(promotionRepository.getActivePromotionsForDate(ArgumentMatchers.any(java.util.Date::class.java)))
            .thenReturn(flowOf(listOf(promo)))

        val subtotal = 100_000.0
        // 50% of 100k is 50k, but max is 20k
        val discount = promoService.calculateDiscount(subtotal)
        assertEquals(20_000.0, discount, 0.001)
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
        `when`(promotionRepository.getActivePromotionsForDate(ArgumentMatchers.any(java.util.Date::class.java)))
            .thenReturn(flowOf(listOf(promo1, promo2)))

        val subtotal = 100_000.0
        val discount = promoService.calculateDiscount(subtotal)
        assertEquals(10_000.0, discount, 0.001)
    }
}
