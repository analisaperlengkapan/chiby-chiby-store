package com.chibychibystore.ui.viewmodel
import org.robolectric.annotation.Config

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.service.AuthService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Config(manifest = Config.NONE)
@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockAuthService: AuthService

    private lateinit var viewModel: SettingsViewModel

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

        // Mock the auth service to return the test user
        `when`(mockAuthService.observeCurrentUser()).thenReturn(flowOf(testUser))
        `when`(mockAuthService.getCurrentUser()).thenReturn(testUser)

        viewModel = SettingsViewModel(mockAuthService)
    }

    @Test
    fun `initial state loads current user successfully`() = runTest {
        // Wait for the ViewModel to initialize
        kotlinx.coroutines.delay(100)

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(testUser, uiState.currentUser)
        assertNull(uiState.error)
    }

    @Test
    fun `changePassword success updates state correctly`() = runTest {
        // Mock successful password change
        `when`(mockAuthService.changePassword("oldPass", "newPass"))
            .thenReturn(Result.success(Unit))

        viewModel.changePassword("oldPass", "newPass")

        // Wait for the operation to complete
        kotlinx.coroutines.delay(100)

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isChangingPassword)
        assertTrue(uiState.passwordChangeSuccess)
        assertNull(uiState.error)
    }

    @Test
    fun `changePassword failure updates state with error`() = runTest {
        val errorMessage = "Invalid password"
        `when`(mockAuthService.changePassword("wrongOld", "newPass"))
            .thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.changePassword("wrongOld", "newPass")

        kotlinx.coroutines.delay(100)

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isChangingPassword)
        assertFalse(uiState.passwordChangeSuccess)
        assertEquals(errorMessage, uiState.error)
    }

    @Test
    fun `logout calls authService and triggers callback`() = runTest {
        var logoutCalled = false
        val onLogoutSuccess = { logoutCalled = true }

        `when`(mockAuthService.logout()).thenReturn(Result.success(Unit))

        viewModel.logout(onLogoutSuccess)

        kotlinx.coroutines.delay(100)

        assertTrue(logoutCalled)
        verify(mockAuthService).logout()
    }

    @Test
    fun `clearError removes error from state`() = runTest {
        // First set an error
        viewModel.changePassword("old", "new") // This will fail in test
        kotlinx.coroutines.delay(100)

        // Clear the error
        viewModel.clearError()

        val uiState = viewModel.uiState.value
        assertNull(uiState.error)
    }

    @Test
    fun `clearPasswordChangeSuccess resets success flag`() = runTest {
        // Mock successful password change first
        `when`(mockAuthService.changePassword("old", "new"))
            .thenReturn(Result.success(Unit))

        viewModel.changePassword("old", "new")
        kotlinx.coroutines.delay(100)

        // Clear success flag
        viewModel.clearPasswordChangeSuccess()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.passwordChangeSuccess)
    }

    @Test
    fun `createBackup shows not implemented error`() = runTest {
        viewModel.createBackup()

        val uiState = viewModel.uiState.value
        assertEquals("Fitur backup belum diimplementasi", uiState.error)
    }
}