package com.chibychibystore.ui.cash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Shift
import com.chibychibystore.data.local.entity.ShiftStatus
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.CashManagementService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShiftUiState(
    val currentShift: Shift? = null,
    val shifts: List<Shift> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val cashManagementService: CashManagementService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShiftUiState())
    val uiState: StateFlow<ShiftUiState> = _uiState.asStateFlow()

    init {
        loadCurrentShift()
        loadAllShifts()
    }

    private fun loadCurrentShift() {
        viewModelScope.launch {
            val user = authService.getCurrentUser()
            if (user != null) {
                val result = cashManagementService.getOpenShift(user.id)
                if (result is Result.Success) {
                    _uiState.update { it.copy(currentShift = result.data) }
                }
            }
        }
    }

    private fun loadAllShifts() {
        viewModelScope.launch {
            cashManagementService.observeAllShifts()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, shifts = list) }
                }
        }
    }

    fun openShift(startingCash: Double) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = authService.getCurrentUser()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in") }
                return@launch
            }

            val result = cashManagementService.openShift(user.id, startingCash)
            if (result is Result.Success) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                loadCurrentShift()
            } else {
                _uiState.update { it.copy(isLoading = false, error = (result as Result.Failure).exception.message) }
            }
        }
    }

    fun closeShift(actualCash: Double, notes: String?) {
        viewModelScope.launch {
            val shiftId = _uiState.value.currentShift?.id ?: return@launch
            _uiState.update { it.copy(isLoading = true) }

            val result = cashManagementService.closeShift(shiftId, actualCash, notes)
            if (result is Result.Success) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true, currentShift = null) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = (result as Result.Failure).exception.message) }
            }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
