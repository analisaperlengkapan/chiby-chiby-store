package com.chibychibystore.service

import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.error.ChibyChibyException
import com.chibychibystore.repository.ProdukRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementasi BarcodeService menggunakan ZXing library
 */
@Singleton
class BarcodeServiceImpl @Inject constructor(
    private val produkRepository: ProdukRepository
) : BarcodeService {

    private val writer = MultiFormatWriter()
    private val qrWriter = QRCodeWriter()

    override suspend fun generateBarcode(
        productId: Long,
        format: BarcodeFormat,
        size: LabelSize
    ): Result<BarcodeData> = withContext(Dispatchers.IO) {
        try {
            // Get product data
            val product = produkRepository.getProdukById(productId)
                ?: return@withContext Result.failure(
                    ChibyChibyException.ProductNotFound("Product with ID $productId not found")
                )

            // Validate barcode format for product
            if (product.barcode.isNullOrBlank()) {
                return@withContext Result.failure(
                    ChibyChibyException.ValidationError("Product does not have a barcode assigned")
                )
            }

            // Generate barcode image
            val barcodeValue = product.barcode!!
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
                ChibyChibyException.BarcodeError("Failed to generate barcode: ${e.message}")
            )
        } catch (e: Exception) {
            Result.failure(
                ChibyChibyException.BarcodeError("Unexpected error generating barcode: ${e.message}")
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
     * Generate barcode image using ZXing
     */
    private fun generateBarcodeImage(
        value: String,
        format: BarcodeFormat,
        size: LabelSize
    ): BufferedImage {
        val zxingFormat = when (format) {
            BarcodeFormat.EAN_13 -> BarcodeFormat.EAN_13
            BarcodeFormat.CODE_128 -> BarcodeFormat.CODE_128
            BarcodeFormat.QR_CODE -> BarcodeFormat.QR_CODE
            BarcodeFormat.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
        }

        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val bitMatrix: BitMatrix = if (format == BarcodeFormat.QR_CODE) {
            qrWriter.encode(value, zxingFormat, size.width * 10, size.height * 10, hints)
        } else {
            writer.encode(value, zxingFormat, size.width * 10, size.height * 10, hints)
        }

        return MatrixToImageWriter.toBufferedImage(bitMatrix)
    }

    /**
     * Create labeled barcode with product information
     */
    private fun createLabeledBarcode(
        barcodeImage: BufferedImage,
        product: Produk,
        size: LabelSize
    ): BufferedImage {
        val labelHeight = 60 // Space for text below barcode
        val totalHeight = barcodeImage.height + labelHeight
        val totalWidth = barcodeImage.width

        val labeledImage = BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB)
        val g2d = labeledImage.graphics as Graphics2D

        // Set white background
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, totalWidth, totalHeight)

        // Draw barcode
        g2d.drawImage(barcodeImage, 0, 0, null)

        // Draw product info
        g2d.color = Color.BLACK
        g2d.font = Font("Arial", Font.BOLD, 12)

        // Product name (truncate if too long)
        val displayName = if (product.name.length > 20) {
            product.name.substring(0, 17) + "..."
        } else {
            product.name
        }

        val nameWidth = g2d.fontMetrics.stringWidth(displayName)
        val nameX = (totalWidth - nameWidth) / 2
        g2d.drawString(displayName, nameX, barcodeImage.height + 20)

        // Price
        val priceText = "Rp ${String.format("%,.0f", product.sellingPrice)}"
        val priceWidth = g2d.fontMetrics.stringWidth(priceText)
        val priceX = (totalWidth - priceWidth) / 2
        g2d.drawString(priceText, priceX, barcodeImage.height + 40)

        g2d.dispose()

        return labeledImage
    }

    /**
     * Convert BufferedImage to PNG byte array
     */
    private fun convertToPngBytes(image: BufferedImage): ByteArray {
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", outputStream)
        return outputStream.toByteArray()
    }
}