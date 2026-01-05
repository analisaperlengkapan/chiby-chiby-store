package com.chibychibystore.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.service.ExpenseService
import com.chibychibystore.data.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

/**
 * UI State untuk Expense Screen
 */
data class ExpenseUiState(
    val expenses: List<Pengeluaran> = emptyList(),
    val startDate: Date? = null,
    val endDate: Date? = null,
    val selectedCategory: KategoriPengeluaran? = null,
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

    // Filter States
    private val _startDate = MutableStateFlow<Date?>(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))
    private val _endDate = MutableStateFlow<Date?>(Date())
    private val _selectedCategory = MutableStateFlow<KategoriPengeluaran?>(null)
    private val _refreshTrigger = MutableStateFlow(0)

    // UI Feedback States
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    // Data Loading Pipeline
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _expensesDataFlow = combine(
        _startDate,
        _endDate,
        _selectedCategory,
        _refreshTrigger
    ) { start, end, category, _ ->
        Triple(start, end, category)
    }.flatMapLatest { (start, end, category) ->
        _isLoading.value = true
        _error.value = null

        flow<List<Pengeluaran>> {
            try {
                val startLocalDate = start?.let { java.time.Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).toLocalDate() }
                val endLocalDate = end?.let { java.time.Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault()).toLocalDate() }

                val result = expenseService.getPengeluarans(
                    startLocalDate,
                    endLocalDate,
                    category?.name
                )

                val list = when (result) {
                    is Result.Success -> result.data
                    is Result.Failure -> {
                        _error.value = result.exception.message
                        emptyList()
                    }
                }
                emit(list)
            } catch (e: Exception) {
                _error.value = e.message
                emit(emptyList())
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Combined UI State using array-based combine for 6+ flows
    val uiState: StateFlow<ExpenseUiState> = combine(
        _expensesDataFlow,
        _startDate,
        _endDate,
        _selectedCategory,
        _isLoading,
        _error
    ) { values: Array<*> ->
        @Suppress("UNCHECKED_CAST")
        val expenses = values[0] as List<Pengeluaran>
        val start = values[1] as Date?
        val end = values[2] as Date?
        val category = values[3] as KategoriPengeluaran?
        val loading = values[4] as Boolean
        val errorMsg = values[5] as String?

        ExpenseUiState(
            expenses = expenses,
            startDate = start,
            endDate = end,
            selectedCategory = category,
            isLoading = loading,
            error = errorMsg,
            totalExpenses = expenses.sumOf { it.amount }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseUiState(isLoading = true)
    )

    // User Actions

    fun setDateRange(start: Date?, end: Date?) {
        _startDate.value = start
        _endDate.value = end
    }

    fun setCategory(category: KategoriPengeluaran?) {
        _selectedCategory.value = category
    }

    fun refresh() {
        _refreshTrigger.value = _refreshTrigger.value + 1
    }

    fun createExpense(
        date: Date,
        category: KategoriPengeluaran,
        amount: Double,
        description: String?,
        createdBy: Long
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val pengeluaran = Pengeluaran(
                    expenseDate = date,
                    category = category,
                    amount = amount,
                    description = description,
                    createdBy = createdBy
                )
                val result = expenseService.createPengeluaran(pengeluaran)
                result.onSuccess {
                    refresh()
                }.onFailure { e ->
                    _error.value = e.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = expenseService.deletePengeluaran(id)
                result.onSuccess {
                    refresh()
                }.onFailure { e ->
                    _error.value = e.message
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
