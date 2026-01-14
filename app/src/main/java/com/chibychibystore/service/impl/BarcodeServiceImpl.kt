package com.chibychibystore.service.impl

import android.graphics.Bitmap
import android.graphics.Canvas
import com.chibychibystore.service.BarcodeService
import com.chibychibystore.service.BarcodeFormat
import com.chibychibystore.service.LabelSize
import com.chibychibystore.service.BarcodeData
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.model.Result
import com.chibychibystore.di.IoDispatcher
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.ProdukRepository
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.security.SecureRandom
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementasi BarcodeService menggunakan ZXing library
 */
@Singleton
class BarcodeServiceImpl @Inject constructor(
    private val productRepository: ProdukRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BarcodeService {

    private val writer = MultiFormatWriter()
    private val qrWriter = QRCodeWriter()
    private val secureRandom = SecureRandom()

    override suspend fun generateNewBarcodeValue(): String = withContext(ioDispatcher) {
        // Format: 2 (Internal) + YYMMDD (Date) + XXXXX (Random) + C (Check Digit)
        // Total 13 digits for EAN-13 compatibility
        val prefix = "2" // Internal prefix
        val datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))

        // Retry logic to ensure uniqueness
        var attempt = 0
        val maxAttempts = 10

        while (attempt < maxAttempts) {
            // Generate 5 random digits using SecureRandom
            val randomPart = generateRandom5Digits()
            val codeWithoutCheckDigit = prefix + datePart + randomPart
            val checkDigit = calculateCheckDigit(codeWithoutCheckDigit)
            val fullBarcode = codeWithoutCheckDigit + checkDigit

            // Check for collision
            val existingProduct = productRepository.getProdukByBarcode(fullBarcode)
            if (existingProduct.isFailure) {
                // Not found (Result.Failure means no product found with this barcode, or error)
                // Note: getProdukByBarcode returns Success if found, Failure if not found (per implementation in Repository)
                return@withContext fullBarcode
            }

            attempt++
        }

        throw ChibyChibyException.BusinessLogicError("Gagal generate unique barcode setelah $maxAttempts percobaan")
    }

    override suspend fun generateBarcode(
        productId: Long,
        format: BarcodeFormat,
        size: LabelSize
    ): Result<BarcodeData> = withContext(ioDispatcher) {
        try {
            // Get product data
            val productResult = productRepository.getProdukById(productId)
            val product = productResult.getOrNull() ?: return@withContext Result.failure(
                ChibyChibyException.DatabaseError("Produk dengan ID $productId tidak ditemukan")
            )

            // Validate barcode format for product
            if (product.barcode.isNullOrBlank()) {
                return@withContext Result.failure(
                    ChibyChibyException.ValidationError("barcode", "Produk tidak memiliki barcode")
                )
            }

            // Generate barcode image
            val barcodeValue = product.barcode
            val imageData = generateBarcodeImage(barcodeValue, format, size)

            // Create label with product info
            val labeledImage = createLabeledBarcode(imageData, product, size)

            // Convert to PNG bytes
            val pngData = convertToPngBytes(labeledImage)

            val barcodeData = BarcodeData(
                format = format,
                size = size,
                barcodeValue = barcodeValue,
                imageData = pngData,
                width = labeledImage.width,
                height = labeledImage.height,
                productName = product.name,
                productPrice = product.sellingPrice
            )

            Result.success(barcodeData)

        } catch (e: WriterException) {
            Result.failure(
                ChibyChibyException.BusinessLogicError("Gagal generate barcode: ${e.message}")
            )
        } catch (e: Exception) {
            Result.failure(
                ChibyChibyException.DatabaseError("Gagal generate barcode", e)
            )
        }
    }

    override fun validateBarcode(barcode: String): Boolean {
        return when {
            // EAN-13 validation (13 digits, valid check digit)
            barcode.length == 13 && barcode.all { it.isDigit() } -> validateCheckDigit(barcode)

            // Code 128 validation (variable length, alphanumeric)
            barcode.length in 1..48 -> true // Basic length check

            // QR Code validation (no specific format requirements)
            else -> barcode.isNotBlank()
        }
    }

    override fun getSupportedFormats(): List<BarcodeFormat> {
        return BarcodeFormat.values().toList()
    }

    override fun calculateCheckDigit(code: String): Int {
        if (code.length != 12 || !code.all { it.isDigit() }) {
            throw IllegalArgumentException("Code must be exactly 12 digits")
        }

        // EAN-13 check digit calculation
        // Step 1: Add digits in odd positions (1, 3, 5, 7, 9, 11)
        val oddSum = (0..10 step 2).sumOf { code[it].digitToInt() }

        // Step 2: Multiply odd sum by 3
        val oddSumTimes3 = oddSum * 3

        // Step 3: Add digits in even positions (2, 4, 6, 8, 10, 12)
        val evenSum = (1..11 step 2).sumOf { code[it].digitToInt() }

        // Step 4: Add odd sum * 3 + even sum
        val total = oddSumTimes3 + evenSum

        // Step 5: Find the smallest number >= total that is divisible by 10
        val nextMultipleOf10 = ((total + 9) / 10) * 10

        // Step 6: Subtract total from next multiple of 10
        return nextMultipleOf10 - total
    }

    override fun validateCheckDigit(barcode: String): Boolean {
        if (barcode.length != 13 || !barcode.all { it.isDigit() }) {
            return false
        }

        val code = barcode.substring(0, 12)
        val providedCheckDigit = barcode[12].digitToInt()
        val calculatedCheckDigit = calculateCheckDigit(code)

        return providedCheckDigit == calculatedCheckDigit
    }

    /**
     * Generate 5 random digits using SecureRandom to minimize collision probability
     */
    private fun generateRandom5Digits(): String {
        return (secureRandom.nextInt(90000) + 10000).toString()
    }

    /**
     * Generate barcode image using ZXing
     */
    private fun generateBarcodeImage(
        value: String,
        format: BarcodeFormat,
        size: LabelSize
    ): Bitmap {
        val zxingFormat = when (format) {
            BarcodeFormat.EAN_13 -> com.google.zxing.BarcodeFormat.EAN_13
            BarcodeFormat.CODE_128 -> com.google.zxing.BarcodeFormat.CODE_128
            BarcodeFormat.QR_CODE -> com.google.zxing.BarcodeFormat.QR_CODE
            BarcodeFormat.DATA_MATRIX -> com.google.zxing.BarcodeFormat.DATA_MATRIX
        }

        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val width = size.width * 10
        val height = size.height * 10
        val bitMatrix: BitMatrix = if (format == BarcodeFormat.QR_CODE) {
            qrWriter.encode(value, zxingFormat, width, height, hints)
        } else {
            writer.encode(value, zxingFormat, width, height, hints)
        }

        return bitMatrixToBitmap(bitMatrix)
    }

    /**
     * Convert BitMatrix to Android Bitmap
     */
    private fun bitMatrixToBitmap(bitMatrix: BitMatrix): Bitmap {
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }

        return bitmap
    }

    /**
     * Create labeled barcode with product information
     */
    private fun createLabeledBarcode(
        barcodeImage: Bitmap,
        product: Produk,
        size: LabelSize
    ): Bitmap {
        val labelHeight = 60 // Space for text below barcode
        val totalHeight = barcodeImage.height + labelHeight
        val totalWidth = barcodeImage.width

        val labeledImage = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.RGB_565)
        val canvas = Canvas(labeledImage)

        // Set white background
        canvas.drawColor(Color.WHITE)

        // Draw barcode
        canvas.drawBitmap(barcodeImage, 0f, 0f, null)

        // Draw product info
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Produk name (truncate if too long)
        val displayName = if (product.name.length > 20) {
            product.name.substring(0, 17) + "..."
        } else {
            product.name
        }

        val nameWidth = paint.measureText(displayName)
        val nameX = (totalWidth - nameWidth) / 2
        canvas.drawText(displayName, nameX, (barcodeImage.height + 20).toFloat(), paint)

        // Price
        val priceText = "Rp ${String.format("%,.0f", product.sellingPrice)}"
        val priceWidth = paint.measureText(priceText)
        val priceX = (totalWidth - priceWidth) / 2
        canvas.drawText(priceText, priceX, (barcodeImage.height + 40).toFloat(), paint)

        return labeledImage
    }

    /**
     * Convert Bitmap to PNG byte array
     */
    private fun convertToPngBytes(image: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }
}