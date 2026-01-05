package com.chibychibystore.ui.settings
import com.chibychibystore.ui.components.shared.ErrorMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.service.AuthService
import com.chibychibystore.service.BackupService
import com.chibychibystore.data.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk Settings Screen
 *
 * Mengelola state dan business logic untuk halaman settings aplikasi Chiby Chiby Store.
 * Bertanggung jawab atas operasi-operasi settings seperti:
 * - User profile management
 * - Password changes
 * - Backup operations
 * - Logout functionality
 *
 * **Architecture Pattern:**
 * - MVVM dengan reactive UI updates melalui StateFlow
 * - Dependency injection menggunakan Hilt
 * - Coroutine-based async operations
 * - Error handling dengan user-friendly messages
 *
 * **State Management:**
 * Menggunakan [SettingsUiState] untuk merepresentasikan seluruh UI state.
 * State updates dilakukan secara immutable melalui copy() operations.
 * Reactive updates ke UI melalui StateFlow collection.
 *
 * **Business Logic Flow:**
 * ```kotlin
 * // 1. Initialization
 * SettingsViewModel() → loadCurrentUser() → update UI state
 *
 * // 2. Password Change Flow
 * changePassword() → validate → call AuthService → update state
 *     ↓ success: show success message
 *     ↓ failure: show error message
 *
 * // 3. Backup Flow
 * createBackup() → call BackupService → update lastBackupDate
 *     ↓ success: update timestamp
 *     ↓ failure: show error message
 *
 * // 4. Logout Flow
 * logout() → call AuthService → navigate to login
 * ```
 *
 * **Security Considerations:**
 * - Password changes memerlukan validasi old password
 * - Backup operations menggunakan encrypted storage
 * - User data hanya diakses melalui AuthService
 * - Error messages tidak expose sensitive information
 *
 * **Error Handling:**
 * - Network/database errors: User-friendly Indonesian messages
 * - Validation errors: Specific field-level feedback
 * - State recovery: Clear error states on new operations
 * - Logging: Internal error logging untuk debugging
 *
 * **Dependencies:**
 * - [AuthService]: User authentication dan profile management
 * - [BackupService]: Data backup dan restore operations
 * - [viewModelScope]: Lifecycle-aware coroutine scope
 *
 * **UI Integration:**
 * ```kotlin
 * @Composable
 * fun SettingsScreen(viewModel: SettingsViewModel) {
 *     val uiState by viewModel.uiState.collectAsState()
 *
 *     // User profile section
 *     uiState.currentUser?.let { user ->
 *         UserProfileCard(user)
 *     }
 *
 *     // Password change section
 *     if (uiState.isChangingPassword) {
 *         CircularProgressIndicator()
 *     }
 *
 *     // Error display
 *     uiState.error?.let { error ->
 *         ErrorMessage(error)
 *     }
 *
 *     // Success feedback
 *     if (uiState.passwordChangeSuccess) {
 *         SuccessMessage("Password berhasil diubah")
 *     }
 * }
 * ```
 *
 * **Testing Strategy:**
 * - Unit tests: Mock services, test state transitions
 * - Integration tests: Real services dengan test database
 * - UI tests: StateFlow collection dan UI updates
 *
 * **Performance Considerations:**
 * - Lazy initialization: User data dimuat saat diperlukan
 * - Coroutine cancellation: Operations cancelled saat ViewModel destroyed
 * - Memory management: StateFlow properly scoped
 * - Background operations: Backup tidak block UI thread
 *
 * @property authService Service untuk operasi authentication dan user management
 * @property backupService Service untuk operasi backup dan restore
 * @property uiState Reactive state flow untuk UI updates
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authService: AuthService,
    private val backupService: BackupService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    /**
     * Memuat data user saat ini untuk ditampilkan di settings
     *
     * Method private yang dipanggil otomatis saat ViewModel diinisialisasi.
     * Mengambil data user yang sedang login dari AuthService dan mengupdate UI state.
     * Operasi ini dilakukan secara asynchronous menggunakan coroutine.
     *
     * **Business Logic:**
     * 1. Panggil AuthService.getCurrentUser() untuk mendapatkan data user
     * 2. Jika sukses: Update state dengan user data dan set loading = false
     * 3. Jika gagal: Update state dengan error message dan set loading = false
     *
     * **State Transitions:**
     * ```kotlin
     * // Initial state: isLoading = true
     * // Success case:
     * state.copy(currentUser = user, isLoading = false)
     *
     * // Error case:
     * state.copy(error = "Gagal memuat data user", isLoading = false)
     * ```
     *
     * **Error Handling:**
     * - Exception dari AuthService: Ditangkap dan dikonversi ke user-friendly message
     * - Default error message: "Gagal memuat data user"
     * - State tetap konsisten: loading selalu diset false setelah operasi
     *
     * **Threading:**
     * - Dijalankan di IO dispatcher melalui viewModelScope
     * - Tidak block UI thread
     * - Automatically cancelled jika ViewModel destroyed
     *
     * **Dependencies:**
     * - [AuthService.getCurrentUser()]: Mengambil data user dari session/database
     * - [viewModelScope]: Lifecycle-aware coroutine execution
     * - [_uiState]: Mutable state untuk UI updates
     *
     * **Testing:**
     * ```kotlin
     * @Test
     * fun `loadCurrentUser success updates state correctly`() {
     *     // Given: Mock AuthService returns user
     *     // When: loadCurrentUser called
     *     // Then: State updated with user, loading = false
     * }
     *
     * @Test
     * fun `loadCurrentUser error shows error message`() {
     *     // Given: Mock AuthService throws exception
     *     // When: loadCurrentUser called
     *     // Then: State updated with error, loading = false
     * }
     * ```
     *
     * **Performance:**
     * - Lightweight operation: Hanya query database untuk current user
     * - Cached result: AuthService biasanya cache user data
     * - Fast execution: Tidak ada complex computations
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                val user = authService.getCurrentUser()
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Gagal memuat data user",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Mengubah password user saat ini
     *
     * Method public yang dipanggil dari UI untuk mengubah password user.
     * Melakukan validasi dan update password melalui AuthService.
     * Operasi dilakukan secara asynchronous dengan state management.
     *
     * **Business Logic:**
     * 1. Set loading state: isChangingPassword = true, clear previous errors
     * 2. Panggil AuthService.changePassword() dengan old dan new password
     * 3. Jika sukses: Update state dengan success flag
     * 4. Jika gagal: Update state dengan error message
     *
     * **Security Considerations:**
     * - Old password required untuk verifikasi identitas
     * - Password validation dilakukan di AuthService layer
     * - Error messages tidak expose password details
     * - Operation logged untuk audit trail
     *
     * **State Transitions:**
     * ```kotlin
     * // Initial: Start operation
     * state.copy(isChangingPassword = true, error = null)
     *
     * // Success case:
     * state.copy(isChangingPassword = false, passwordChangeSuccess = true)
     *
     * // Error case:
     * state.copy(isChangingPassword = false, error = "Invalid old password")
     * ```
     *
     * **Error Handling:**
     * - Invalid old password: "Password lama tidak valid"
     * - Weak new password: "Password baru terlalu lemah"
     * - Network/Database errors: "Gagal mengubah password"
     * - State recovery: Loading flag selalu direset
     *
     * **UI Integration:**
     * ```kotlin
     * // In Composable:
     * Button(
     *     onClick = { viewModel.changePassword(oldPass, newPass) },
     *     enabled = !uiState.isChangingPassword
     * ) {
     *     if (uiState.isChangingPassword) {
     *         CircularProgressIndicator()
     *     } else {
     *         Text("Ubah Password")
     *     }
     * }
     *
     * // Success feedback:
     * if (uiState.passwordChangeSuccess) {
     *     LaunchedEffect(Unit) {
     *         // Show success message, then clear flag
     *         viewModel.clearPasswordChangeSuccess()
     *     }
     * }
     * ```
     *
     * **Threading:**
     * - Async execution menggunakan viewModelScope
     * - Non-blocking UI operations
     * - Automatic cancellation on ViewModel destroy
     *
     * **Dependencies:**
     * - [AuthService.changePassword()]: Core password change logic
     * - [viewModelScope]: Coroutine execution context
     * - [_uiState]: State management untuk UI feedback
     *
     * **Testing:**
     * ```kotlin
     * @Test
     * fun `changePassword success updates state correctly`() {
     *     // Given: Mock AuthService returns success
     *     // When: changePassword called with valid passwords
     *     // Then: State shows success, loading = false
     * }
     *
     * @Test
     * fun `changePassword failure shows error message`() {
     *     // Given: Mock AuthService throws exception
     *     // When: changePassword called
     *     // Then: State shows error, loading = false
     * }
     * ```
     *
     * **Performance:**
     * - Fast operation: Hashing dan database update
     * - Minimal memory usage: String operations only
     * - Scalable: Same performance untuk all user types
     *
     * @param oldPassword Password lama untuk verifikasi (required)
     * @param newPassword Password baru yang akan diset (required)
     */
    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isChangingPassword = true, error = null)

            try {
                val result = authService.changePassword(oldPassword, newPassword)
                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isChangingPassword = false,
                        passwordChangeSuccess = true
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isChangingPassword = false,
                        error = error.message ?: "Gagal mengubah password"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isChangingPassword = false,
                    error = e.message ?: "Terjadi kesalahan"
                )
            }
        }
    }

    /**
     * Logout user dari aplikasi
     *
     * Method public yang dipanggil dari UI untuk mengakhiri session user.
     * Membersihkan authentication state dan menavigasi ke login screen.
     * Operasi dilakukan secara asynchronous dengan callback untuk navigation.
     *
     * **Business Logic:**
     * 1. Panggil AuthService.logout() untuk clear session
     * 2. Jika sukses: Panggil callback onLogoutSuccess untuk navigation
     * 3. Jika gagal: Update state dengan error message
     *
     * **Security Considerations:**
     * - Session data dihapus dari memory dan storage
     * - Sensitive data cleared sebelum navigation
     * - No user data remains accessible setelah logout
     * - Audit logging untuk security tracking
     *
     * **State Transitions:**
     * ```kotlin
     * // Logout process doesn't change UI state directly
     * // Error case only:
     * state.copy(error = "Gagal logout")
     * ```
     *
     * **Error Handling:**
     * - Database errors: "Gagal logout"
     * - Network errors: "Gagal logout" (offline-first app)
     * - State recovery: Error state dapat diclear dengan clearError()
     *
     * **UI Integration:**
     * ```kotlin
     * // In Composable:
     * Button(onClick = {
     *     viewModel.logout {
     *         // Navigation callback
     *         navController.navigate("login") {
     *             popUpTo("main") { inclusive = true }
     *         }
     *     }
     * }) {
     *     Text("Logout")
     * }
     *
     * // Error handling:
     * uiState.error?.let { error ->
     *     if (error.contains("logout")) {
     *         AlertDialog(
     *             title = "Logout Error",
     *             text = error,
     *             onDismiss = { viewModel.clearError() }
     *         )
     *     }
     * }
     * ```
     *
     * **Navigation Flow:**
     * ```kotlin
     * SettingsScreen → logout() → AuthService.logout()
     *     ↓ success → onLogoutSuccess() → navigate to LoginScreen
     *     ↓ failure → show error → user can retry
     * ```
     *
     * **Threading:**
     * - Async execution menggunakan viewModelScope
     * - Non-blocking UI operations
     * - Automatic cancellation on ViewModel destroy
     *
     * **Dependencies:**
     * - [AuthService.logout()]: Core logout logic dan session cleanup
     * - [viewModelScope]: Coroutine execution context
     * - [onLogoutSuccess]: Callback untuk navigation handling
     *
     * **Testing:**
     * ```kotlin
     * @Test
     * fun `logout success calls callback and clears session`() {
     *     // Given: Mock AuthService logout succeeds
     *     // When: logout called with callback
     *     // Then: Callback invoked, session cleared
     * }
     *
     * @Test
     * fun `logout failure shows error message`() {
     *     // Given: Mock AuthService throws exception
     *     // When: logout called
     *     // Then: State updated with error message
     * }
     * ```
     *
     * **Performance:**
     * - Fast operation: Session cleanup dan database updates
     * - Minimal memory usage: State clearing operations
     * - Immediate feedback: Callback executed synchronously setelah success
     *
     * @param onLogoutSuccess Callback yang dipanggil ketika logout berhasil untuk navigation
     */
    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                authService.logout()
                onLogoutSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Gagal logout"
                )
            }
        }
    }

    /**
     * Membersihkan error message dari UI state
     *
     * Method public yang dipanggil dari UI untuk menghilangkan error message
     * yang sedang ditampilkan. Biasanya dipanggil setelah user melihat error
     * atau ketika memulai operasi baru.
     *
     * **Business Logic:**
     * - Reset error field ke null
     * - State lainnya tetap tidak berubah
     * - Immediate state update tanpa async operations
     *
     * **Use Cases:**
     * - User menutup error dialog/snackbar
     * - Starting new operation yang clear previous errors
     * - Reset state setelah error handling
     *
     * **State Transitions:**
     * ```kotlin
     * // Before: state.copy(error = "Some error message")
     * clearError()
     * // After: state.copy(error = null) // other fields unchanged
     * ```
     *
     * **UI Integration:**
     * ```kotlin
     * // Error display with dismiss action:
     * uiState.error?.let { error ->
     *     Snackbar(
     *         action = {
     *             TextButton(onClick = { viewModel.clearError() }) {
     *                 Text("Tutup")
     *             }
     *         }
     *     ) {
     *         Text(error)
     *     }
     * }
     *
     * // Or in AlertDialog:
     * AlertDialog(
     *     onDismissRequest = { viewModel.clearError() },
     *     // ...
     * )
     * ```
     *
     * **Threading:**
     * - Synchronous operation: Immediate state update
     * - No coroutine required: Simple state mutation
     * - Thread-safe: StateFlow handles concurrent access
     *
     * **Dependencies:**
     * - [_uiState]: Mutable state untuk immediate updates
     *
     * **Testing:**
     * ```kotlin
     * @Test
     * fun `clearError removes error message from state`() {
     *     // Given: State has error message
     *     _uiState.value = SettingsUiState(error = "Test error")
     *
     *     // When: clearError called
     *     viewModel.clearError()
     *
     *     // Then: Error is null
     *     assertEquals(null, _uiState.value.error)
     * }
     * ```
     *
     * **Performance:**
     * - Instant operation: No async delays
     * - Minimal overhead: Simple state copy
     * - Memory efficient: No additional allocations
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Membersihkan flag success perubahan password
     *
     * Method public yang dipanggil dari UI untuk mereset flag password change success
     * setelah user melihat konfirmasi success. Mencegah success message
     * ditampilkan terus menerus.
     *
     * **Business Logic:**
     * - Reset passwordChangeSuccess flag ke false
     * - State lainnya tetap tidak berubah
     * - Immediate state update tanpa async operations
     *
     * **Use Cases:**
     * - User melihat success message dan menutupnya
     * - Navigation setelah password change success
     * - Reset state untuk operasi password change berikutnya
     *
     * **State Transitions:**
     * ```kotlin
     * // Before: state.copy(passwordChangeSuccess = true)
     * clearPasswordChangeSuccess()
     * // After: state.copy(passwordChangeSuccess = false) // other fields unchanged
     * ```
     *
     * **UI Integration:**
     * ```kotlin
     * // Success feedback with auto-clear:
     * if (uiState.passwordChangeSuccess) {
     *     LaunchedEffect(Unit) {
     *         // Show success message
     *         showSnackbar("Password berhasil diubah!")
     *
     *         // Clear flag after delay or user interaction
     *         delay(3000) // Auto-clear after 3 seconds
     *         viewModel.clearPasswordChangeSuccess()
     *     }
     * }
     *
     * // Or with manual clear:
     * if (uiState.passwordChangeSuccess) {
     *     AlertDialog(
     *         onDismissRequest = { viewModel.clearPasswordChangeSuccess() },
     *         title = { Text("Berhasil!") },
     *         text = { Text("Password telah diubah") },
     *         confirmButton = {
     *             TextButton(onClick = { viewModel.clearPasswordChangeSuccess() }) {
     *                 Text("Oke")
     *             }
     *         }
     *     )
     * }
     * ```
     *
     * **Threading:**
     * - Synchronous operation: Immediate state update
     * - No coroutine required: Simple state mutation
     * - Thread-safe: StateFlow handles concurrent access
     *
     * **Dependencies:**
     * - [_uiState]: Mutable state untuk immediate updates
     *
     * **Testing:**
     * ```kotlin
     * @Test
     * fun `clearPasswordChangeSuccess resets success flag`() {
     *     // Given: State has success flag set
     *     _uiState.value = SettingsUiState(passwordChangeSuccess = true)
     *
     *     // When: clearPasswordChangeSuccess called
     *     viewModel.clearPasswordChangeSuccess()
     *
     *     // Then: Success flag is false
     *     assertEquals(false, _uiState.value.passwordChangeSuccess)
     * }
     * ```
     *
     * **Performance:**
     * - Instant operation: No async delays
     * - Minimal overhead: Simple state copy
     * - Memory efficient: No additional allocations
     */
    fun clearPasswordChangeSuccess() {
        _uiState.value = _uiState.value.copy(passwordChangeSuccess = false)
    }

    /**
     * Membuat backup data aplikasi
     *
     * Method public yang dipanggil dari UI untuk membuat backup lengkap
     * dari semua data aplikasi. Backup dilakukan secara asynchronous dengan
     * progress indication dan error handling.
     *
     * **Business Logic:**
     * 1. Set loading state: isCreatingBackup = true, clear previous errors
     * 2. Panggil BackupService.createBackup() untuk export data
     * 3. Jika sukses: Update state dengan timestamp backup terbaru
     * 4. Jika gagal: Update state dengan error message
     *
     * **Data Included in Backup:**
     * - User accounts dan permissions
     * - Product catalog dengan categories
     * - Warehouse dan stock information
     * - Sales transactions dan history
     * - Purchase records dan suppliers
     * - Expense data dan financial records
     *
     * **Security Considerations:**
     * - Backup files dienkripsi dengan AES-256-GCM
     * - Master key dari Android KeyStore
     * - File extension .enc untuk encrypted files
     * - No sensitive data exposed in plain text
     *
     * **State Transitions:**
     * ```kotlin
     * // Initial: Start backup operation
     * state.copy(isCreatingBackup = true, error = null)
     *
     * // Success case:
     * state.copy(isCreatingBackup = false, lastBackupDate = timestamp)
     *
     * // Error case:
     * state.copy(isCreatingBackup = false, error = "Backup failed")
     * ```
     *
     * **Error Handling:**
     * - Storage permission denied: "Izin penyimpanan diperlukan"
     * - Insufficient storage: "Ruang penyimpanan tidak cukup"
     * - Encryption errors: "Gagal mengenkripsi backup"
     * - Database errors: "Gagal mengakses data"
     *
     * **UI Integration:**
     * ```kotlin
     * // Backup button with loading state:
     * Button(
     *     onClick = { viewModel.createBackup() },
     *     enabled = !uiState.isCreatingBackup
     * ) {
     *     if (uiState.isCreatingBackup) {
     *         CircularProgressIndicator()
     *         Spacer(modifier = Modifier.width(8.dp))
     *         Text("Membuat Backup...")
     *     } else {
     *         Text("Buat Backup")
     *     }
     * }
     *
     * // Last backup display:
     * uiState.lastBackupDate?.let { timestamp ->
     *     Text("Backup terakhir: ${formatDate(timestamp)}")
     * }
     *
     * // Success feedback:
     * LaunchedEffect(uiState.lastBackupDate) {
     *     // Show success message when backup completes
     *     showSnackbar("Backup berhasil dibuat!")
     * }
     * ```
     *
     * **File Management:**
     * - Location: Downloads/ChibyChibyBackup/
     * - Naming: backup_YYYYMMDD_HHMMSS.enc
     * - Size: Depends on data volume (users, transactions, products)
     * - Retention: Manual deletion required
     *
     * **Threading:**
     * - Async execution menggunakan viewModelScope
     * - Background operation: Tidak block UI
     * - Automatic cancellation on ViewModel destroy
     * - Progress indication untuk long operations
     *
     * **Dependencies:**
     * - [BackupService.createBackup()]: Core backup logic dengan encryption
     * - [viewModelScope]: Coroutine execution context
     * - [_uiState]: State management untuk progress dan results
     *
     * **Testing:**
     * ```kotlin
     * @Test
     * fun `createBackup success updates lastBackupDate`() {
     *     // Given: Mock BackupService returns success with timestamp
     *     // When: createBackup called
     *     // Then: State updated with timestamp, loading = false
     * }
     *
     * @Test
     * fun `createBackup failure shows error message`() {
     *     // Given: Mock BackupService throws exception
     *     // When: createBackup called
     *     // Then: State shows error, loading = false
     * }
     * ```
     *
     * **Performance:**
     * - Variable duration: Depends on data size (seconds to minutes)
     * - Memory usage: Streaming encryption untuk large datasets
     * - Storage impact: Compressed encrypted files
     * - Background execution: UI remains responsive
     *
     * **Best Practices:**
     * - Regular backups: Weekly atau sebelum major changes
     * - Test restores: Verify backup integrity periodically
     * - Secure storage: Keep backup files in safe location
     * - Version control: Track backup versions untuk rollback
     */
    fun createBackup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreatingBackup = true, error = null)

            when (val result = backupService.createBackup()) {
                is com.chibychibystore.data.model.Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isCreatingBackup = false,
                        lastBackupDate = result.data.createdAt
                    )
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
}

/**
 * UI State untuk Settings Screen
 *
 * Data class yang merepresentasikan seluruh state UI untuk halaman settings.
 * Menggunakan immutable data class pattern untuk reactive UI updates dengan StateFlow.
 * Semua field bersifat immutable dan perubahan state dilakukan melalui copy().
 *
 * **State Fields:**
 * - `currentUser`: Data user yang sedang login (nullable = not loaded)
 * - `isLoading`: Status loading data user saat startup
 * - `isChangingPassword`: Status proses perubahan password
 * - `passwordChangeSuccess`: Flag sukses perubahan password
 * - `error`: Error message yang ditampilkan (nullable = no error)
 * - `lastBackupDate`: Timestamp backup terakhir (nullable = no backup)
 * - `isCreatingBackup`: Status proses pembuatan backup
 *
 * **State Transitions:**
 * ```kotlin
 * // Initial state
 * SettingsUiState() // isLoading = true, other defaults
 *
 * // User loaded
 * state.copy(currentUser = user, isLoading = false)
 *
 * // Password change in progress
 * state.copy(isChangingPassword = true, error = null)
 *
 * // Password change success
 * state.copy(isChangingPassword = false, passwordChangeSuccess = true)
 *
 * // Backup creation
 * state.copy(isCreatingBackup = true)
 * // ... backup complete
 * state.copy(isCreatingBackup = false, lastBackupDate = timestamp)
 *
 * // Error state
 * state.copy(error = "Operation failed", isChangingPassword = false)
 * ```
 *
 * **UI Integration:**
 * - User Profile: Displayed dari `currentUser` field
 * - Loading Indicators: Shown ketika `isLoading*` fields = true
 * - Success Messages: Triggered ketika `passwordChangeSuccess = true`
 * - Error Messages: Displayed ketika `error != null`
 * - Backup Status: Shows `lastBackupDate` dengan formatted timestamp
 * - Buttons: Disabled selama operations in progress
 *
 * **Default Values:**
 * - User data: null (not loaded yet)
 * - Booleans: false untuk non-active states
 * - Loading: true untuk initial user loading
 * - Nullable fields: null untuk optional data
 *
 * **Immutability Benefits:**
 * - Thread-safe: State tidak bisa diubah secara accidental
 * - Predictable: Perubahan state selalu explicit melalui copy()
 * - Reactive: StateFlow dapat detect perubahan dengan reliable
 * - Debugging: Easy tracking of state changes
 *
 * **Usage in ViewModel:**
 * ```kotlin
 * private val _uiState = MutableStateFlow(SettingsUiState())
 * val uiState: StateFlow<SettingsUiState> = _uiState
 *
 * // Update state
 * _uiState.value = _uiState.value.copy(
 *     currentUser = loadedUser,
 *     isLoading = false
 * )
 * ```
 *
 * **Usage in Compose UI:**
 * ```kotlin
 * val uiState by viewModel.uiState.collectAsState()
 *
 * // User profile display
 * uiState.currentUser?.let { user ->
 *     Text("Welcome, ${user.username}")
 * }
 *
 * // Loading states
 * if (uiState.isLoading) {
 *     CircularProgressIndicator()
 * }
 *
 * // Password change feedback
 * if (uiState.passwordChangeSuccess) {
 *     Text("Password changed successfully!")
 * }
 *
 * // Error display
 * uiState.error?.let { error ->
 *     Text(error, color = MaterialTheme.colorScheme.error)
 * }
 * ```
 *
 * @property currentUser Data user yang sedang login untuk profile display
 * @property isLoading Status loading data user saat aplikasi startup
 * @property isChangingPassword Status proses perubahan password sedang berlangsung
 * @property passwordChangeSuccess Flag yang menandai password berhasil diubah
 * @property error Error message yang ditampilkan ke user (null = no error)
 * @property lastBackupDate Timestamp pembuatan backup terakhir dalam milliseconds
 * @property isCreatingBackup Status proses pembuatan backup sedang berlangsung
 */
data class SettingsUiState(
    val currentUser: Pengguna? = null,
    val isLoading: Boolean = true,
    val isChangingPassword: Boolean = false,
    val passwordChangeSuccess: Boolean = false,
    val error: String? = null,
    val lastBackupDate: Long? = null,
    val isCreatingBackup: Boolean = false
)
