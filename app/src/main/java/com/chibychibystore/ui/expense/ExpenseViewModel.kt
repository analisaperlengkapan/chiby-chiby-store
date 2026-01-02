package com.chibychibystore.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.service.ExpenseService
import com.chibychibystore.data.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
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

    // Filter States
    private val _startDate = MutableStateFlow<Date?>(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)) // Default 30 days
    private val _endDate = MutableStateFlow<Date?>(Date())
    private val _selectedCategory = MutableStateFlow<ExpenseCategory?>(null)
    private val _refreshTrigger = MutableStateFlow(0) // Trigger for manual reload

    // UI Feedback States
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Data Loading Pipeline
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    private val _expensesDataFlow = kotlinx.coroutines.flow.combine(
        _startDate,
        _endDate,
        _selectedCategory,
        _refreshTrigger
    ) { start, end, category, _ ->
        Triple(start, end, category)
    }.kotlinx.coroutines.flow.flatMapLatest { (start, end, category) ->
        _isLoading.value = true
        _error.value = null

        kotlinx.coroutines.flow.flow {
            try {
                // Convert to LocalDate for service
                val startLocalDate = start?.let { java.time.Instant.ofEpochMilli(it.time).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
                val endLocalDate = end?.let { java.time.Instant.ofEpochMilli(it.time).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }

                val result = expenseService.getExpenses(
                    startLocalDate,
                    endLocalDate,
                    category?.name
                )

                val list = when (result) {
                    is com.chibychibystore.data.model.Result.Success -> result.data
                    is com.chibychibystore.data.model.Result.Failure -> {
                        _error.value = result.exception.message
                        emptyList()
                    }
                }
                emit(list)
            } catch (e: Exception) {
                _error.value = e.message ?: "Terjadi kesalahan"
                emit(emptyList())
            } finally {
                _isLoading.value = false
            }
        }
    }

    val uiState: StateFlow<ExpenseUiState> = kotlinx.coroutines.flow.combine(
        _expensesDataFlow,
        _startDate,
        _endDate,
        _selectedCategory,
        _isLoading,
        _error
    ) { expenses, start, end, category, loading, errorMsg ->
        ExpenseUiState(
            expenses = expenses,
            startDate = start,
            endDate = end,
            selectedCategory = category,
            isLoading = loading,
            error = errorMsg,
            totalExpenses = expenses.sumOf { it.amount }
        )
    }.kotlinx.coroutines.flow.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState(isLoading = true)
    )

    /**
     * Set filter tanggal mulai
     */
    fun setStartDate(date: Date?) {
        _startDate.update { date }
    }

    /**
     * Set filter tanggal akhir
     */
    fun setEndDate(date: Date?) {
        _endDate.update { date }
    }

    /**
     * Set filter kategori
     */
    fun setCategoryFilter(category: ExpenseCategory?) {
        _selectedCategory.update { category }
    }

    /**
     * Set semua filter sekaligus
     */
    fun setFilters(startDate: Date?, endDate: Date?, category: ExpenseCategory?) {
        _startDate.update { startDate }
        _endDate.update { endDate }
        _selectedCategory.update { category }
    }

    /**
     * Hapus pengeluaran
     */
    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            _isLoading.update { true }
            try {
                // Updated to handle Result return type correctly
                val result = expenseService.deleteExpense(expenseId)
                if (result is com.chibychibystore.data.model.Result.Success) {
                     _refreshTrigger.update { it + 1 }
                } else if (result is com.chibychibystore.data.model.Result.Failure) {
                     _error.update { result.exception.message ?: "Gagal menghapus pengeluaran" }
                }
            } catch (e: Exception) {
                _error.update { e.message ?: "Gagal menghapus pengeluaran" }
            } finally {
                _isLoading.update { false }
            }
        }
    }

    /**
     * Reset filter
     */
    fun resetFilters() {
        _startDate.update { null }
        _endDate.update { null }
        _selectedCategory.update { null }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _error.update { null }
    }
}
