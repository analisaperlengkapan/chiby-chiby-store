package com.chibychibystore.ui.supplier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Supplier
import com.chibychibystore.service.SupplierService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierUiState(
    val suppliers: List<Supplier> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedSupplier: Supplier? = null,
    val isAddEditDialogVisible: Boolean = false
)

data class SupplierFormData(
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null
)

@HiltViewModel
class SupplierViewModel @Inject constructor(
    private val supplierService: SupplierService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SupplierUiState())
    val uiState: StateFlow<SupplierUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        observeSuppliers()
    }

    private fun observeSuppliers() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    val flow = if (query.isBlank()) {
                        supplierService.getAllSuppliers()
                    } else {
                        supplierService.searchSuppliers(query)
                    }
                    flow.catch { e ->
                        // Emit empty list or handle error locally to prevent outer flow termination
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load suppliers"
                        )
                        emit(emptyList())
                    }
                }
                .onStart { _uiState.value = _uiState.value.copy(isLoading = true) }
                .collect { suppliers ->
                    _uiState.value = _uiState.value.copy(
                        suppliers = suppliers,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onAddSupplierClick() {
        _uiState.value = _uiState.value.copy(
            selectedSupplier = null,
            isAddEditDialogVisible = true
        )
    }

    fun onEditSupplierClick(supplier: Supplier) {
        _uiState.value = _uiState.value.copy(
            selectedSupplier = supplier,
            isAddEditDialogVisible = true
        )
    }

    fun onDismissAddEditDialog() {
        _uiState.value = _uiState.value.copy(
            isAddEditDialogVisible = false,
            selectedSupplier = null
        )
    }

    fun saveSupplier(formData: SupplierFormData) {
        viewModelScope.launch {
            val result = if (_uiState.value.selectedSupplier == null) {
                supplierService.createSupplier(
                    name = formData.name,
                    address = formData.address,
                    phone = formData.phone,
                    email = formData.email
                )
            } else {
                supplierService.updateSupplier(
                    id = _uiState.value.selectedSupplier!!.id,
                    name = formData.name,
                    address = formData.address,
                    phone = formData.phone,
                    email = formData.email
                )
            }

            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isAddEditDialogVisible = false,
                    selectedSupplier = null,
                    error = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Gagal menyimpan supplier"
                )
            }
        }
    }

    fun deleteSupplier(id: Long) {
        viewModelScope.launch {
            supplierService.deleteSupplier(id)
                .onSuccess {
                    // Success handled by flow update
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Gagal menghapus supplier"
                    )
                }
        }
    }

    fun onErrorShown() {
         _uiState.value = _uiState.value.copy(error = null)
    }
}
