package com.chibychibystore.screenshots

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.AuthService
import com.chibychibystore.ui.auth.LoginUiState
import com.chibychibystore.ui.auth.LoginViewModel
import com.chibychibystore.ui.components.shared.LocalScreenViewModelFactory
import com.chibychibystore.ui.components.shared.ScreenTitleTestTag
import com.chibychibystore.ui.dashboard.DashboardUiState
import com.chibychibystore.ui.dashboard.DashboardViewModel
import com.chibychibystore.ui.navigation.AppNavigation
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.theme.ChibyChibyStoreTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The restored session decides where [AppNavigation] starts.
 *
 * This renders the **real** `AppNavigation` with a real `AuthService`, so it
 * fails if the composable stops deriving its start destination from the restored
 * user. A test that builds its own `NavHost` with a hard-coded start route cannot
 * see that wiring, which is exactly how the earlier regression slipped through:
 * a restored user was left on the login form with no way into the app.
 *
 * Only the start destination's ViewModel is resolved (the graph composes one
 * destination), so the factory below only needs the dashboard and login mocks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class AppNavigationRestoreTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var nav: TestNavHostController

    private fun <T> state(value: T): StateFlow<T> = MutableStateFlow(value)

    private inline fun <reified VM : Any> mockVm(configure: VM.() -> Unit = {}): VM =
        mock<VM>().also { it.configure() }

    private val viewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val vm: ViewModel = when (modelClass) {
                DashboardViewModel::class.java ->
                    mockVm<DashboardViewModel> { whenever(uiState).thenReturn(state(DashboardUiState())) }
                LoginViewModel::class.java ->
                    mockVm<LoginViewModel> { whenever(uiState).thenReturn(state(LoginUiState())) }
                else -> error("AppNavigationRestoreTest has no mock ViewModel for ${modelClass.name}")
            }
            @Suppress("UNCHECKED_CAST")
            return vm as T
        }
    }

    /** AuthService whose restore outcome the test controls. */
    private class FakeAuthService(
        private val restoredUser: Pengguna?,
        private val restoreResult: Result<Unit> = Result.success(Unit)
    ) : AuthService {
        private val user = MutableStateFlow<Pengguna?>(null)

        override suspend fun login(username: String, password: String): Result<Pengguna> =
            Result.failure(Exception("not used"))
        override suspend fun logout(): Result<Unit> {
            user.value = null
            return Result.success(Unit)
        }
        override suspend fun getCurrentUser(): Pengguna? = user.value
        override suspend fun hasPermission(permission: String): Boolean = user.value != null
        override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> =
            Result.success(Unit)
        override fun observeCurrentUser(): Flow<Pengguna?> = user
        override suspend fun initializeSession(): Result<Unit> {
            if (restoreResult.isSuccess) user.value = restoredUser
            return restoreResult
        }
        override suspend fun isSessionExpired(): Boolean = false
    }

    private fun render(authService: AuthService) {
        nav = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }
        composeTestRule.setContent {
            CompositionLocalProvider(LocalScreenViewModelFactory provides viewModelFactory) {
                ChibyChibyStoreTheme {
                    AppNavigation(navController = nav, authService = authService)
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun renderedTitle(): String? =
        composeTestRule.onNodeWithTag(ScreenTitleTestTag, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text }

    @Test
    fun `a restored session starts on the dashboard`() {
        render(FakeAuthService(restoredUser = SampleData.owner))

        assertEquals(Screen.Dashboard.route, nav.currentDestination?.route)
        assertEquals("Dasbor", renderedTitle())
    }

    @Test
    fun `no restored session starts on the login form`() {
        render(FakeAuthService(restoredUser = null))

        assertEquals(Screen.Login.route, nav.currentDestination?.route)
        composeTestRule.onNodeWithText("Selamat Datang!").assertExists()
    }

    @Test
    fun `a failed restore shows the retry screen`() {
        render(FakeAuthService(restoredUser = null, restoreResult = Result.failure(Exception("db down"))))

        composeTestRule.onNodeWithText("Gagal memuat sesi").assertExists()
        composeTestRule.onNodeWithText("Coba Lagi").assertExists()
    }
}
