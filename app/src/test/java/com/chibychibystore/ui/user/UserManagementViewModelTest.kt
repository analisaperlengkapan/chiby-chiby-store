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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
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

        // Default behavior for getAllUsers called in init
        `when`(userManagementService.getAllUsers()).thenReturn(flowOf(emptyList()))

        viewModel = UserManagementViewModel(userManagementService, authService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createUser success updates state`() = runTest {
        // Arrange
        val username = "testuser"
        val password = "password123"
        val role = Role.CASHIER
        val currentUser = Pengguna(
            id = 1L,
            username = "admin",
            passwordHash = "hash",
            role = Role.OWNER,
            permissions = "[]",
            createdAt = java.util.Date(),
            updatedAt = java.util.Date()
        )
        val createdUserId = 2L

        `when`(authService.getCurrentUser()).thenReturn(currentUser)
        `when`(userManagementService.createUser(username, password, role, currentUser.id))
            .thenReturn(Result.success(createdUserId))

        // Act
        val result = viewModel.createUser(username, password, role)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(createdUserId, result.getOrNull())
        verify(userManagementService).createUser(username, password, role, currentUser.id)
        verify(userManagementService, org.mockito.Mockito.times(2)).getAllUsers() // Once in init, once after create
    }

    @Test
    fun `createUser failure updates error state`() = runTest {
        // Arrange
        val username = "testuser"
        val password = "password123"
        val role = Role.CASHIER
        val currentUser = Pengguna(
            id = 1L,
            username = "admin",
            passwordHash = "hash",
            role = Role.OWNER,
            permissions = "[]",
            createdAt = java.util.Date(),
            updatedAt = java.util.Date()
        )
        val errorMessage = "Username already exists"

        `when`(authService.getCurrentUser()).thenReturn(currentUser)
        `when`(userManagementService.createUser(username, password, role, currentUser.id))
            .thenReturn(Result.failure(Exception(errorMessage)))

        // Act
        val result = viewModel.createUser(username, password, role)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(errorMessage, viewModel.uiState.value.error)
    }
}
