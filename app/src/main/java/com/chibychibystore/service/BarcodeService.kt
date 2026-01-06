package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result

/**
 * Service untuk mengelola operasi barcode
 * Menggunakan ZXing library untuk generate dan validate barcode
 */
interface BarcodeService {

    /**
     * Generate barcode untuk product
     * @param productId ID product
     * @param format Format barcode (EAN_13, CODE_128, QR_CODE, DATA_MATRIX)
     * @param size Ukuran barcode (SMALL, MEDIUM, LARGE, EXTRA_LARGE)
     * @return BarcodeData berisi image data dan metadata
     */
    suspend fun generateBarcode(
        productId: Long,
        format: BarcodeFormat,
        size: LabelSize
    ): Result<BarcodeData>

    /**
     * Generate value barcode baru yang valid (EAN-13)
     * @return String barcode 13 digit
     */
    suspend fun generateNewBarcodeValue(): String

    /**
     * Validate barcode string
     * @param barcode String barcode yang akan divalidasi
     * @return true jika valid, false jika tidak
     */
    fun validateBarcode(barcode: String): Boolean

    /**
     * Get supported barcode formats
     * @return List format barcode yang didukung
     */
    fun getSupportedFormats(): List<BarcodeFormat>

    /**
     * Calculate check digit untuk EAN-13
     * @param code 12-digit code tanpa check digit
     * @return Check digit (0-9)
     */
    fun calculateCheckDigit(code: String): Int

    /**
     * Validate check digit untuk EAN-13
     * @param barcode 13-digit EAN-13 barcode
     * @return true jika check digit valid
     */
    fun validateCheckDigit(barcode: String): Boolean
}

/**
 * Format barcode yang didukung
 */
enum class BarcodeFormat {
    EAN_13,     // Standard retail barcode
    CODE_128,   // General purpose barcode
    QR_CODE,    // 2D barcode untuk data tambahan
    DATA_MATRIX // GS1 DataMatrix untuk healthcare/retail
}

/**
 * Ukuran label barcode
 */
enum class LabelSize(val width: Int, val height: Int) {
    SMALL(20, 10),      // 2x1 cm
    MEDIUM(30, 20),     // 3x2 cm
    LARGE(30, 20),      // 3x2 cm (landscape)
    EXTRA_LARGE(70, 40) // 7x4 cm
}

/**
 * Data barcode yang dihasilkan
 */
data class BarcodeData(
    val format: BarcodeFormat,
    val size: LabelSize,
    val barcodeValue: String,
    val imageData: ByteArray, // PNG image data
    val width: Int,
    val height: Int,
    val productName: String,
    val productPrice: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BarcodeData

        if (format != other.format) return false
        if (size != other.size) return false
        if (barcodeValue != other.barcodeValue) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (productName != other.productName) return false
        if (productPrice != other.productPrice) return false

        return true
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + barcodeValue.hashCode()
        result = 31 * result + imageData.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + productName.hashCode()
        result = 31 * result + productPrice.hashCode()
        return result
    }
}