package com.chibychibystore.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.BackupService
import com.chibychibystore.ui.settings.SettingsViewModel
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockAuthService: AuthService

    @Mock
    private lateinit var mockBackupService: BackupService

    private lateinit var viewModel: SettingsViewModel

    private val testUser = Pengguna(
        id = 1L,
        username = "testuser",
        passwordHash = "hash",
        role = Role.CASHIER,
        createdAt = Date(),
        updatedAt = Date()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        runTest(testDispatcher) {
            `when`(mockAuthService.getCurrentUser()).thenReturn(testUser)
        }

        viewModel = SettingsViewModel(mockAuthService, mockBackupService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads current user successfully`() = runTest(testDispatcher) {
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(testUser, uiState.currentUser)
        assertNull(uiState.error)
    }

    @Test
    fun `changePassword success updates state correctly`() = runTest(testDispatcher) {
        `when`(mockAuthService.changePassword("oldPass", "newPass"))
            .thenReturn(com.chibychibystore.data.model.Result.success(Unit))

        viewModel.changePassword("oldPass", "newPass")
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isChangingPassword)
        assertTrue(uiState.passwordChangeSuccess)
        assertNull(uiState.error)
    }

    @Test
    fun `changePassword failure updates state with error`() = runTest(testDispatcher) {
        val errorMessage = "Invalid password"
        `when`(mockAuthService.changePassword("wrongOld", "newPass"))
            .thenReturn(com.chibychibystore.data.model.Result.failure(Exception(errorMessage)))

        viewModel.changePassword("wrongOld", "newPass")
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.isChangingPassword)
        assertFalse(uiState.passwordChangeSuccess)
        assertEquals(errorMessage, uiState.error)
    }

    @Test
    fun `logout calls authService and triggers callback`() = runTest(testDispatcher) {
        var logoutCalled = false
        val onLogoutSuccess = { logoutCalled = true }

        `when`(mockAuthService.logout()).thenReturn(com.chibychibystore.data.model.Result.success(Unit))

        viewModel.logout(onLogoutSuccess)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(logoutCalled)
        verify(mockAuthService).logout()
    }

    @Test
    fun `clearError removes error from state`() = runTest(testDispatcher) {
        viewModel.changePassword("old", "new")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearError()

        val uiState = viewModel.uiState.value
        assertNull(uiState.error)
    }

    @Test
    fun `clearPasswordChangeSuccess resets success flag`() = runTest(testDispatcher) {
        `when`(mockAuthService.changePassword("old", "new"))
            .thenReturn(com.chibychibystore.data.model.Result.success(Unit))

        viewModel.changePassword("old", "new")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearPasswordChangeSuccess()

        val uiState = viewModel.uiState.value
        assertFalse(uiState.passwordChangeSuccess)
    }
}
