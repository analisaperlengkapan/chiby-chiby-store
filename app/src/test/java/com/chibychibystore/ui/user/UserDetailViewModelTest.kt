package com.chibychibystore.ui.user

import androidx.lifecycle.SavedStateHandle
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.model.Result
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

@OptIn(ExperimentalCoroutinesApi::class)
class UserDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var userManagementService: UserManagementService

    private lateinit var viewModel: UserDetailViewModel
    private val userId = 1L

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        val savedStateHandle = SavedStateHandle(mapOf("userId" to userId))
        viewModel = UserDetailViewModel(userManagementService, savedStateHandle)
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
}
