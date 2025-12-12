package com.chibychibystore.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.ExpenseCategory
import com.chibychibystore.service.ExpenseService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * UI State untuk Expense Add Screen
 */
data class ExpenseAddUiState(
    val amount: String = "",
    val selectedCategory: ExpenseCategory? = null,
    val expenseDate: Date? = Date(),
    val description: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFormValid: Boolean = false
)

/**
 * ViewModel untuk menambah pengeluaran baru
 */
@HiltViewModel
class ExpenseAddViewModel @Inject constructor(
    private val expenseService: ExpenseService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseAddUiState())
    val uiState: StateFlow<ExpenseAddUiState> = _uiState

    init {
        validateForm()
    }

    /**
     * Update jumlah pengeluaran
     */
    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
        validateForm()
    }

    /**
     * Update kategori pengeluaran
     */
    fun updateCategory(category: ExpenseCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        validateForm()
    }

    /**
     * Update tanggal pengeluaran
     */
    fun updateDate(date: Date) {
        _uiState.value = _uiState.value.copy(expenseDate = date)
        validateForm()
    }

    /**
     * Update deskripsi pengeluaran
     */
    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
        validateForm()
    }

    /**
     * Validasi form
     */
    private fun validateForm() {
        val state = _uiState.value
        val isValid = state.amount.isNotBlank() &&
                     state.amount.toDoubleOrNull() != null &&
                     state.amount.toDouble() > 0 &&
                     state.selectedCategory != null &&
                     state.expenseDate != null

        _uiState.value = _uiState.value.copy(isFormValid = isValid)
    }

    /**
     * Simpan pengeluaran
     */
    fun saveExpense(onSuccess: () -> Unit) {
        if (!_uiState.value.isFormValid) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val state = _uiState.value
                val amount = state.amount.toDoubleOrNull() ?: 0.0

                val result = expenseService.createExpense(
                    expenseDate = state.expenseDate!!,
                    category = state.selectedCategory!!,
                    amount = amount,
                    description = state.description.takeIf { it.isNotBlank() }
                )

                if (result.isSuccess) {
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = result.exceptionOrNull()?.message ?: "Gagal menyimpan pengeluaran",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Terjadi kesalahan",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}