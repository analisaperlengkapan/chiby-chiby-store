package com.chibychibystore.service.impl

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PromoServiceImplTest {

    private lateinit var promoService: PromoServiceImpl

    @Before
    fun setup() {
        promoService = PromoServiceImpl()
    }

    @Test
    fun `calculateDiscount returns 0 when subtotal is below 100,000`() {
        val subtotal = 99_000.0
        val discount = promoService.calculateDiscount(subtotal)
        assertEquals(0.0, discount, 0.001)
    }

    @Test
    fun `calculateDiscount returns 5 percent when subtotal is exactly 100,000`() {
        val subtotal = 100_000.0
        val discount = promoService.calculateDiscount(subtotal)
        val expected = 100_000.0 * 0.05
        assertEquals(expected, discount, 0.001)
    }

    @Test
    fun `calculateDiscount returns 5 percent when subtotal is above 100,000`() {
        val subtotal = 200_000.0
        val discount = promoService.calculateDiscount(subtotal)
        val expected = 200_000.0 * 0.05
        assertEquals(expected, discount, 0.001)
    }
}
