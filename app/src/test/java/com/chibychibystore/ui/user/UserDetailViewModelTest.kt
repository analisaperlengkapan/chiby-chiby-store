package com.chibychibystore.ui.user

import androidx.lifecycle.SavedStateHandle
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.UserManagementService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.Date
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class UserDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var userManagementService: UserManagementService

    @Mock
    private lateinit var authService: AuthService

    private lateinit var viewModel: UserDetailViewModel
    private val userId = 1L
    private val currentUserId = 99L
    private val currentUser = Pengguna(
        id = currentUserId,
        username = "admin",
        passwordHash = "hash",
        role = Role.OWNER,
        permissions = "[]",
        createdAt = Date(),
        updatedAt = Date()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        val savedStateHandle = SavedStateHandle(mapOf("userId" to userId))
        viewModel = UserDetailViewModel(userManagementService, authService, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadUser success updates state with user`() = runTest {
        val user = Pengguna(
            id = userId,
            username = "testuser",
            passwordHash = "hash",
            role = Role.CASHIER,
            permissions = "[]",
            createdAt = Date(),
            updatedAt = Date()
        )

        Mockito.`when`(userManagementService.getUserById(userId))
            .thenReturn(Result.success(user))

        viewModel.loadUser(userId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(user, state.user)
        assertEquals(null, state.error)
    }

    @Test
    fun `loadUser failure updates state with error`() = runTest {
        val errorMessage = "Network error"
        Mockito.`when`(userManagementService.getUserById(userId))
            .thenReturn(Result.failure(Exception(errorMessage)))

        viewModel.loadUser(userId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(null, state.user)
        assertEquals(errorMessage, state.error)
    }

    @Test
    fun `saveUser success calls updateUser`() = runTest {
        val user = Pengguna(
            id = userId,
            username = "testuser",
            passwordHash = "hash",
            role = Role.CASHIER,
            permissions = "[]",
            createdAt = Date(),
            updatedAt = Date()
        )

        Mockito.`when`(userManagementService.getUserById(userId)).thenReturn(Result.success(user))
        Mockito.`when`(authService.getCurrentUser()).thenReturn(currentUser)
        Mockito.`when`(userManagementService.updateUser(any(), anyOrNull(), anyOrNull(), anyOrNull(), any())).thenReturn(Result.success(Unit))

        viewModel.loadUser(userId)
        advanceUntilIdle()

        viewModel.saveUser()
        advanceUntilIdle()

        verify(userManagementService).updateUser(
            eq(userId),
            eq(user.username),
            eq(user.role),
            eq(null),
            eq(currentUserId)
        )
        assertFalse(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun `deleteUser success calls deleteUser`() = runTest {
        Mockito.`when`(authService.getCurrentUser()).thenReturn(currentUser)
        Mockito.`when`(userManagementService.deleteUser(any(), any())).thenReturn(Result.success(Unit))

        var onSuccessCalled = false
        viewModel.deleteUser { onSuccessCalled = true }
        advanceUntilIdle()

        verify(userManagementService).deleteUser(eq(userId), eq(currentUserId))
        assertTrue(onSuccessCalled)
    }
}
