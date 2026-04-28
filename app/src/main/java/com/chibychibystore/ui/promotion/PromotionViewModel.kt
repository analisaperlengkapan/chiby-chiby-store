package com.chibychibystore.ui.promotion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.repository.PromotionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PromotionUiState(
    val promotions: List<Promotion> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class PromotionViewModel @Inject constructor(
    private val promotionRepository: PromotionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PromotionUiState())
    val uiState: StateFlow<PromotionUiState> = _uiState.asStateFlow()

    init {
        loadPromotions()
    }

    private fun loadPromotions() {
        viewModelScope.launch {
            promotionRepository.getAllPromotions()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, promotions = list) }
                }
        }
    }

    fun deletePromotion(promotion: Promotion) {
        viewModelScope.launch {
            try {
                promotionRepository.deletePromotion(promotion)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun savePromotion(promotion: Promotion) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                if (promotion.id == 0L) {
                    promotionRepository.insertPromotion(promotion)
                } else {
                    promotionRepository.updatePromotion(promotion)
                }
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun resetSuccess() {
        _uiState.update { it.copy(isSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    suspend fun getPromotionById(id: Long): Promotion? {
        return promotionRepository.getPromotionById(id)
    }
}
