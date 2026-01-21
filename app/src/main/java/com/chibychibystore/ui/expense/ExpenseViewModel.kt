package com.chibychibystore.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.service.ExpenseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
<<<<<<< HEAD
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
=======
import kotlinx.coroutines.flow.asStateFlow
>>>>>>> feat/ui-overhaul
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
<<<<<<< HEAD
import java.util.*
=======
import java.util.Date
>>>>>>> feat/ui-overhaul
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

<<<<<<< HEAD
    // Filter States
    private val _startDate = MutableStateFlow<Date?>(Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000))
    private val _endDate = MutableStateFlow<Date?>(Date())
    private val _selectedCategory = MutableStateFlow<KategoriPengeluaran?>(null)
    private val _refreshTrigger = MutableStateFlow(0)
=======
    private val _uiState = MutableStateFlow(ExpenseUiState(isLoading = true))
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()
>>>>>>> feat/ui-overhaul

    init {
        loadExpenses()
    }

<<<<<<< HEAD
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
=======
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
>>>>>>> feat/ui-overhaul

                val result = expenseService.getPengeluarans(
                    startLocalDate,
                    endLocalDate,
                    currentState.selectedCategory?.name
                )

<<<<<<< HEAD
                val list = when (result) {
                    is Result.Success -> result.data
                    is Result.Failure -> {
                        _error.value = result.exception.message
                        emptyList()
=======
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
>>>>>>> feat/ui-overhaul
                    }
                }
            } catch (e: Exception) {
<<<<<<< HEAD
                _error.value = e.message
                emit(emptyList())
            } finally {
                _isLoading.value = false
=======
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Terjadi kesalahan"
                    )
                }
>>>>>>> feat/ui-overhaul
            }
        }
    }

<<<<<<< HEAD
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
=======
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
>>>>>>> feat/ui-overhaul
            }
        }
    }

<<<<<<< HEAD
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
=======
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
>>>>>>> feat/ui-overhaul
    }

    fun clearError() {
<<<<<<< HEAD
        _error.value = null
=======
        _uiState.update { it.copy(error = null) }
>>>>>>> feat/ui-overhaul
    }
}
