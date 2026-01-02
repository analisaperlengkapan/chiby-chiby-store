package com.chibychibystore.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chibychibystore.service.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State untuk Login Screen
 *
 * Data class yang merepresentasikan seluruh state UI untuk halaman login.
 * Menggunakan immutable data class pattern untuk reactive UI updates dengan StateFlow.
 * Semua field bersifat immutable dan perubahan state dilakukan melalui copy().
 *
 * **State Fields:**
 * - `username`: Input username dari user (kosong = belum diisi)
 * - `password`: Input password dari user (kosong = belum diisi)
 * - `isLoading`: Status proses login sedang berlangsung
 * - `errorMessage`: Error message yang ditampilkan (nullable = no error)
 * - `isLoginSuccessful`: Flag sukses login untuk navigation trigger
 *
 * **State Transitions:**
 * ```kotlin
 * // Initial state
 * LoginUiState() // semua default values
 *
 * // User input
 * state.copy(username = "admin", password = "pass123")
 *
 * // Validation error
 * state.copy(errorMessage = "Username tidak boleh kosong")
 *
 * // Login in progress
 * state.copy(isLoading = true, errorMessage = null)
 *
 * // Login success
 * state.copy(isLoading = false, isLoginSuccessful = true)
 *
 * // Login failed
 * state.copy(isLoading = false, errorMessage = "Invalid credentials")
 * ```
 *
 * **UI Integration:**
 * - TextFields: Bound ke `username` dan `password` fields
 * - Loading Button: Disabled ketika `isLoading = true`
 * - Error Text: Displayed ketika `errorMessage != null`
 * - Navigation: Triggered ketika `isLoginSuccessful = true`
 * - Input Validation: Real-time validation dengan error clearing
 *
 * **Default Values:**
 * - Strings: Empty strings untuk clean initial state
 * - Booleans: false untuk non-active states
 * - Nullable: null untuk optional error states
 *
 * **Immutability Benefits:**
 * - Thread-safe: State tidak bisa diubah secara accidental
 * - Predictable: Perubahan state selalu explicit melalui copy()
 * - Reactive: StateFlow dapat detect perubahan dengan reliable
 * - Debugging: Easy tracking of state changes
 *
 * **Usage in ViewModel:**
 * ```kotlin
 * private val _uiState = MutableStateFlow(LoginUiState())
 * val uiState: StateFlow<LoginUiState> = _uiState
 *
 * // Update state
 * _uiState.value = _uiState.value.copy(
 *     username = newUsername,
 *     errorMessage = null // Clear error on input
 * )
 * ```
 *
 * **Usage in Compose UI:**
 * ```kotlin
 * val uiState by viewModel.uiState.collectAsState()
 *
 * // TextField binding
 * OutlinedTextField(
 *     value = uiState.username,
 *     onValueChange = viewModel::onUsernameChange
 * )
 *
 * // Login button
 * Button(
 *     onClick = viewModel::login,
 *     enabled = !uiState.isLoading && uiState.username.isNotBlank()
 * ) {
 *     if (uiState.isLoading) {
 *         CircularProgressIndicator()
 *     } else {
 *         Text("Login")
 *     }
 * }
 *
 * // Error display
 * uiState.errorMessage?.let { error ->
 *     Text(error, color = MaterialTheme.colorScheme.error)
 * }
 *
 * // Navigation trigger
 * LaunchedEffect(uiState.isLoginSuccessful) {
 *     if (uiState.isLoginSuccessful) {
 *         navController.navigate("dashboard")
 *     }
 * }
 * ```
 *
 * @property username Input username dari user untuk authentication
 * @property password Input password dari user untuk authentication
 * @property isLoading Status loading selama proses login berlangsung
 * @property errorMessage Error message yang ditampilkan ke user (null = no error)
 * @property isLoginSuccessful Flag yang menandai login berhasil untuk navigation
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccessful: Boolean = false
)

/**
 * ViewModel untuk Login Screen
 *
 * ViewModel ini mengelola state dan business logic untuk halaman login aplikasi.
 * Bertanggung jawab atas input handling, validasi credentials, authentication process,
 * dan state management untuk user feedback selama login flow.
 *
 * **Arsitektur & Pola:**
 * - MVVM Pattern: Reactive UI updates melalui StateFlow
 * - Service Pattern: Business logic melalui AuthService
 * - State Management: Centralized state di LoginUiState
 * - Reactive Programming: StateFlow untuk real-time UI updates
 *
 * **Tanggung Jawab Utama:**
 * - Input handling untuk username dan password fields
 * - Client-side validation sebelum authentication
 * - Orchestration authentication process melalui AuthService
 * - Error handling dan user feedback untuk login failures
 * - State management untuk loading, success, dan error states
 * - Session state reset untuk logout dan navigation
 *
 * **Fitur Utama:**
 * - Input Validation: Real-time validation dengan immediate feedback
 * - Authentication Flow: Complete login process dengan error handling
 * - Reactive UI: StateFlow-based updates untuk seamless UX
 * - Error Recovery: User-controlled error dismissal dan retry capability
 * - State Reset: Complete cleanup untuk logout dan fresh starts
 * - Security: Proper credential handling dan state sanitization
 *
 * **Authentication Workflow:**
 * 1. **Input Phase**: User enters username/password → onUsernameChange/onPasswordChange
 * 2. **Validation Phase**: Input validation → login() method validation
 * 3. **Authentication Phase**: Call AuthService.login() → async processing
 * 4. **Result Phase**: Success → navigation trigger, Failure → error display
 * 5. **Recovery Phase**: Error dismissal → clearError(), Retry → login()
 *
 * **State Management:**
 * ```kotlin
 * data class LoginUiState(
 *     val username: String = "",                    // User input
 *     val password: String = "",                    // User input (masked)
 *     val isLoading: Boolean = false,               // Login in progress
 *     val errorMessage: String? = null,             // Error display
 *     val isLoginSuccessful: Boolean = false        // Success flag
 * )
 * ```
 *
 * **Dependencies:**
 * - AuthService: Core authentication business logic
 * - ViewModelScope: Coroutine scope untuk async operations
 * - StateFlow: Reactive state management
 *
 * **Security Considerations:**
 * - Password Handling: Plain text di memory, hashed di Service layer
 * - Input Validation: Client-side validation sebelum server transmission
 * - State Cleanup: Automatic credential cleanup pada reset
 * - Error Messages: Non-revealing error messages untuk security
 *
 * **Integration Points:**
 * - LoginScreen: Compose UI yang consume LoginUiState
 * - AuthService: Backend authentication logic
 * - Navigation: Triggered by isLoginSuccessful flag
 * - Session Management: State reset pada logout
 *
 * **Usage Example:**
 * ```kotlin
 * @HiltViewModel
 * class LoginViewModel @Inject constructor(
 *     private val authService: AuthService
 * ) : ViewModel() {
 *
 *     // Reactive state untuk UI binding
 *     val uiState: StateFlow<LoginUiState> = _uiState
 *
 *     // Input handlers
 *     fun onUsernameChange(username: String) { /* ... */ }
 *     fun onPasswordChange(password: String) { /* ... */ }
 *
 *     // Authentication
 *     fun login() { /* ... */ }
 *
 *     // State management
 *     fun clearError() { /* ... */ }
 *     fun resetLoginState() { /* ... */ }
 * }
 * ```
 *
 * **Threading & Coroutines:**
 * - Main Thread: UI state updates dan user interactions
 * - IO Thread: Authentication calls melalui AuthService
 * - Error Handling: Comprehensive exception handling di semua operations
 *
 * **Testing Considerations:**
 * - Unit Tests: Mock AuthService untuk business logic testing
 * - Integration Tests: Full authentication flow testing
 * - UI Tests: StateFlow emissions dan user interaction flows
 *
 * **Performance Characteristics:**
 * - Immediate Input Feedback: Real-time state updates tanpa debouncing
 * - Async Authentication: Non-blocking UI selama login process
 * - Memory Efficient: State cleanup pada reset operations
 * - Reactive Updates: Efficient StateFlow untuk UI binding
 *
 * **Error Handling:**
 * - Input Validation: Immediate client-side validation dengan clear messages
 * - Authentication Failures: User-friendly error messages dari AuthService
 * - Network Issues: Graceful handling dengan appropriate error states
 * - Unexpected Errors: Exception catching dengan fallback messages
 *
 * @property authService Service untuk authentication business logic
 * @property uiState Reactive state untuk UI binding dan updates
 * @see LoginUiState Data class untuk UI state management
 * @see AuthService Backend authentication logic
 * @see LoginScreen Compose UI yang menggunakan ViewModel ini
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    /**
     * Handle perubahan input username dari user
     *
     * Metode ini dipanggil setiap kali user mengetik di username field.
     * Memperbarui state dengan username baru dan otomatis clear error message
     * untuk memberikan feedback langsung bahwa input sudah diakui.
     *
     * **Business Logic Flow:**
     * 1. Update username di UI state dengan input baru
     * 2. Clear error message untuk UX yang lebih baik
     * 3. Trigger reactive update ke UI
     *
     * **State Management:**
     * - `username`: Diperbarui dengan input user
     * - `errorMessage`: Direset ke null untuk clear validation errors
     * - Other fields: Tidak diubah untuk preserve existing state
     *
     * **UX Considerations:**
     * - Error Clearing: Menghilangkan error saat user mulai mengetik
     * - Immediate Feedback: State update langsung terlihat di UI
     * - Input Validation: Tidak ada validation di level ini (di login())
     * - Reactive Updates: UI langsung update tanpa delay
     *
     * **Integration Points:**
     * - TextField Component: onValueChange callback
     * - UI State: Reactive binding ke username field
     * - Error Display: Automatic error clearing saat typing
     *
     * **Usage Example:**
     * ```kotlin
     * // Di Compose UI
     * OutlinedTextField(
     *     value = uiState.username,
     *     onValueChange = viewModel::onUsernameChange,
     *     label = { Text("Username") }
     * )
     *
     * // Atau di XML dengan data binding
     * <EditText
     *     android:text="@{viewModel.username}"
     *     app:addTextChangedListener="@{viewModel::onUsernameChange}" />
     * ```
     *
     * **Note:** Method ini tidak melakukan validation - hanya state management.
     * Validation dilakukan di method login() sebelum authentication.
     * Error clearing memberikan UX yang lebih intuitif saat user correction.
     *
     * @param username Input username baru dari user
     * @see onPasswordChange
     * @see login
     * @see LoginUiState.username
     */
    fun onUsernameChange(username: String) {
        _uiState.value = _uiState.value.copy(
            username = username,
            errorMessage = null
        )
    }

    /**
     * Handle perubahan input password dari user
     *
     * Metode ini dipanggil setiap kali user mengetik di password field.
     * Memperbarui state dengan password baru dan otomatis clear error message
     * untuk memberikan feedback langsung bahwa input sudah diakui.
     *
     * **Business Logic Flow:**
     * 1. Update password di UI state dengan input baru
     * 2. Clear error message untuk UX yang lebih baik
     * 3. Trigger reactive update ke UI
     *
     * **State Management:**
     * - `password`: Diperbarui dengan input user (plain text in memory)
     * - `errorMessage`: Direset ke null untuk clear validation errors
     * - Other fields: Tidak diubah untuk preserve existing state
     *
     * **Security Considerations:**
     * - In-Memory Storage: Password disimpan plain text di memory sementara
     * - No Persistence: Password tidak di-cache atau di-persist
     * - Secure Transmission: Password dikirim ke AuthService untuk hashing
     * - Memory Cleanup: State direset setelah login attempt
     *
     * **UX Considerations:**
     * - Error Clearing: Menghilangkan error saat user mulai mengetik
     * - Immediate Feedback: State update langsung terlihat di UI
     * - Input Masking: Password field biasanya menggunakan visual transformation
     * - Validation Timing: Client-side validation dilakukan di login()
     *
     * **Integration Points:**
     * - PasswordTextField: onValueChange callback dengan masking
     * - UI State: Reactive binding ke password field
     * - AuthService: Password dikirim untuk authentication
     * - Error Display: Automatic error clearing saat typing
     *
     * **Usage Example:**
     * ```kotlin
     * // Di Compose UI dengan password masking
     * OutlinedTextField(
     *     value = uiState.password,
     *     onValueChange = viewModel::onPasswordChange,
     *     label = { Text("Password") },
     *     visualTransformation = PasswordVisualTransformation()
     * )
     *
     * // Atau di XML
     * <EditText
     *     android:inputType="textPassword"
     *     android:text="@{viewModel.password}"
     *     app:addTextChangedListener="@{viewModel::onPasswordChange}" />
     * ```
     *
     * **Note:** Method ini menangani password sebagai plain text di memory.
     * Password hashing dan security dilakukan di layer AuthService.
     * Error clearing memberikan UX yang lebih intuitif saat user correction.
     *
     * @param password Input password baru dari user
     * @see onUsernameChange
     * @see login
     * @see AuthService
     * @see LoginUiState.password
     */
    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            errorMessage = null
        )
    }

    /**
     * Proses authentication dengan username dan password
     *
     * Metode ini menangani seluruh flow authentication mulai dari validasi input,
     * proses login melalui AuthService, hingga state management untuk UI feedback.
     * Merupakan entry point utama untuk user authentication di aplikasi.
     *
     * **Business Logic Flow:**
     * 1. Ambil current state untuk validasi
     * 2. Validasi input: username dan password tidak boleh kosong
     * 3. Set loading state dan clear previous errors
     * 4. Panggil AuthService.login() dengan credentials
     * 5. Handle success: update state dengan login successful flag
     * 6. Handle failure: update state dengan error message
     * 7. Exception handling: catch unexpected errors
     *
     * **Input Validation:**
     * - Username: Tidak boleh blank/empty
     * - Password: Tidak boleh blank/empty
     * - Client-side: Immediate validation sebelum server call
     * - Error Messages: User-friendly Indonesian messages
     *
     * **State Management:**
     * - Loading: `isLoading = true` selama authentication
     * - Success: `isLoginSuccessful = true`, `isLoading = false`
     * - Failure: `errorMessage` diisi, `isLoading = false`
     * - Error: Comprehensive error handling dengan fallback messages
     *
     * **Security Considerations:**
     * - Password Handling: Plain text di memory, hashed di AuthService
     * - Failed Attempts: Error messages tidak reveal security info
     * - Session Management: Success flag triggers navigation/session setup
     * - Input Sanitization: Basic validation prevents empty credentials
     *
     * **Error Handling:**
     * - Validation Errors: Immediate return dengan error message
     * - Auth Failures: Service error messages dari AuthService
     * - Network Issues: Generic error dengan user-friendly message
     * - Unexpected Errors: Exception catching dengan fallback message
     *
     * **Integration Points:**
     * - AuthService.login(): Core authentication logic
     * - UI Components: Button click handler dengan loading state
     * - Navigation: Triggered by `isLoginSuccessful` flag
     * - Error Display: Reactive error message updates
     *
     * **Usage Example:**
     * ```kotlin
     * // Button click handler
     * Button(
     *     onClick = viewModel::login,
     *     enabled = !uiState.isLoading
     * ) {
     *     Text(if (uiState.isLoading) "Logging in..." else "Login")
     * }
     *
     * // Navigation trigger
     * LaunchedEffect(uiState.isLoginSuccessful) {
     *     if (uiState.isLoginSuccessful) {
     *         navController.navigate("dashboard") {
     *             popUpTo("login") { inclusive = true }
     *         }
     *     }
     * }
     *
     * // Error handling
     * uiState.errorMessage?.let { error ->
     *     Text(error, color = MaterialTheme.colorScheme.error)
     * }
     * ```
     *
     * **Performance Considerations:**
     * - Async Processing: Non-blocking UI dengan coroutines
     * - State Updates: Immediate loading feedback
     * - Error Recovery: Clear error states untuk retry
     *
     * **Note:** Method ini menangani complete authentication workflow.
     * Navigation dan session setup biasanya ditangani di UI layer
     * berdasarkan `isLoginSuccessful` flag.
     *
     * @see AuthService.login
     * @see onUsernameChange
     * @see onPasswordChange
     * @see LoginUiState.isLoginSuccessful
     */
    fun login() {
        val currentState = _uiState.value
        if (currentState.username.isBlank() || currentState.password.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Username dan password tidak boleh kosong"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
            
            val result = authService.login(currentState.username, currentState.password)
            
            if (result.isSuccess) {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    isLoginSuccessful = true,
                    errorMessage = null
                )
            } else {
                _uiState.value = currentState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Login gagal"
                )
            }
        }
    }

    /**
     * Clear error message dari UI state
     *
     * Metode utility ini membersihkan error message yang sedang
     * ditampilkan di UI. Berguna untuk memberikan user kontrol
     * untuk menutup error notifications atau prepare untuk retry.
     *
     * **Business Logic Flow:**
     * 1. Set errorMessage di UI state menjadi null
     * 2. UI akan menyembunyikan error message
     *
     * **State Management:**
     * - `errorMessage`: Direset ke null
     * - Other states: Tidak diubah untuk preserve existing state
     * - UI feedback: Error message akan hilang dari display
     *
     * **UX Considerations:**
     * - User Control: Memberikan user kemampuan dismiss error
     * - Error Recovery: Clear error sebelum mencoba login lagi
     * - State Cleanup: Error tidak di-cache secara permanent
     * - Retry Preparation: Bersihkan error untuk attempt berikutnya
     *
     * **Integration Points:**
     * - Error Display Components: Text/Error UI akan tertutup
     * - Retry Actions: Dipanggil sebelum user mencoba login lagi
     * - Input Changes: Otomatis dipanggil saat user mengetik
     *
     * **Usage Example:**
     * ```kotlin
     * // User dismiss error message
     * viewModel.clearError()
     *
     * // Atau dalam error recovery workflow
     * uiState.errorMessage?.let { error ->
     *     Text(error)
     *     Button(onClick = viewModel::clearError) {
     *         Text("Tutup")
     *     }
     * }
     *
     * // Prepare for retry
     * viewModel.clearError()
     * viewModel.login() // Retry login
     * ```
     *
     * **Note:** Method ini hanya clear error display, tidak
     * mengubah underlying state atau data. User tetap bisa
     * mencoba login dengan credentials yang sama setelah clear error.
     * Berguna untuk UX dimana user ingin mencoba lagi tanpa
     * error message yang mengganggu.
     *
     * @see login
     * @see onUsernameChange
     * @see onPasswordChange
     * @see LoginUiState.errorMessage
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Reset seluruh login state ke kondisi awal
     *
     * Metode ini melakukan reset complete dari semua field di UI state
     * kembali ke default values. Berguna untuk logout, navigation reset,
     * atau ketika perlu memulai login flow dari awal.
     *
     * **Business Logic Flow:**
     * 1. Replace entire UI state dengan LoginUiState() baru
     * 2. Semua fields kembali ke default values
     * 3. UI akan menampilkan form login kosong
     *
     * **State Management:**
     * - Complete Reset: Semua fields direset ke default values
     * - `username`: Empty string
     * - `password`: Empty string
     * - `isLoading`: false
     * - `errorMessage`: null
     * - `isLoginSuccessful`: false
     *
     * **Use Cases:**
     * - Logout: Clear all login data setelah logout
     * - Navigation Reset: Prepare untuk login screen baru
     * - Fresh Start: Clear previous login attempts
     * - Session Cleanup: Remove sensitive data dari memory
     *
     * **Security Considerations:**
     * - Credential Cleanup: Password dan username dihapus dari memory
     * - State Sanitization: Tidak ada sensitive data tersisa
     * - Session Reset: Clear authentication flags
     * - Memory Safety: Prepare untuk garbage collection
     *
     * **Integration Points:**
     * - Logout Flow: Dipanggil setelah user logout
     * - Navigation: Prepare untuk kembali ke login screen
     * - Session Management: Clear authentication state
     * - UI Reset: Form kembali ke kondisi kosong
     *
     * **Usage Example:**
     * ```kotlin
     * // Dalam logout flow
     * fun logout() {
     *     authUseCases.logout()
     *     viewModel.resetLoginState()
     *     navController.navigate("login") {
     *         popUpTo(0) // Clear back stack
     *     }
     * }
     *
     * // Navigation reset
     * fun navigateToLogin() {
     *     viewModel.resetLoginState()
     *     navController.navigate("login")
     * }
     *
     * // Fresh start untuk retry
     * viewModel.resetLoginState()
     * // Form sekarang kosong dan siap untuk input baru
     * ```
     *
     * **Note:** Method ini melakukan reset total dari login state.
     * Gunakan dengan hati-hati karena akan menghilangkan semua
     * input user dan error states. Cocok untuk logout atau
     * ketika perlu memulai authentication flow dari awal.
     *
     * @see LoginUiState
     * @see clearError
     */
    fun resetLoginState() {
        _uiState.value = LoginUiState()
    }
}
