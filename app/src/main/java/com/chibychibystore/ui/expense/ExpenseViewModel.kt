package com.chibychibystore.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.data.local.entity.Expense
import com.chibychibystore.service.ExpenseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

/**
 * UI State untuk Expense Screen
 */
data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val startDate: Date? = null,
    val endDate: Date? = null,
    val selectedCategory: ExpenseCategory? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalExpenses: Double = 0.0
)

/**
 * ViewModel untuk Expense Management
 * Mengelola daftar expense dengan filter tanggal dan kategori
 */
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseService: ExpenseService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState(isLoading = true))
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    init {
        loadExpenses()
    }

    /**
     * Load expenses based on current filters
     */
    fun loadExpenses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val currentState = _uiState.value
                
                // Convert Date to LocalDate for service
                val startLocalDate: LocalDate? = currentState.startDate?.let { 
                    Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).toLocalDate() 
                }
                val endLocalDate: LocalDate? = currentState.endDate?.let { 
                    Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).toLocalDate() 
                }

                val result = expenseService.getExpenses(
                    startLocalDate,
                    endLocalDate,
                    currentState.selectedCategory?.name
                )

                when (result) {
                    is com.chibychibystore.data.model.Result.Success -> {
                        val expenses = result.data
                        _uiState.update { 
                            it.copy(
                                expenses = expenses,
                                totalExpenses = expenses.sumOf { expense -> expense.amount },
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is com.chibychibystore.data.model.Result.Failure -> {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = result.exception.message ?: "Gagal memuat data"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Terjadi kesalahan"
                    )
                }
            }
        }
    }

    /**
     * Set filter tanggal mulai
     */
    fun setStartDate(date: Date?) {
        _uiState.update { it.copy(startDate = date) }
        loadExpenses()
    }

    /**
     * Set filter tanggal akhir
     */
    fun setEndDate(date: Date?) {
        _uiState.update { it.copy(endDate = date) }
        loadExpenses()
    }

    /**
     * Set filter kategori
     */
    fun setCategoryFilter(category: ExpenseCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
        loadExpenses()
    }

    /**
     * Set semua filter sekaligus
     */
    fun setFilters(startDate: Date?, endDate: Date?, category: ExpenseCategory?) {
        _uiState.update { 
            it.copy(
                startDate = startDate,
                endDate = endDate,
                selectedCategory = category
            )
        }
        loadExpenses()
    }

    /**
     * Hapus expense
     */
    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                expenseService.deleteExpense(expenseId)
                    .onSuccess {
                        loadExpenses() // Reload after delete
                    }
                    .onFailure { exception ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                error = exception.message ?: "Gagal menghapus expense"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Gagal menghapus expense"
                    )
                }
            }
        }
    }

    /**
     * Reset filter
     */
    fun resetFilters() {
        _uiState.update { 
            it.copy(
                startDate = null,
                endDate = null,
                selectedCategory = null
            )
        }
        loadExpenses()
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
