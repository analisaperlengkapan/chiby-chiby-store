package com.chibychibystore.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class DashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DashboardViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have loading true initially`() {
        val initialState = viewModel.uiState.value
        assertTrue(initialState.isLoading)
    }

    @Test
    fun `refreshData should trigger data loading`() = runTest {
        val initialState = viewModel.uiState.value

        viewModel.refreshData()

        // Advance coroutine to complete loading
        testDispatcher.scheduler.advanceUntilIdle()

        val newState = viewModel.uiState.value

        // Should have loaded some data
        assertFalse(newState.isLoading)
        assertTrue(newState.todaySales >= 0.0)
        assertTrue(newState.todayTransactionCount >= 0)
        assertTrue(newState.lowStockItems.isNotEmpty() || newState.recentTransactions.isNotEmpty())
    }

    @Test
    fun `dashboard should generate mock low stock items`() = runTest {
        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        // Should have some low stock items
        assertTrue(state.lowStockItems.isNotEmpty())
        state.lowStockItems.forEach { item ->
            assertTrue(item.stockQuantity <= 10) // Low stock threshold
            assertTrue(item.name.isNotEmpty())
            assertTrue(!item.barcode.isNullOrBlank())
        }
    }

    @Test
    fun `dashboard should generate mock recent transactions`() = runTest {
        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        // Should have some recent transactions
        assertTrue(state.recentTransactions.isNotEmpty())
        state.recentTransactions.forEach { transaction ->
            assertTrue(transaction.totalAmount > 0.0)
            assertTrue(transaction.saleDate.time > 0)
            assertTrue(transaction.cashierId > 0)
        }
    }

    @Test
    fun `today sales should be within reasonable range`() = runTest {
        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        // Mock data should be between 1M and 5M
        assertTrue(state.todaySales >= 1000000.0)
        assertTrue(state.todaySales <= 5000000.0)
    }

    @Test
    fun `transaction count should be within reasonable range`() = runTest {
        viewModel.refreshData()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value

        // Mock data should be between 10 and 50
        assertTrue(state.todayTransactionCount >= 10)
        assertTrue(state.todayTransactionCount <= 50)
    }
}