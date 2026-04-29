package com.chibychibystore.ui.pelanggan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Pelanggan
import com.chibychibystore.data.model.Result
import com.chibychibystore.service.PelangganService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PelangganUiState(
    val pelangganList: List<Pelanggan> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class PelangganViewModel @Inject constructor(
    private val pelangganService: PelangganService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PelangganUiState())
    val uiState: StateFlow<PelangganUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadPelanggan()
    }

    private fun loadPelanggan() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        pelangganService.ambilSemuaPelanggan()
                    } else {
                        pelangganService.cariPelanggan(query)
                    }
                }
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, pelangganList = list) }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun savePelanggan(id: Long = 0, nama: String, telepon: String?, email: String?, alamat: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (id == 0L) {
                pelangganService.buatPelanggan(nama, telepon, email, alamat)
            } else {
                pelangganService.perbaruiPelanggan(id, nama, telepon, email, alamat)
            }

            if (result is Result.Success) {
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = (result as Result.Failure).exception.message) }
            }
        }
    }

    fun deletePelanggan(pelanggan: Pelanggan) {
        viewModelScope.launch {
            pelangganService.hapusPelanggan(pelanggan)
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    suspend fun getPelangganById(id: Long): Pelanggan? {
        val result = pelangganService.getPelangganById(id)
        return if (result is Result.Success) result.data else null
    }
}
