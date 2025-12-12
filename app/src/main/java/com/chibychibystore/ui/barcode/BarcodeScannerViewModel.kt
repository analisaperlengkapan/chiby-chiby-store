package com.chibychibystore.ui.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State untuk Barcode Scanner Screen
 */
data class BarcodeScannerUiState(
    val hasCameraPermission: Boolean = false,
    val isScanning: Boolean = false,
    val isProcessing: Boolean = false,
    val lastScannedBarcode: String? = null,
    val showManualInput: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel untuk Barcode Scanner Screen
 * Mengelola state scanning barcode dan permission handling
 */
@HiltViewModel
class BarcodeScannerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(BarcodeScannerUiState())
    val uiState: StateFlow<BarcodeScannerUiState> = _uiState

    /**
     * Handle hasil permission request
     */
    fun onPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasCameraPermission = granted,
            isScanning = granted,
            error = if (!granted) "Camera permission denied" else null
        )
    }

    /**
     * Handle barcode berhasil di-scan
     */
    fun onBarcodeDetected(barcode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessing = true,
                lastScannedBarcode = barcode,
                error = null
            )

            // Simulate processing delay for UI feedback
            kotlinx.coroutines.delay(500)

            _uiState.value = _uiState.value.copy(isProcessing = false)
        }
    }

    /**
     * Handle error saat scanning
     */
    fun onScanError(error: String) {
        _uiState.value = _uiState.value.copy(
            error = error,
            isProcessing = false
        )
    }

    /**
     * Handle manual barcode input
     */
    fun onManualBarcodeEntered(barcode: String) {
        _uiState.value = _uiState.value.copy(
            lastScannedBarcode = barcode,
            showManualInput = false,
            error = null
        )
    }

    /**
     * Show manual input dialog
     */
    fun showManualInput() {
        _uiState.value = _uiState.value.copy(showManualInput = true)
    }

    /**
     * Hide manual input dialog
     */
    fun hideManualInput() {
        _uiState.value = _uiState.value.copy(showManualInput = false)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Reset scanner state
     */
    fun resetScanner() {
        _uiState.value = BarcodeScannerUiState()
    }
}