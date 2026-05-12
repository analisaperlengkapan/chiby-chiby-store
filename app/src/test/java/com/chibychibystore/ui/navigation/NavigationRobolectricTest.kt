package com.chibychibystore.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationRobolectricTest {

    @Test
    fun `screen routes are defined correctly and parameterized routes build correctly`() {
        // Simple assertions to ensure navigation route strings are correct
        assertEquals("login", Screen.Login.route)
        assertEquals("dashboard", Screen.Dashboard.route)
        assertEquals("inventory", Screen.Inventory.route)
        assertEquals("pos", Screen.Pos.route)

        // Parameterized routes
        val productRoute = Screen.ProductDetail.createRoute("42")
        assertEquals("inventory/product/42", productRoute)

        val warehouseRoute = Screen.WarehouseDetail.createRoute("5")
        assertEquals("inventory/warehouse/5", warehouseRoute)

        val expenseRoute = Screen.ExpenseDetail.createRoute(123L)
        assertEquals("expense/detail/123", expenseRoute)
    }

    @Test
    fun `requiresAuth returns false for login and true for protected routes`() {
        // Local helper to determine if a route requires authentication
        fun requiresAuth(route: String) = route != Screen.Login.route

        assertEquals(false, requiresAuth(Screen.Login.route))
        assertEquals(true, requiresAuth(Screen.Dashboard.route))
        assertEquals(true, requiresAuth(Screen.Inventory.route))
    }
}

