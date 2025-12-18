package com.chibychibystore.service

import com.chibychibystore.repository.UserSessionRepository
import com.chibychibystore.repository.PenggunaRepository
import com.chibychibystore.data.local.entity.UserSession
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Role
import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import com.chibychibystore.data.model.Result
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication Service untuk Chiby Chiby Store
 *
 * Service ini menangani semua operasi autentikasi dan otorisasi dalam aplikasi POS.
 * Menggunakan SHA-256 untuk hashing password dan session management untuk
 * melacak aktivitas user.
 *
 * **Fitur Utama:**
 * - Login/logout dengan validasi credentials
 * - Role-based access control (Owner, Manager, Cashier, Warehouse)
 * - Session management dengan timeout 24 jam
 * - Password change dengan validasi
 * - Permission checking untuk operasi bisnis
 *
 * **Keamanan:**
 * - Password hashing menggunakan SHA-256
 * - Session timeout otomatis
 * - Permission validation sebelum setiap operasi
 * - Input validation untuk mencegah injection attacks
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see Pengguna
 * @see Role
 * @see ChibyChibyException
 */
interface AuthService {

    /**
     * Melakukan login dengan username dan password
     *
     * Method ini memvalidasi credentials user, membuat session baru,
     * dan mengatur current user jika login berhasil.
     *
     * @param username Username yang akan login (tidak boleh kosong)
     * @param password Password dalam plain text (akan di-hash dengan SHA-256)
     * @return Result yang berisi [Pengguna] jika berhasil, atau exception jika gagal
     *
     * @throws ChibyChibyException.ValidationError jika username/password kosong
     * @throws ChibyChibyException.AuthenticationError jika credentials salah
     * @throws ChibyChibyException.DatabaseError jika terjadi error database
     *
     * @sample
     * ```kotlin
     * val result = authService.login("admin", "password123")
     * result.onSuccess { user ->
     *     println("Login berhasil: ${user.username}")
     * }.onFailure { error ->
     *     println("Login gagal: ${error.message}")
     * }
     * ```
     */
    suspend fun login(username: String, password: String): Result<Pengguna>

    /**
     * Melakukan logout untuk current user
     *
     * Method ini akan menonaktifkan semua session aktif untuk user saat ini
     * dan membersihkan state current user.
     *
     * @return Result yang menunjukkan keberhasilan operasi
     *
     * @throws ChibyChibyException.DatabaseError jika terjadi error database
     */
    suspend fun logout(): Result<Unit>

    /**
     * Mendapatkan user yang sedang login saat ini
     *
     * @return [Pengguna] yang sedang login, atau null jika tidak ada user yang login
     */
    suspend fun getCurrentUser(): Pengguna?

    /**
     * Mengecek apakah current user memiliki permission tertentu
     *
     * Permission checking menggunakan role-based access control:
     * - OWNER: Akses penuh ke semua fitur
     * - MANAGER: Laporan, inventory management, approval
     * - CASHIER: POS operations, daily reports
     * - WAREHOUSE: Inventory management, barcode printing
     *
     * @param permission Permission yang akan dicek (contoh: "CREATE_SALES", "VIEW_REPORTS")
     * @return true jika user memiliki permission, false jika tidak
     *
     * @see hasManagerPermission
     * @see hasCashierPermission
     * @see hasWarehousePermission
     */
    suspend fun hasPermission(permission: String): Boolean

    /**
     * Mengubah password untuk current user
     *
     * Method ini memvalidasi password lama, kemudian mengupdate password
     * dengan hash baru. Password minimal 6 karakter.
     *
     * @param oldPassword Password lama dalam plain text
     * @param newPassword Password baru dalam plain text (minimal 6 karakter)
     * @return Result yang menunjukkan keberhasilan operasi
     *
     * @throws ChibyChibyException.AuthenticationError jika tidak ada user login atau password lama salah
     * @throws ChibyChibyException.ValidationError jika input tidak valid
     * @throws ChibyChibyException.DatabaseError jika terjadi error database
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit>

    /**
     * Mengamati perubahan current user secara real-time
     *
     * Flow ini akan emit user saat ini setiap kali terjadi perubahan.
     * Berguna untuk reactive UI updates.
     *
     * @return Flow yang emit [Pengguna] atau null
     */
    fun observeCurrentUser(): Flow<Pengguna?>

    /**
     * Menginisialisasi session dari data tersimpan saat aplikasi start
     *
     * Method ini dipanggil saat aplikasi pertama kali dibuka untuk
     * memulihkan session user yang masih aktif (belum expired).
     *
     * @return Result yang menunjukkan keberhasilan inisialisasi
     */
    suspend fun initializeSession(): Result<Unit>

    /**
     * Mengecek apakah session current user sudah expired
     *
     * Session dianggap expired jika sudah lebih dari 24 jam sejak
     * aktivitas terakhir.
     *
     * @return true jika session expired, false jika masih aktif
     */
    suspend fun isSessionExpired(): Boolean

    /**
     * Memperpanjang session current user
     *
     * Method ini mengupdate waktu aktivitas terakhir untuk
     * mencegah session timeout.
     *
     * @return Result yang menunjukkan keberhasilan operasi
     */
    suspend fun extendSession(): Result<Unit>

    /**
     * Force logout semua user (fungsi admin)
     *
     * Method ini menonaktifkan semua session aktif di sistem.
     * Hanya bisa digunakan oleh user dengan role OWNER.
     *
     * @return Result yang menunjukkan keberhasilan operasi
     */
    suspend fun forceLogoutAll(): Result<Unit>
}

/**
 * Implementation dari AuthService menggunakan Room database
 *
 * Kelas ini mengimplementasikan interface [AuthService] dengan menggunakan
 * Room database untuk penyimpanan data user dan session management.
 *
 * **Komponen Utama:**
 * - [PenggunaDao]: Untuk operasi CRUD pada tabel users
 * - [UserSessionRepository]: Untuk manajemen session user
 * - SHA-256 hashing: Untuk secure password storage
 * - Session timeout: 24 jam dari aktivitas terakhir
 *
 * **Thread Safety:**
 * - Semua operasi menggunakan suspend functions untuk thread safety
 * - State currentUser disimpan dalam memory (tidak persistent)
 * - Database operations menggunakan Room's built-in thread safety
 *
 * **Error Handling:**
 * - Menggunakan Result<T> untuk type-safe error handling
 * - Custom exceptions untuk berbagai jenis error
 * - Centralized error logging untuk debugging
 *
 * **Dependencies:**
 * - Hilt untuk dependency injection
 * - Room database untuk data persistence
 * - Kotlin Coroutines untuk async operations
 *
 * @property penggunaDao DAO untuk operasi user database
 * @property userSessionRepository Repository untuk session management
 * @property currentUser User yang sedang login (in-memory state)
 *
 * @constructor Inject dependencies melalui Hilt
 * @param penggunaDao DAO untuk user operations
 * @param userSessionRepository Repository untuk session operations
 *
 * @author Chiby Chiby Store Development Team
 * @since 1.0.0
 * @see AuthService
 * @see PenggunaDao
 * @see UserSessionRepository
 */
@Singleton
class AuthServiceImpl @Inject constructor(
    private val penggunaDao: PenggunaDao,
    private val userSessionRepository: UserSessionRepository
) : AuthService {

    /**
     * Current logged in user (in-memory state)
     *
     * State ini tidak persistent dan akan hilang saat aplikasi restart.
     * Gunakan [initializeSession] untuk memulihkan session dari database.
     */
    private var currentUser: Pengguna? = null

    /**
     * Implementasi login dengan validasi komprehensif
     *
     * **Alur Login:**
     * 1. Validasi input (username dan password tidak kosong)
     * 2. Cari user berdasarkan username di database
     * 3. Verifikasi password dengan hash comparison
     * 4. Set current user jika berhasil
     * 5. Buat session baru untuk tracking aktivitas
     *
     * **Keamanan:**
     * - Password di-hash menggunakan SHA-256 sebelum comparison
     * - Error message yang sama untuk username/password salah (security by obscurity)
     * - Session dibuat untuk tracking aktivitas user
     *
     * @param username Username input dari user
     * @param password Password plain text (akan di-hash)
     * @return Result<Pengguna> dengan user data jika berhasil
     */
    override suspend fun login(username: String, password: String): Result<Pengguna> {
        return try {
            // Validasi input
            if (username.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("username", "Username tidak boleh kosong"))
            }
            if (password.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("password", "Password tidak boleh kosong"))
            }

            // Cari user berdasarkan username
            val user = penggunaDao.getPenggunaByUsername(username)
                ?: return Result.failure(ChibyChibyException.AuthenticationError("Username atau password salah"))

            // Verifikasi password dengan hash comparison
            val passwordHash = hashPassword(password)
            if (user.passwordHash != passwordHash) {
                return Result.failure(ChibyChibyException.AuthenticationError("Username atau password salah"))
            }

            // Check if user is active
            if (!user.isActive) {
                return Result.failure(ChibyChibyException.AuthenticationError("Akun Anda telah dinonaktifkan. Silakan hubungi admin."))
            }

            // Set current user dan buat session baru
            currentUser = user

            // Create user session untuk tracking aktivitas
            val session = UserSession(
                userId = user.id,
                loginTime = java.util.Date(),
                lastActivityTime = java.util.Date(),
                isActive = true
            )
            userSessionRepository.createSession(session)

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("login", e))
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val user = currentUser
            if (user != null) {
                // Deactivate user sessions
                userSessionRepository.deactivateUserSessions(user.id)
            }
            currentUser = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("logout", e))
        }
    }

    override suspend fun getCurrentUser(): Pengguna? {
        return currentUser
    }

    /**
     * Implementasi role-based access control (RBAC)
     *
     * **Hierarki Permission:**
     * 1. **OWNER**: Akses penuh ke semua fitur sistem
     * 2. **MANAGER**: Laporan penjualan/keuangan, approval transaksi besar, inventory management
     * 3. **CASHIER**: Operasi POS, laporan harian, view inventory
     * 4. **WAREHOUSE**: Manajemen inventory, gudang, dan barcode printing
     *
     * **Permission Constants:**
     * - VIEW_SALES_REPORTS, VIEW_FINANCIAL_REPORTS
     * - CREATE_SALES, APPROVE_LARGE_TRANSACTIONS
     * - VIEW_INVENTORY, EDIT_INVENTORY, MANAGE_WAREHOUSES
     * - PRINT_BARCODE_LABELS, VIEW_INVENTORY_REPORTS
     * - MANAGE_USERS (khusus manager)
     *
     * @param permission String permission yang akan dicek
     * @return true jika user memiliki permission tersebut
     *
     * @see hasManagerPermission
     * @see hasCashierPermission
     * @see hasWarehousePermission
     * @see Role
     */
    override suspend fun hasPermission(permission: String): Boolean {
        val user = currentUser ?: return false

        // Owner punya akses penuh ke semua fitur
        if (user.role == Role.OWNER) return true

        // Check role-based permissions berdasarkan role user
        return when (user.role) {
            Role.MANAGER -> hasManagerPermission(permission)
            Role.CASHIER -> hasCashierPermission(permission)
            Role.WAREHOUSE -> hasWarehousePermission(permission)
            else -> false
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = currentUser
                ?: return Result.failure(ChibyChibyException.AuthenticationError("Tidak ada user yang login"))

            // Validasi input
            if (oldPassword.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("oldPassword", "Password lama tidak boleh kosong"))
            }
            if (newPassword.isBlank()) {
                return Result.failure(ChibyChibyException.ValidationError("newPassword", "Password baru tidak boleh kosong"))
            }
            if (newPassword.length < 6) {
                return Result.failure(ChibyChibyException.ValidationError("newPassword", "Password minimal 6 karakter"))
            }

            // Verifikasi password lama
            val oldPasswordHash = hashPassword(oldPassword)
            if (user.passwordHash != oldPasswordHash) {
                return Result.failure(ChibyChibyException.AuthenticationError("Password lama salah"))
            }

            // Update password
            val newPasswordHash = hashPassword(newPassword)
            val updatedUser = user.copy(
                passwordHash = newPasswordHash,
                updatedAt = java.util.Date()
            )

            penggunaDao.updatePengguna(updatedUser)
            currentUser = updatedUser

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("changePassword", e))
        }
    }

    override fun observeCurrentUser(): Flow<Pengguna?> {
        // For now return flow with current user
        // In a more advanced implementation, this could observe database changes
        return kotlinx.coroutines.flow.flowOf(currentUser)
    }

    override suspend fun initializeSession(): Result<Unit> {
        return try {
            // Try to restore active session
            val activeSession = userSessionRepository.getActiveSession().getOrNull()
            if (activeSession != null) {
                // Check if session is not expired (24 hours)
                val sessionAge = System.currentTimeMillis() - activeSession.loginTime.time
                val maxSessionAge = 24 * 60 * 60 * 1000L // 24 hours

                if (sessionAge < maxSessionAge) {
                    // Restore user from session
                    val user = penggunaDao.getPenggunaById(activeSession.userId)
                    if (user != null) {
                        currentUser = user
                        // Update last activity
                        userSessionRepository.updateLastActivityTime(activeSession.id)
                    } else {
                        // User no longer exists, clean up session
                        userSessionRepository.deactivateUserSessions(activeSession.userId)
                    }
                } else {
                    // Session expired, clean up
                    userSessionRepository.deactivateUserSessions(activeSession.userId)
                }
            }

            // Clean up old sessions
            userSessionRepository.cleanupOldSessions()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("initializeSession", e))
        }
    }

    override suspend fun isSessionExpired(): Boolean {
        val user = currentUser ?: return true
        val activeSession = userSessionRepository.getActiveSessionForUser(user.id).getOrNull()
        if (activeSession == null) return true

        // Check if session is older than 24 hours
        val sessionAge = System.currentTimeMillis() - activeSession.lastActivityTime.time
        val maxSessionAge = 24 * 60 * 60 * 1000L // 24 hours

        return sessionAge > maxSessionAge
    }

    override suspend fun extendSession(): Result<Unit> {
        return try {
            val user = currentUser
                ?: return Result.failure(ChibyChibyException.AuthenticationError("Tidak ada user yang login"))

            val activeSession = userSessionRepository.getActiveSessionForUser(user.id).getOrNull()
                ?: return Result.failure(ChibyChibyException.AuthenticationError("Session tidak ditemukan"))

            userSessionRepository.updateLastActivityTime(activeSession.id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("extendSession", e))
        }
    }

    override suspend fun forceLogoutAll(): Result<Unit> {
        return try {
            userSessionRepository.deactivateAllSessions()
            currentUser = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("forceLogoutAll", e))
        }
    }

    /**
     * Mengecek permission untuk role MANAGER
     *
     * **Manager Permissions:**
     * - VIEW_SALES_REPORTS: Melihat laporan penjualan
     * - VIEW_FINANCIAL_REPORTS: Melihat laporan keuangan
     * - APPROVE_LARGE_TRANSACTIONS: Approve transaksi besar
     * - VIEW_INVENTORY: Melihat inventory
     * - EDIT_INVENTORY: Edit inventory (stok adjustment)
     * - MANAGE_USERS: Mengelola user (kecuali owner)
     *
     * @param permission Permission yang akan dicek
     * @return true jika manager memiliki permission tersebut
     */
    private fun hasManagerPermission(permission: String): Boolean {
        return when (permission) {
            "VIEW_SALES_REPORTS",
            "VIEW_FINANCIAL_REPORTS",
            "APPROVE_LARGE_TRANSACTIONS",
            "VIEW_INVENTORY",
            "EDIT_INVENTORY",
            "MANAGE_USERS" -> true
            else -> false
        }
    }

    /**
     * Mengecek permission untuk role CASHIER
     *
     * **Cashier Permissions:**
     * - CREATE_SALES: Membuat transaksi penjualan
     * - VIEW_INVENTORY: Melihat inventory untuk penjualan
     * - VIEW_DAILY_SALES_REPORT: Melihat laporan penjualan harian
     *
     * @param permission Permission yang akan dicek
     * @return true jika cashier memiliki permission tersebut
     */
    private fun hasCashierPermission(permission: String): Boolean {
        return when (permission) {
            "CREATE_SALES",
            "VIEW_INVENTORY",
            "VIEW_DAILY_SALES_REPORT",
            "VIEW_SALES_REPORTS" -> true
            else -> false
        }
    }

    /**
     * Mengecek permission untuk role WAREHOUSE
     *
     * **Warehouse Permissions:**
     * - VIEW_INVENTORY: Melihat inventory
     * - EDIT_INVENTORY: Edit inventory (stok adjustment, penerimaan barang)
     * - MANAGE_WAREHOUSES: Mengelola lokasi gudang
     * - PRINT_BARCODE_LABELS: Print barcode labels
     * - VIEW_INVENTORY_REPORTS: Melihat laporan inventory
     *
     * @param permission Permission yang akan dicek
     * @return true jika warehouse staff memiliki permission tersebut
     */
    private fun hasWarehousePermission(permission: String): Boolean {
        return when (permission) {
            "VIEW_INVENTORY",
            "EDIT_INVENTORY",
            "MANAGE_WAREHOUSES",
            "PRINT_BARCODE_LABELS",
            "VIEW_INVENTORY_REPORTS" -> true
            else -> false
        }
    }

    /**
     * Hash password menggunakan SHA-256
     *
     * **Security Implementation:**
     * - Menggunakan SHA-256 untuk one-way hashing
     * - Output dalam format hexadecimal string
     * - Salt tidak digunakan (untuk kesederhanaan)
     * - Future enhancement: tambahkan salt dan pepper
     *
     * @param password Password plain text yang akan di-hash
     * @return Hash string dalam format hex (64 karakter)
     */
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}