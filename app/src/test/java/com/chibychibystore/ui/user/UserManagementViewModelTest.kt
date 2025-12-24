package com.chibychibystore.ui.user

import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.UserManagementService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class UserManagementViewModelTest {

    @Mock
    private lateinit var userManagementService: UserManagementService

    @Mock
    private lateinit var authService: AuthService

    private lateinit var viewModel: UserManagementViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock default behavior for getAllUsers to avoid initialization issues
        `when`(userManagementService.getAllUsers()).thenReturn(flowOf(emptyList()))

        viewModel = UserManagementViewModel(userManagementService, authService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createUser success`() = runTest {
        // Given
        val username = "newuser"
        val password = "password"
        val role = Role.CASHIER
        val currentUser = Pengguna(id = 1L, username = "admin", passwordHash = "hash", role = Role.OWNER, permissions = "[]", createdAt = java.util.Date(), updatedAt = java.util.Date())

        `when`(authService.getCurrentUser()).thenReturn(currentUser)
        `when`(userManagementService.createUser(eq(username), eq(password), eq(role), eq(currentUser.id))).thenReturn(Result.success(2L))
        `when`(userManagementService.getAllUsers()).thenReturn(flowOf(emptyList()))

        // When
        val result = viewModel.createUser(username, password, role)

        // Then
        assertTrue(result.isSuccess)
        verify(userManagementService).createUser(username, password, role, currentUser.id)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `createUser failure`() = runTest {
        // Given
        val username = "newuser"
        val password = "password"
        val role = Role.CASHIER
        val currentUser = Pengguna(id = 1L, username = "admin", passwordHash = "hash", role = Role.OWNER, permissions = "[]", createdAt = java.util.Date(), updatedAt = java.util.Date())
        val errorMessage = "Failed to create user"

        `when`(authService.getCurrentUser()).thenReturn(currentUser)
        `when`(userManagementService.createUser(eq(username), eq(password), eq(role), eq(currentUser.id))).thenReturn(Result.failure(Exception(errorMessage)))

        // When
        val result = viewModel.createUser(username, password, role)

        // Then
        assertTrue(result.isFailure)
        assertEquals(errorMessage, result.exceptionOrNull()?.message)
        assertEquals(errorMessage, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
