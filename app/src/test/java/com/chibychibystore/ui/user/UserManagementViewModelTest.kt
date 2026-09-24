package com.chibychibystore.ui.user


import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.UserManagementService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class UserManagementViewModelTest {

    @Mock
    private lateinit var userManagementService: UserManagementService

    @Mock
    private lateinit var authService: AuthService

    private lateinit var viewModel: UserManagementViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() = runBlocking {
        MockitoAnnotations.openMocks(this@UserManagementViewModelTest)
        Dispatchers.setMain(testDispatcher)

        // Default behavior for getAllUsers called in init
        whenever(userManagementService.getAllUsers()).thenReturn(flowOf(emptyList()))
        whenever(userManagementService.getUserStats()).thenReturn(com.chibychibystore.data.model.Result.success(com.chibychibystore.service.UserStats(0,0,0,0,0,0)))
        whenever(userManagementService.canDeleteLastOwner()).thenReturn(flowOf(true))

        viewModel = UserManagementViewModel(userManagementService, authService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createUser success updates state`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
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

        whenever(authService.getCurrentUser()).thenReturn(currentUser)
        whenever(userManagementService.createUser(username, password, role, currentUser.id))
            .thenReturn(Result.success(createdUserId))

        // Pre-fill form state
        viewModel.onCreateUserUsernameChange(username)
        viewModel.onCreateUserPasswordChange(password)
        viewModel.onCreateUserConfirmPasswordChange(password)
        viewModel.onCreateUserRoleChange(role)

        // Act
        viewModel.createUser()
        advanceTimeBy(1000)

        // Assert
        val state = viewModel.uiState.value
        assertEquals("User berhasil dibuat", state.successMessage)
        verify(userManagementService).createUser(username, password, role, currentUser.id)
        verify(userManagementService, times(1)).getAllUsers() // Once in init
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

        whenever(authService.getCurrentUser()).thenReturn(currentUser)
        whenever(userManagementService.createUser(username, password, role, currentUser.id))
            .thenReturn(Result.failure(Exception(errorMessage)))

        // Pre-fill form state
        viewModel.onCreateUserUsernameChange(username)
        viewModel.onCreateUserPasswordChange(password)
        viewModel.onCreateUserConfirmPasswordChange(password)
        viewModel.onCreateUserRoleChange(role)

        // Act
        viewModel.createUser()
        advanceUntilIdle()

        // Assert
        val formState = viewModel.createUserFormState.value
        assertEquals(errorMessage, formState.errorMessage)
    }
}
