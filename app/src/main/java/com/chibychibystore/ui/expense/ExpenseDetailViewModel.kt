package com.chibychibystore.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.service.ExpenseService
import com.chibychibystore.data.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State untuk Expense Detail Screen
 */
data class ExpenseDetailUiState(
    val expense: Pengeluaran? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditing: Boolean = false,
    val editAmount: String = "",
    val editCategory: KategoriPengeluaran? = null,
    val editDescription: String = "",
    val isEditFormValid: Boolean = false
)

/**
 * ViewModel untuk detail expense
 */
@HiltViewModel
class ExpenseDetailViewModel @Inject constructor(
    private val expenseService: ExpenseService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseDetailUiState())
    val uiState: StateFlow<ExpenseDetailUiState> = _uiState

    /**
     * Load expense berdasarkan ID
     */
    fun loadExpense(expenseId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = expenseService.getPengeluaran(expenseId)
                result.onSuccess { data ->
                    _uiState.value = _uiState.value.copy(
                        expense = data,
                        isLoading = false
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Gagal memuat expense",
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
     * Toggle edit mode
     */
    fun toggleEditMode() {
        val currentState = _uiState.value
        val expense = currentState.expense

        if (currentState.isEditing) {
            // Exit edit mode
            _uiState.value = currentState.copy(
                isEditing = false,
                editAmount = "",
                editCategory = null,
                editDescription = "",
                isEditFormValid = false
            )
        } else if (expense != null) {
            // Enter edit mode
            _uiState.value = currentState.copy(
                isEditing = true,
                editAmount = expense.amount.toString(),
                editCategory = expense.category,
                editDescription = expense.description ?: "",
                isEditFormValid = true
            )
        }
    }

    /**
     * Update edit amount
     */
    fun updateEditAmount(amount: String) {
        _uiState.value = _uiState.value.copy(editAmount = amount)
        validateEditForm()
    }

    /**
     * Update edit category
     */
    fun updateEditCategory(category: KategoriPengeluaran) {
        _uiState.value = _uiState.value.copy(editCategory = category)
        validateEditForm()
    }

    /**
     * Update edit description
     */
    fun updateEditDescription(description: String) {
        _uiState.value = _uiState.value.copy(editDescription = description)
        validateEditForm()
    }

    /**
     * Validate edit form
     */
    private fun validateEditForm() {
        val state = _uiState.value
        val isValid = state.editAmount.isNotBlank() &&
                     state.editAmount.toDoubleOrNull() != null &&
                     state.editAmount.toDouble() > 0 &&
                     state.editCategory != null

        _uiState.value = _uiState.value.copy(isEditFormValid = isValid)
    }

    /**
     * Save edited expense
     */
    fun saveExpense(onSuccess: () -> Unit) {
        if (!_uiState.value.isEditFormValid || _uiState.value.expense == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Note: Update functionality would be implemented in ExpenseService
                // For now, just simulate success
                onSuccess()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Gagal menyimpan perubahan",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Delete expense
     */
    fun deleteExpense(onSuccess: () -> Unit) {
        val expense = _uiState.value.expense ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val result = expenseService.deletePengeluaran(expense.id)
                result.onSuccess {
                    onSuccess()
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Gagal menghapus expense",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Gagal menghapus expense",
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