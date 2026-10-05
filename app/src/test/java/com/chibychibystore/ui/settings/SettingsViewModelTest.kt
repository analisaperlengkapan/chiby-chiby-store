package com.chibychibystore.ui.settings

import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.BackupService
import com.chibychibystore.service.BackupInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authService: AuthService
    private lateinit var backupService: BackupService

    private val testUser = Pengguna(id = 1, username = "testuser", passwordHash = "x", role = Role.CASHIER)

    @Before
    fun setup() = runBlocking<Unit> {
        Dispatchers.setMain(testDispatcher)
        authService = mock()
        backupService = mock()
        whenever(authService.getCurrentUser()).thenReturn(testUser)
        whenever(authService.logout()).thenReturn(Result.success(Unit))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads the current user`() = runTest {
        val viewModel = SettingsViewModel(authService, backupService)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(testUser, state.currentUser)
        assertNull(state.error)
    }

    @Test
    fun `changePassword success sets the success flag`() = runTest {
        whenever(authService.changePassword("oldPass", "newPass")).thenReturn(Result.success(Unit))

        val viewModel = SettingsViewModel(authService, backupService)
        advanceUntilIdle()

        viewModel.changePassword("oldPass", "newPass")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isChangingPassword)
        assertTrue(state.passwordChangeSuccess)
        assertNull(state.error)
    }

    @Test
    fun `changePassword failure records the error`() = runTest {
        whenever(authService.changePassword("wrongOld", "newPass"))
            .thenReturn(Result.failure(Exception("Invalid password")))

        val viewModel = SettingsViewModel(authService, backupService)
        advanceUntilIdle()

        viewModel.changePassword("wrongOld", "newPass")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isChangingPassword)
        assertFalse(state.passwordChangeSuccess)
        assertEquals("Invalid password", state.error)
    }

    @Test
    fun `logout delegates to the auth service and invokes the callback`() = runTest {
        var logoutCalled = false

        val viewModel = SettingsViewModel(authService, backupService)
        advanceUntilIdle()

        viewModel.logout { logoutCalled = true }
        advanceUntilIdle()

        assertTrue(logoutCalled)
        verify(authService).logout()
    }

    @Test
    fun `clearError removes the recorded error`() = runTest {
        whenever(authService.changePassword("old", "new")).thenReturn(Result.failure(Exception("boom")))

        val viewModel = SettingsViewModel(authService, backupService)
        advanceUntilIdle()

        viewModel.changePassword("old", "new")
        advanceUntilIdle()
        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `clearPasswordChangeSuccess resets the success flag`() = runTest {
        whenever(authService.changePassword("oldPass", "newPass")).thenReturn(Result.success(Unit))

        val viewModel = SettingsViewModel(authService, backupService)
        advanceUntilIdle()

        viewModel.changePassword("oldPass", "newPass")
        advanceUntilIdle()
        viewModel.clearPasswordChangeSuccess()

        assertFalse(viewModel.uiState.value.passwordChangeSuccess)
    }

    @Test
    fun `createBackup stores the backup timestamp`() = runTest {
        val info = BackupInfo(id = "1", fileName = "b.enc", filePath = "/x", createdAt = 123L, sizeBytes = 10, checksum = "abc")
        whenever(backupService.createBackup()).thenReturn(Result.success(info))

        val viewModel = SettingsViewModel(authService, backupService)
        advanceUntilIdle()

        viewModel.createBackup()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isCreatingBackup)
        assertEquals(123L, state.lastBackupDate)
        assertNull(state.error)
    }
}
