package com.chibychibystore.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.BackupService
import com.chibychibystore.ui.settings.SettingsViewModel
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
        MockitoAnnotations.openMocks(this)

        runTest {
            `when`(mockAuthService.getCurrentUser()).thenReturn(testUser)
        }

        viewModel = SettingsViewModel(mockAuthService, mockBackupService)
    }

    @Test
    fun `initial state loads current user successfully`() = runTest {
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals(testUser, uiState.currentUser)
        assertNull(uiState.error)
    }

    @Test
    fun `changePassword success updates state correctly`() = runTest {
        `when`(mockAuthService.changePassword("oldPass", "newPass"))
            .thenReturn(Result.success(Unit))

        viewModel.changePassword("oldPass", "newPass")

        val uiState = viewModel.uiState.value
        assertTrue(uiState.passwordChangeSuccess)
        assertNull(uiState.error)
    }

    @Test
    fun `logout calls authService and triggers callback`() = runTest {
        var logoutCalled = false
        val onLogoutSuccess = { logoutCalled = true }

        viewModel.logout(onLogoutSuccess)

        assertTrue(logoutCalled)
        verify(mockAuthService).logout()
    }
}
