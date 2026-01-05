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
import kotlinx.coroutines.flow.update
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
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = expenseService.getPengeluaran(expenseId)
                result.onSuccess { data ->
                    _uiState.update {
                        it.copy(
                            expense = data,
                            isLoading = false
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Gagal memuat expense",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Terjadi kesalahan",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Toggle edit mode
     */
    fun toggleEditMode() {
        _uiState.update { currentState ->
            val expense = currentState.expense

            if (currentState.isEditing) {
                // Exit edit mode
                currentState.copy(
                    isEditing = false,
                    editAmount = "",
                    editCategory = null,
                    editDescription = "",
                    isEditFormValid = false
                )
            } else if (expense != null) {
                // Enter edit mode
                currentState.copy(
                    isEditing = true,
                    editAmount = expense.amount.toString(),
                    editCategory = expense.category,
                    editDescription = expense.description ?: "",
                    isEditFormValid = true
                )
            } else {
                currentState
            }
        }
    }

    /**
     * Update edit amount
     */
    fun updateEditAmount(amount: String) {
        _uiState.update { it.copy(editAmount = amount) }
        validateEditForm()
    }

    /**
     * Update edit category
     */
    fun updateEditCategory(category: KategoriPengeluaran) {
        _uiState.update { it.copy(editCategory = category) }
        validateEditForm()
    }

    /**
     * Update edit description
     */
    fun updateEditDescription(description: String) {
        _uiState.update { it.copy(editDescription = description) }
        validateEditForm()
    }

    /**
     * Validate edit form
     */
    private fun validateEditForm() {
        _uiState.update { state ->
            val isValid = state.editAmount.isNotBlank() &&
                    state.editAmount.toDoubleOrNull() != null &&
                    state.editAmount.toDouble() > 0 &&
                    state.editCategory != null

            state.copy(isEditFormValid = isValid)
        }
    }

    /**
     * Save edited expense
     */
    fun saveExpense(onSuccess: () -> Unit) {
        val currentState = _uiState.value
        val expense = currentState.expense
        if (!currentState.isEditFormValid || expense == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val updatedExpense = expense.copy(
                    amount = currentState.editAmount.toDouble(),
                    category = currentState.editCategory!!,
                    description = currentState.editDescription
                )

                val result = expenseService.updatePengeluaran(updatedExpense)

                result.onSuccess {
                    // Update UI with new data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            expense = updatedExpense,
                            isEditing = false
                        )
                    }
                    onSuccess()
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Gagal menyimpan perubahan",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Gagal menyimpan perubahan",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Delete expense
     */
    fun deleteExpense(onSuccess: () -> Unit) {
        val expense = _uiState.value.expense ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = expenseService.deletePengeluaran(expense.id)
                result.onSuccess {
                    onSuccess()
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = e.message ?: "Gagal menghapus expense",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Gagal menghapus expense",
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Clear error
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
