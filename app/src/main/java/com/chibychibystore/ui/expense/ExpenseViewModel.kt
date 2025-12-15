package com.chibychibystore.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.service.ExpenseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*
import javax.inject.Inject

/**
 * UI State untuk Expense Screen
 */
data class ExpenseUiState(
    val expenses: List<Pengeluaran> = emptyList(),
    val startDate: Date? = null,
    val endDate: Date? = null,
    val selectedCategory: ExpenseCategory? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalExpenses: Double = 0.0
)

/**
 * ViewModel untuk Expense Management
 * Mengelola daftar pengeluaran dengan filter tanggal dan kategori
 */
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseService: ExpenseService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState

    init {
        loadExpenses()
    }

    /**
     * Load pengeluaran dengan filter
     */
    fun loadExpenses() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val startDate = _uiState.value.startDate ?: Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000) // 30 hari lalu
                val endDate = _uiState.value.endDate ?: Date()

                val result = expenseService.getExpensesInDateRange(startDate, endDate)

                val filteredExpensesList = result // Result is List<Pengeluaran> directly

                var filteredExpenses = filteredExpensesList

                // Filter by category if selected
                _uiState.value.selectedCategory?.let { category ->
                    filteredExpenses = filteredExpenses.filter { it.category == category }
                }

                // Calculate total
                val total = filteredExpenses.sumOf { it.amount }

                _uiState.value = _uiState.value.copy(
                    expenses = filteredExpenses,
                    totalExpenses = total,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Terjadi kesalahan",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Set filter tanggal mulai
     */
    fun setStartDate(date: Date?) {
        _uiState.value = _uiState.value.copy(startDate = date)
        loadExpenses()
    }

    /**
     * Set filter tanggal akhir
     */
    fun setEndDate(date: Date?) {
        _uiState.value = _uiState.value.copy(endDate = date)
        loadExpenses()
    }

    /**
     * Set filter kategori
     */
    fun setCategoryFilter(category: ExpenseCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadExpenses()
    }

    /**
     * Hapus pengeluaran
     */
    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Note: Delete functionality would be implemented in ExpenseService
                // For now, just reload the list
                loadExpenses()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Gagal menghapus pengeluaran",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Reset filter
     */
    fun resetFilters() {
        _uiState.value = _uiState.value.copy(
            startDate = null,
            endDate = null,
            selectedCategory = null
        )
        loadExpenses()
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}