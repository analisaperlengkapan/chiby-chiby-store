package com.chibychibystore.error

/**
 * Centralized error messages dalam Bahasa Indonesia
 * 
 * Object ini menyediakan user-friendly error messages yang consistent
 * di seluruh aplikasi untuk better UX.
 */
object ErrorMessages {
    
    // Authentication Errors
    const val INVALID_CREDENTIALS = "Username atau password salah"
    const val USER_NOT_FOUND = "Pengguna tidak ditemukan"
    const val SESSION_EXPIRED = "Sesi Anda telah berakhir, silakan login kembali"
    const val UNAUTHORIZED = "Anda tidak memiliki izin untuk melakukan aksi ini"
    const val PASSWORD_TOO_SHORT = "Password minimal 6 karakter"
    const val PASSWORD_MISMATCH = "Password lama tidak sesuai"
    const val USERNAME_REQUIRED = "Username tidak boleh kosong"
    const val PASSWORD_REQUIRED = "Password tidak boleh kosong"
    
    // Product/Inventory Errors
    const val PRODUCT_NOT_FOUND = "Produk tidak ditemukan"
    const val INSUFFICIENT_STOCK = "Stok tidak mencukupi"
    const val INVALID_BARCODE = "Barcode tidak valid"
    const val DUPLICATE_BARCODE = "Barcode sudah digunakan oleh produk lain"
    const val PRODUCT_NAME_REQUIRED = "Nama produk tidak boleh kosong"
    const val INVALID_PRICE = "Harga tidak valid"
    const val PRICE_BELOW_COST = "Harga jual tidak boleh lebih rendah dari harga beli"
    const val NEGATIVE_STOCK = "Stok tidak boleh negatif"
    const val LOW_STOCK_WARNING = "Stok produk ini sudah rendah"
    
    // Sales/Transaction Errors
    const val EMPTY_CART = "Keranjang belanja kosong"
    const val INVALID_QUANTITY = "Jumlah tidak valid"
    const val INVALID_PAYMENT = "Jumlah pembayaran tidak valid"
    const val INSUFFICIENT_PAYMENT = "Pembayaran kurang dari total"
    const val TRANSACTION_FAILED = "Transaksi gagal, silakan coba lagi"
    const val SALE_NOT_FOUND = "Transaksi penjualan tidak ditemukan"
    const val ALREADY_REFUNDED = "Transaksi ini sudah di-refund"
    const val REFUND_NOT_ALLOWED = "Refund tidak diizinkan untuk transaksi ini"
    
    // Warehouse Errors
    const val WAREHOUSE_NOT_FOUND = "Gudang tidak ditemukan"
    const val WAREHOUSE_NAME_REQUIRED = "Nama gudang tidak boleh kosong"
    const val DUPLICATE_WAREHOUSE = "Nama gudang sudah digunakan"
    const val TRANSFER_FAILED = "Transfer stok gagal"
    const val SAME_WAREHOUSE = "Gudang asal dan tujuan tidak boleh sama"
    
    // Category Errors
    const val CATEGORY_NOT_FOUND = "Kategori tidak ditemukan"
    const val CATEGORY_NAME_REQUIRED = "Nama kategori tidak boleh kosong"
    const val DUPLICATE_CATEGORY = "Nama kategori sudah digunakan"
    const val CATEGORY_IN_USE = "Kategori masih digunakan oleh produk"
    
    // User Management Errors
    const val USER_ALREADY_EXISTS = "Username sudah digunakan"
    const val INVALID_ROLE = "Role tidak valid"
    const val CANNOT_DELETE_SELF = "Anda tidak dapat menghapus akun sendiri"
    const val CANNOT_DELETE_OWNER = "Akun owner tidak dapat dihapus"
    const val USER_INACTIVE = "Akun pengguna tidak aktif"
    
    // Expense Errors
    const val EXPENSE_NOT_FOUND = "Pengeluaran tidak ditemukan"
    const val INVALID_AMOUNT = "Jumlah tidak valid"
    const val EXPENSE_DESCRIPTION_REQUIRED = "Deskripsi pengeluaran tidak boleh kosong"
    const val EXPENSE_CATEGORY_REQUIRED = "Kategori pengeluaran tidak boleh kosong"
    
    // Backup/Restore Errors
    const val BACKUP_FAILED = "Backup gagal, silakan coba lagi"
    const val RESTORE_FAILED = "Restore gagal, file backup mungkin rusak"
    const val INVALID_BACKUP_FILE = "File backup tidak valid"
    const val BACKUP_FILE_NOT_FOUND = "File backup tidak ditemukan"
    const val STORAGE_PERMISSION_DENIED = "Izin akses penyimpanan ditolak"
    
    // Printer Errors
    const val PRINTER_NOT_CONNECTED = "Printer tidak terhubung"
    const val PRINTER_ERROR = "Terjadi kesalahan pada printer"
    const val BLUETOOTH_NOT_ENABLED = "Bluetooth tidak aktif"
    const val BLUETOOTH_PERMISSION_DENIED = "Izin Bluetooth ditolak"
    
    // Network/Database Errors
    const val NETWORK_ERROR = "Tidak dapat terhubung ke server"
    const val DATABASE_ERROR = "Terjadi kesalahan database"
    const val TIMEOUT_ERROR = "Koneksi timeout, silakan coba lagi"
    const val UNKNOWN_ERROR = "Terjadi kesalahan yang tidak diketahui"
    
    // Validation Errors
    const val REQUIRED_FIELD = "Field ini wajib diisi"
    const val INVALID_FORMAT = "Format tidak valid"
    const val INVALID_DATE = "Tanggal tidak valid"
    const val DATE_RANGE_INVALID = "Rentang tanggal tidak valid"
    const val FUTURE_DATE_NOT_ALLOWED = "Tanggal tidak boleh di masa depan"
    
    // General Messages
    const val OPERATION_SUCCESS = "Operasi berhasil"
    const val OPERATION_FAILED = "Operasi gagal"
    const val PLEASE_TRY_AGAIN = "Silakan coba lagi"
    const val LOADING = "Memuat data..."
    const val NO_DATA = "Tidak ada data"
    const val CONFIRM_DELETE = "Apakah Anda yakin ingin menghapus?"
    const val CONFIRM_LOGOUT = "Apakah Anda yakin ingin keluar?"
    
    /**
     * Format error message dengan parameter
     */
    fun productNotFound(productName: String) = "Produk '$productName' tidak ditemukan"
    fun insufficientStock(productName: String, available: Int) = 
        "Stok '$productName' tidak mencukupi. Tersedia: $available"
    fun lowStockWarning(productName: String, current: Int, min: Int) = 
        "Stok '$productName' rendah. Saat ini: $current, Minimum: $min"
    fun duplicateEntry(field: String) = "$field sudah digunakan"
    fun fieldRequired(field: String) = "$field tidak boleh kosong"
    fun invalidValue(field: String) = "Nilai $field tidak valid"
    fun operationFailed(operation: String) = "Gagal melakukan $operation"
    fun permissionDenied(action: String) = "Anda tidak memiliki izin untuk $action"
}

/**
 * Extension function untuk convert Exception ke user-friendly message
 */
fun Throwable.toUserMessage(): String {
    return when (this) {
        is ChibyChibyException.ValidationError -> this.message
        is ChibyChibyException.AuthenticationError -> this.message
        is ChibyChibyException.DatabaseError -> ErrorMessages.DATABASE_ERROR
        is ChibyChibyException.BusinessLogicError -> this.message
        is ChibyChibyException.PermissionError -> this.message
        else -> this.message ?: ErrorMessages.UNKNOWN_ERROR
    }
}
