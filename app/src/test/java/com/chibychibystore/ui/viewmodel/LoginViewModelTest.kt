package com.chibychibystore.ui.viewmodel
import org.robolectric.annotation.Config

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.chibychibystore.service.AuthService
import com.chibychibystore.ui.auth.LoginViewModel
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var authService: AuthService
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        authService = mock()
        viewModel = LoginViewModel(authService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be empty and not loading`() {
        val initialState = viewModel.uiState.value

        assertEquals("", initialState.username)
        assertEquals("", initialState.password)
        assertFalse(initialState.isLoading)
        assertNull(initialState.errorMessage)
        assertFalse(initialState.isLoginSuccessful)
    }

    @Test
    fun `onUsernameChange should update username and clear error`() {
        viewModel.onUsernameChange("testuser")

        val state = viewModel.uiState.value
        assertEquals("testuser", state.username)
        assertNull(state.errorMessage)
    }

    @Test
    fun `onPasswordChange should update password and clear error`() {
        viewModel.onPasswordChange("testpass")

        val state = viewModel.uiState.value
        assertEquals("testpass", state.password)
        assertNull(state.errorMessage)
    }

    @Test
    fun `login with empty username should show error`() {
        viewModel.onPasswordChange("password")
        viewModel.login()

        val state = viewModel.uiState.value
        assertEquals("Username dan password tidak boleh kosong", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `login with empty password should show error`() {
        viewModel.onUsernameChange("username")
        viewModel.login()

        val state = viewModel.uiState.value
        assertEquals("Username dan password tidak boleh kosong", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `successful login should update state correctly`() = runTest {
        val username = "testuser"
        val password = "testpass"

        val user = Pengguna(
            id = 1,
            username = username,
            passwordHash = "hash",
            role = Role.CASHIER,
            permissions = "[]"
        )

        whenever(authService.login(username, password)).thenReturn(com.chibychibystore.data.model.Result.success(user))

        viewModel.onUsernameChange(username)
        viewModel.onPasswordChange(password)
        viewModel.login()

        // Advance coroutine
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isLoginSuccessful)
        assertNull(state.errorMessage)
    }

    @Test
    fun `failed login should show error message`() = runTest {
        val username = "testuser"
        val password = "wrongpass"
        val errorMessage = "Invalid credentials"

        whenever(authService.login(username, password)).thenReturn(
            com.chibychibystore.data.model.Result.failure(Exception(errorMessage))
        )

        viewModel.onUsernameChange(username)
        viewModel.onPasswordChange(password)
        viewModel.login()

        // Advance coroutine
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isLoginSuccessful)
        assertEquals(errorMessage, state.errorMessage)
    }

    @Test
    fun `clearError should remove error message`() {
        viewModel.onUsernameChange("")
        viewModel.login() // This will set an error

        viewModel.clearError()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
    }

    @Test
    fun `resetLoginState should reset all state to initial`() {
        viewModel.onUsernameChange("user")
        viewModel.onPasswordChange("pass")
        viewModel.login() // This will set loading to true

        viewModel.resetLoginState()

        val state = viewModel.uiState.value
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isLoginSuccessful)
    }
}