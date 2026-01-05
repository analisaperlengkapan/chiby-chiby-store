package com.chibychibystore.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.service.BackupInfo
import com.chibychibystore.service.BackupProgress
import com.chibychibystore.service.BackupService
import com.chibychibystore.service.RestoreService
import com.chibychibystore.service.BackupPreview
import com.chibychibystore.service.RestoreResult
import com.chibychibystore.service.RestoreProgress
import com.chibychibystore.data.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupService: BackupService,
    private val restoreService: RestoreService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _backupProgress = MutableStateFlow<BackupProgress?>(null)
    val backupProgress: StateFlow<BackupProgress?> = _backupProgress.asStateFlow()

    private val _restoreProgress = MutableStateFlow<RestoreProgress?>(null)
    val restoreProgress: StateFlow<RestoreProgress?> = _restoreProgress.asStateFlow()

    init {
        loadBackupHistory()
        observeProgress()
    }

    private fun observeProgress() {
        viewModelScope.launch {
            backupService.observeBackupProgress().collect { progress ->
                _backupProgress.value = progress
            }
        }

        viewModelScope.launch {
            restoreService.observeRestoreProgress().collect { progress ->
                _restoreProgress.value = progress
            }
        }
    }

    fun loadBackupHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = backupService.getBackupHistory()) {
                is com.chibychibystore.data.model.Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        backupHistory = result.data
                    )
                }
                is com.chibychibystore.data.model.Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exception.message ?: "Gagal memuat riwayat backup"
                    )
                }
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingBackup = true, error = null)

            when (val result = backupService.createBackup()) {
                is com.chibychibystore.data.model.Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingBackup = false,
                        lastBackupInfo = result.data
                    )
                    loadBackupHistory() // Refresh the list
                }
                is com.chibychibystore.data.model.Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingBackup = false,
                        error = result.exception.message ?: "Gagal membuat backup"
                    )
                }
            }
        }
    }

    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeletingBackup = true, error = null)

            when (val result = backupService.deleteBackup(backupId)) {
                is com.chibychibystore.data.model.Result.Success -> {
                    _uiState.value = _uiState.value.copy(isDeletingBackup = false)
                    loadBackupHistory() // Refresh the list
                }
                is com.chibychibystore.data.model.Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isDeletingBackup = false,
                        error = result.exception.message ?: "Gagal menghapus backup"
                    )
                }
            }
        }
    }

    fun previewBackup(backupPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingPreview = true, error = null)

            when (val result = restoreService.previewBackup(backupPath)) {
                is com.chibychibystore.data.model.Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPreview = false,
                        backupPreview = result.data
                    )
                }
                is com.chibychibystore.data.model.Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPreview = false,
                        error = result.exception.message ?: "Gagal memuat preview backup"
                    )
                }
            }
        }
    }

    fun restoreFromBackup(backupPath: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true, error = null)

            when (val result = restoreService.restoreFromBackup(backupPath)) {
                is com.chibychibystore.data.model.Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isRestoring = false,
                        lastRestoreResult = result.data
                    )
                }
                is com.chibychibystore.data.model.Result.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isRestoring = false,
                        error = result.exception.message ?: "Gagal memulihkan data"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearBackupPreview() {
        _uiState.value = _uiState.value.copy(backupPreview = null)
    }

    fun clearRestoreResult() {
        _uiState.value = _uiState.value.copy(lastRestoreResult = null)
    }
}

data class BackupUiState(
    val isLoading: Boolean = false,
    val isCreatingBackup: Boolean = false,
    val isDeletingBackup: Boolean = false,
    val isLoadingPreview: Boolean = false,
    val isRestoring: Boolean = false,
    val backupHistory: List<BackupInfo> = emptyList(),
    val backupPreview: BackupPreview? = null,
    val lastBackupInfo: BackupInfo? = null,
    val lastRestoreResult: RestoreResult? = null,
    val error: String? = null
)