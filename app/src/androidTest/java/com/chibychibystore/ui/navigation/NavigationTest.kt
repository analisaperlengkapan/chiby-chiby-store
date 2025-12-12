package com.chibychibystore.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.service.AuthService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val composeTestRule = createComposeRule()

    @Mock
    private lateinit var mockAuthService: AuthService

    private val testUser = Pengguna(
        id = "test-id",
        username = "testuser",
        nama = "Test User",
        role = Role.CASHIER,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        hiltRule.inject()
    }

    @Test
    fun `requiresAuth returns true for protected routes`() {
        // Test that dashboard requires auth
        assertTrue(requiresAuth(Routes.DASHBOARD))

        // Test that inventory requires auth
        assertTrue(requiresAuth(Routes.INVENTORY))

        // Test that sales requires auth
        assertTrue(requiresAuth(Routes.SALES))
    }

    @Test
    fun `requiresAuth returns false for public routes`() {
        // Test that login does not require auth
        assert(!requiresAuth(Routes.LOGIN))
    }

    @Test
    fun `AuthGuard shows login when user not authenticated`() {
        // Mock unauthenticated state
        `when`(mockAuthService.observeCurrentUser()).thenReturn(flowOf(null))

        composeTestRule.setContent {
            AuthGuard(
                authService = mockAuthService,
                onLoginSuccess = {}
            ) {
                androidx.compose.material3.Text("Protected Content")
            }
        }

        // Verify login screen is shown
        composeTestRule.onNodeWithText("Login").assertExists()
    }

    @Test
    fun `AuthGuard shows protected content when user authenticated`() {
        // Mock authenticated state
        `when`(mockAuthService.observeCurrentUser()).thenReturn(flowOf(testUser))

        composeTestRule.setContent {
            AuthGuard(
                authService = mockAuthService,
                onLoginSuccess = {}
            ) {
                androidx.compose.material3.Text("Protected Content")
            }
        }

        // Verify protected content is shown
        composeTestRule.onNodeWithText("Protected Content").assertExists()
    }
}