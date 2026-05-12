package com.chibychibystore.ui.expense
import org.robolectric.annotation.Config

import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.ExpenseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.anyOrNull
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

    @Mock
    private lateinit var expenseService: ExpenseService

    private lateinit var viewModel: ExpenseViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock successful response
        kotlinx.coroutines.runBlocking {
            `when`(expenseService.getPengeluarans(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(Result.success(emptyList()))
        }

        viewModel = ExpenseViewModel(expenseService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setDateRange and setCategory updates uiState and reloads expenses`() = runTest {
        val startDate = Date()
        val endDate = Date()
        val category = KategoriPengeluaran.OPERATING_EXPENSE_CATEGORIES.first()

        viewModel.setDateRange(startDate, endDate)
        viewModel.setCategory(category)

        val state = viewModel.uiState.first()
        assertEquals(startDate, state.startDate)
        assertEquals(endDate, state.endDate)
        assertEquals(category, state.selectedCategory)
    }

    @Test
    fun `manual reset clears filters`() = runTest {
        // Set some filters first
        val startDate = Date()
        val category = KategoriPengeluaran.OPERATING_EXPENSE_CATEGORIES.first()
        viewModel.setDateRange(startDate, startDate)
        viewModel.setCategory(category)

        // Reset manually
        viewModel.setCategory(null)
        viewModel.setDateRange(null, null)

        val state = viewModel.uiState.first()
        assertEquals(null, state.startDate)
        assertEquals(null, state.endDate)
        assertEquals(null, state.selectedCategory)
    }

    @Test
    fun `setCategory updates only category`() = runTest {
         val category = KategoriPengeluaran.INVENTORY_PURCHASES
         viewModel.setCategory(category)

         val state = viewModel.uiState.first()
         assertEquals(category, state.selectedCategory)
    }
}
