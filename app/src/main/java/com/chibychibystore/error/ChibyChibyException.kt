package com.chibychibystore.error

/**
 * Base exception class untuk aplikasi Chiby Chiby Store
 */
sealed class ChibyChibyException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Error validasi input data
     */
    class ValidationError(
        field: String,
        message: String
    ) : ChibyChibyException("Validasi gagal untuk $field: $message")

    /**
     * Error database operations
     */
    class DatabaseError(
        operation: String,
        cause: Throwable? = null
    ) : ChibyChibyException("Error database saat $operation", cause)

    /**
     * Error permission/access control
     */
    class PermissionError(
        permission: String
    ) : ChibyChibyException("Tidak memiliki izin: $permission")

    /**
     * Error business logic violations
     */
    class BusinessLogicError(
        rule: String
    ) : ChibyChibyException("Pelanggaran aturan bisnis: $rule")

    /**
     * Error autentikasi
     */
    class AuthenticationError(
        reason: String
    ) : ChibyChibyException("Autentikasi gagal: $reason")

    /**
     * Error jaringan/peripheral
     */
    class NetworkError(
        device: String,
        cause: Throwable? = null
    ) : ChibyChibyException("Error koneksi ke $device", cause)
}