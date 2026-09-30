package com.chibychibystore.ui.navigation

import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.repository.PenggunaSessionRepository
import com.chibychibystore.service.impl.AuthServiceImpl
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.security.MessageDigest

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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `observeCurrentUser reports a session that starts and ends`() = runTest {
        // The app shell gates the navigation drawer on this flow, so it must
        // emit the login and logout transitions rather than a one-shot value;
        // otherwise the drawer never appears after signing in.
        val penggunaDao = mock<PenggunaDao>()
        val sessionRepository = mock<PenggunaSessionRepository>()
        val service = AuthServiceImpl(penggunaDao, sessionRepository)
        val user = Pengguna(
            id = 1,
            username = "testuser",
            passwordHash = hashPassword("password123"),
            role = Role.CASHIER
        )
        whenever(penggunaDao.getPenggunaByUsername("testuser")).thenReturn(user)

        val observed = mutableListOf<Pengguna?>()
        val job = launch { service.observeCurrentUser().collect { observed.add(it) } }
        runCurrent()

        assertTrue(service.login("testuser", "password123").isSuccess)
        runCurrent()
        assertTrue(service.logout().isSuccess)
        runCurrent()
        job.cancel()

        assertEquals(listOf(null, user, null), observed)
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}

