package com.chibychibystore.service
import org.robolectric.annotation.Config

import com.chibychibystore.data.model.Result
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.repository.PengeluaranRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.LocalDate
import java.util.*

@ExperimentalCoroutinesApi
class ExpenseServiceTest {

    @Mock
    private lateinit var pengeluaranRepository: PengeluaranRepository

    @Mock
    private lateinit var authService: AuthService

    private lateinit var expenseService: ExpenseService

    private val testUser = com.chibychibystore.data.local.entity.Pengguna(
        id = 1,
        username = "testuser",
        passwordHash = "hash",
        role = "OWNER",
        permissions = "[]",
        createdAt = Date(),
        updatedAt = Date()
    )

    private val testExpense = Pengeluaran(
        id = 1,
        expenseDate = Date(),
        category = ExpenseCategory.UTILITIES,
        amount = 50000.0,
        description = "Test expense",
        approvedBy = 1,
        createdBy = 1,
        createdAt = Date()
    )

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        expenseService = ExpenseService(pengeluaranRepository, authService)
    }

    @Test
    fun `createExpense should return success when valid data and user authenticated`() = runTest {
        // Given
        `when`(authService.getCurrentUser()).thenReturn(testUser)
        `when`(authService.hasPermission(anyString())).thenReturn(true)
        `when`(pengeluaranRepository.createPengeluaran(any())).thenReturn(Result.Success(1L))

        // When
        val result = expenseService.createExpense(
            expenseDate = Date(),
            category = ExpenseCategory.UTILITIES,
            amount = 50000.0,
            description = "Test expense"
        )

        // Then
        assertTrue(result is Result.Success)
        assertEquals(1L, (result as Result.Success).data)
        verify(pengeluaranRepository).createPengeluaran(any())
    }

    @Test
    fun `createExpense should return error when user not authenticated`() = runTest {
        // Given
        `when`(authService.getCurrentUser()).thenReturn(null)

        // When
        val result = expenseService.createExpense(
            expenseDate = Date(),
            category = ExpenseCategory.UTILITIES,
            amount = 50000.0
        )

        // Then
        assertTrue(result is Result.Failure)
        assertEquals("User tidak terautentikasi", (result as Result.Failure).message)
    }

    @Test
    fun `createExpense should return error when amount is zero or negative`() = runTest {
        // Given
        `when`(authService.getCurrentUser()).thenReturn(testUser)

        // When
        val result = expenseService.createExpense(
            expenseDate = Date(),
            category = ExpenseCategory.UTILITIES,
            amount = 0.0
        )

        // Then
        assertTrue(result is Result.Failure)
        assertEquals("Jumlah pengeluaran harus lebih dari 0", (result as Result.Failure).message)
    }

    @Test
    fun `updateExpense should return success when valid data and permissions`() = runTest {
        // Given
        `when`(authService.getCurrentUser()).thenReturn(testUser)
        `when`(authService.hasPermission("EDIT_EXPENSE")).thenReturn(true)
        `when`(pengeluaranRepository.getPengeluaranById(1)).thenReturn(Result.Success(testExpense))
        `when`(pengeluaranRepository.updatePengeluaran(any())).thenReturn(Result.Success(Unit))

        // When
        val result = expenseService.updateExpense(
            id = 1,
            expenseDate = Date(),
            category = ExpenseCategory.UTILITIES,
            amount = 60000.0,
            description = "Updated expense"
        )

        // Then
        assertTrue(result is Result.Success)
        verify(pengeluaranRepository).updatePengeluaran(any())
    }

    @Test
    fun `updateExpense should return error when no edit permission`() = runTest {
        // Given
        `when`(authService.getCurrentUser()).thenReturn(testUser)
        `when`(authService.hasPermission("EDIT_EXPENSE")).thenReturn(false)

        // When
        val result = expenseService.updateExpense(
            id = 1,
            expenseDate = Date(),
            category = ExpenseCategory.UTILITIES,
            amount = 60000.0
        )

        // Then
        assertTrue(result is Result.Failure)
        assertEquals("Tidak memiliki izin untuk mengedit pengeluaran", (result as Result.Failure).message)
    }

    @Test
    fun `approveExpense should return success when valid permission`() = runTest {
        // Given
        `when`(authService.getCurrentUser()).thenReturn(testUser)
        `when`(authService.hasPermission("APPROVE_EXPENSE")).thenReturn(true)
        `when`(pengeluaranRepository.approvePengeluaran(1, 1)).thenReturn(Result.Success(Unit))

        // When
        val result = expenseService.approveExpense(1)

        // Then
        assertTrue(result is Result.Success)
        verify(pengeluaranRepository).approvePengeluaran(1, 1)
    }

    @Test
    fun `deleteExpense should return success when valid permission`() = runTest {
        // Given
        `when`(authService.getCurrentUser()).thenReturn(testUser)
        `when`(authService.hasPermission("DELETE_EXPENSE")).thenReturn(true)
        `when`(pengeluaranRepository.deletePengeluaran(1)).thenReturn(Result.Success(Unit))

        // When
        val result = expenseService.deleteExpense(1)

        // Then
        assertTrue(result is Result.Success)
        verify(pengeluaranRepository).deletePengeluaran(1)
    }

    @Test
    fun `getAllPengeluaran should return flow of expenses`() = runTest {
        // Given
        val expenses = listOf(testExpense)
        `when`(pengeluaranRepository.getAllPengeluaran()).thenReturn(flowOf(expenses))

        // When
        val result = expenseService.getAllPengeluaran().first()

        // Then
        assertEquals(expenses, result)
    }

    @Test
    fun `getExpenseCategories should return all categories`() {
        // When
        val categories = expenseService.getExpenseCategories()

        // Then
        assertTrue(categories.contains(ExpenseCategory.UTILITIES))
        assertTrue(categories.contains(ExpenseCategory.SUPPLIES))
    }

    @Test
    fun `getExpenseCategoryByDisplayName should return correct category`() {
        // When
        val category = expenseService.getExpenseCategoryByDisplayName("Utilitas")

        // Then
        assertEquals(ExpenseCategory.UTILITIES, category)
    }
}