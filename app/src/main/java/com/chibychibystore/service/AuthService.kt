package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow

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
 * @see com.chibychibystore.data.local.entity.Role
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
