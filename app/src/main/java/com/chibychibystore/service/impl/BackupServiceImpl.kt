package com.chibychibystore.service.impl

import android.content.Context
import android.os.Environment
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.chibychibystore.data.backup.BackupData
import com.chibychibystore.data.backup.BackupEntities
import com.chibychibystore.data.backup.BackupMetadata
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.*
import com.chibychibystore.service.BackupInfo
import com.chibychibystore.service.BackupProgress
import com.chibychibystore.service.BackupService
import com.chibychibystore.service.BackupValidationResult
import com.chibychibystore.di.IoDispatcher
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.security.DigestOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation BackupService menggunakan repository
 */
@Singleton
class BackupServiceImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
<<<<<<< HEAD
    private val userRepository: PenggunaRepository,
    private val categoryRepository: KategoriRepository,
    private val warehouseRepository: GudangRepository,
    private val productRepository: ProdukRepository,
    private val supplierRepository: PemasokRepository,
    private val saleRepository: PenjualanRepository,
    private val itemPenjualanRepository: ItemPenjualanRepository,
    private val purchaseRepository: PembelianRepository,
    private val itemPembelianRepository: ItemPembelianRepository,
    private val expenseRepository: PengeluaranRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
=======
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val warehouseRepository: WarehouseRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val saleRepository: SaleRepository,
    private val saleItemRepository: SaleItemRepository,
    private val purchaseRepository: PurchaseRepository,
    private val purchaseItemRepository: PurchaseItemRepository,
    private val expenseRepository: ExpenseRepository
>>>>>>> feat/ui-overhaul
) : BackupService {

    private val json = Json { prettyPrint = true }
    private val jsonCompact = Json { prettyPrint = false }
    private val masterKey by lazy {
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }
    private val _backupProgress = MutableStateFlow(BackupProgress())
    override fun observeBackupProgress(): StateFlow<BackupProgress> = _backupProgress

    override suspend fun createBackup(): Result<BackupInfo> = withContext(ioDispatcher) {
        var tempFile: File? = null
        try {
            _backupProgress.value = BackupProgress(isInProgress = true, totalSteps = 10)

<<<<<<< HEAD
            val createdAt = System.currentTimeMillis()
            tempFile = File.createTempFile("backup_data_part", ".json", context.cacheDir)
=======
            // Step 1: Gather all data
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data pengguna", progress = 0.1f, currentStepIndex = 1, totalSteps = 10)
            val pengguna = userRepository.getAllUsers().firstOrNull() ?: emptyList()

            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data kategori", progress = 0.2f, currentStepIndex = 2, totalSteps = 10)
            val kategori = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()

            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data gudang", progress = 0.3f, currentStepIndex = 3, totalSteps = 10)
            val gudang = warehouseRepository.getAllWarehouses().firstOrNull() ?: emptyList()

            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data product", progress = 0.4f, currentStepIndex = 4, totalSteps = 10)
            val product = productRepository.getAllProducts().firstOrNull() ?: emptyList()

            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data pemasok", progress = 0.5f, currentStepIndex = 5, totalSteps = 10)
            val pemasok = supplierRepository.getAllSuppliers().firstOrNull() ?: emptyList()

            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data penjualan", progress = 0.6f, currentStepIndex = 6, totalSteps = 10)
            val sales = saleRepository.getAllSales().firstOrNull() ?: emptyList()

            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan item penjualan", progress = 0.7f, currentStepIndex = 7, totalSteps = 10)
            val saleItems = saleItemRepository.getAllSaleItems().firstOrNull() ?: emptyList()

            // Note: Need to fix pembelian repository reference
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data pembelian", progress = 0.8f, currentStepIndex = 8, totalSteps = 10)
            val purchases = purchaseRepository.getAllPurchases().firstOrNull() ?: emptyList()

            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan item pembelian", progress = 0.9f, currentStepIndex = 9, totalSteps = 10)
            val purchaseItems = purchaseItemRepository.getAllPurchaseItems().firstOrNull() ?: emptyList()

            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data pengeluaran", progress = 1.0f, currentStepIndex = 10, totalSteps = 10)
            val expenses = expenseRepository.getAllExpenses().firstOrNull() ?: emptyList()

            // Step 2: Create backup data structure
            val createdAt = System.currentTimeMillis()
            val entities = BackupEntities(
                users = pengguna,
                categories = kategori,
                warehouses = gudang,
                products = product,
                suppliers = pemasok,
                sales = sales,
                saleItems = saleItems,
                purchases = purchases,
                purchaseItems = purchaseItems,
                expenses = expenses
            )
>>>>>>> feat/ui-overhaul

            // 1. Prepare Checksum Calculation
            val digest = MessageDigest.getInstance("SHA-256")

            // 2. Construct the Prefix (Header) with empty checksum
            // Structure: {"version":"1.0","createdAt":123,"metadata":{"...":"...","checksum":""},"data":
            // We use kotlinx to ensure exact formatting of the object part, then manually append "data":
            val initialMetadata = BackupMetadata(checksum = "")
            val headerJsonObject = buildJsonObject {
                put("version", "1.0")
                put("createdAt", createdAt)
                put("metadata", jsonCompact.encodeToJsonElement(initialMetadata))
            }
            val headerStringFull = jsonCompact.encodeToString(headerJsonObject)
            // Strip the last '}' and append ',"data":'
            val prefixString = headerStringFull.substring(0, headerStringFull.length - 1) + ",\"data\":"
            val prefixBytes = prefixString.toByteArray(Charsets.UTF_8)

            // Update digest with prefix
            digest.update(prefixBytes)

            // 3. Write Data Body to Temp File AND Digest
            val tempFos = BufferedOutputStream(FileOutputStream(tempFile))
            val dos = DigestOutputStream(tempFos, digest)
            val writer = JsonWriter(OutputStreamWriter(dos, "UTF-8"))

            // Start the "data" object
            writer.beginObject()

            // Step 1: Users
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data pengguna", progress = 0.1f, currentStepIndex = 1, totalSteps = 10)
            writer.name("users")
            writer.beginArray()
            val users = userRepository.getAllUsers().firstOrNull() ?: emptyList()
            users.forEach { writer.jsonValue(jsonCompact.encodeToString(it)) }
            writer.endArray()
            writer.flush()

            // Step 2: Categories
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data kategori", progress = 0.2f, currentStepIndex = 2, totalSteps = 10)
            writer.name("categories")
            writer.beginArray()
            val categories = categoryRepository.getAllKategori().firstOrNull() ?: emptyList()
            categories.forEach { writer.jsonValue(jsonCompact.encodeToString(it)) }
            writer.endArray()
            writer.flush()

            // Step 3: Warehouses
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data gudang", progress = 0.3f, currentStepIndex = 3, totalSteps = 10)
            writer.name("warehouses")
            writer.beginArray()
            val warehouses = warehouseRepository.getAllGudang().firstOrNull() ?: emptyList()
            warehouses.forEach { writer.jsonValue(jsonCompact.encodeToString(it)) }
            writer.endArray()
            writer.flush()

            // Step 4: Products
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data product", progress = 0.4f, currentStepIndex = 4, totalSteps = 10)
            writer.name("products")
            writer.beginArray()
            val products = productRepository.getAllProduk().firstOrNull() ?: emptyList()
            products.forEach { writer.jsonValue(jsonCompact.encodeToString(it)) }
            writer.endArray()
            writer.flush()

            // Step 5: Suppliers
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data pemasok", progress = 0.5f, currentStepIndex = 5, totalSteps = 10)
            writer.name("suppliers")
            writer.beginArray()
            val suppliers = supplierRepository.getAllPemasok().firstOrNull() ?: emptyList()
            suppliers.forEach { writer.jsonValue(jsonCompact.encodeToString(it)) }
            writer.endArray()
            writer.flush()

            // Step 6: Sales
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data penjualan", progress = 0.6f, currentStepIndex = 6, totalSteps = 10)
            writer.name("sales")
            writer.beginArray()
            val sales = saleRepository.getAllPenjualan().firstOrNull() ?: emptyList()
            sales.forEach { writer.jsonValue(jsonCompact.encodeToString(it)) }
            writer.endArray()
            writer.flush()

            // Step 7: Sale Items
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan item penjualan", progress = 0.7f, currentStepIndex = 7, totalSteps = 10)
            writer.name("saleItems")
            writer.beginArray()
            val saleItems = itemPenjualanRepository.getAllSaleItems().firstOrNull() ?: emptyList()
            saleItems.forEach { writer.jsonValue(jsonCompact.encodeToString(it)) }
            writer.endArray()
            writer.flush()

            // Step 8: Purchases
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data pembelian", progress = 0.8f, currentStepIndex = 8, totalSteps = 10)
            writer.name("purchases")
            writer.beginArray()
            val purchases = purchaseRepository.getAllPurchases().firstOrNull() ?: emptyList()
            purchases.forEach { writer.jsonValue(jsonCompact.encodeToString(it)) }
            writer.endArray()
            writer.flush()

            // Step 9: Purchase Items
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan item pembelian", progress = 0.9f, currentStepIndex = 9, totalSteps = 10)
            writer.name("purchaseItems")
            writer.beginArray()
            val purchaseItems = itemPembelianRepository.getAllPurchaseItems().firstOrNull() ?: emptyList()
            purchaseItems.forEach { writer.jsonValue(jsonCompact.encodeToString(it)) }
            writer.endArray()
            writer.flush()

            // Step 10: Expenses
            _backupProgress.value = BackupProgress(isInProgress = true, currentStep = "Mengumpulkan data pengeluaran", progress = 1.0f, currentStepIndex = 10, totalSteps = 10)
            writer.name("expenses")
            writer.beginArray()
            val expenses = expenseRepository.getAllPengeluarans().firstOrNull() ?: emptyList()
            expenses.forEach { writer.jsonValue(jsonCompact.encodeToString(it)) }
            writer.endArray()

            // End "data" object
            writer.endObject()

            writer.flush()
            writer.close() // Closes dos and tempFos

            // 4. Update Digest with Suffix
            // The root object closing brace "}"
            val suffixString = "}"
            val suffixBytes = suffixString.toByteArray(Charsets.UTF_8)
            digest.update(suffixBytes)

            // Calculate Checksum
            val checksum = digest.digest().joinToString("") { "%02x".format(it) }

            // 5. Construct Final File
            val fileName = generateBackupFileName(createdAt)
            val filePath = createBackupFile(fileName)
            val file = File(filePath)
            val outputStream = BufferedOutputStream(openBackupOutputStream(file))

            // Re-construct prefix with REAL checksum
            val finalMetadata = initialMetadata.copy(checksum = checksum)
            val finalHeaderJsonObject = buildJsonObject {
                put("version", "1.0")
                put("createdAt", createdAt)
                put("metadata", jsonCompact.encodeToJsonElement(finalMetadata))
            }
            val finalHeaderStringFull = jsonCompact.encodeToString(finalHeaderJsonObject)
            val finalPrefixString = finalHeaderStringFull.substring(0, finalHeaderStringFull.length - 1) + ",\"data\":"
            val finalPrefixBytes = finalPrefixString.toByteArray(Charsets.UTF_8)

            outputStream.use { out ->
                // Write final prefix
                out.write(finalPrefixBytes)

                // Copy body from temp file
                BufferedInputStream(FileInputStream(tempFile)).use { input ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                }

                // Write suffix
                out.write(suffixBytes)
            }

            _backupProgress.value = BackupProgress(isInProgress = false)

            val backupInfo = BackupInfo(
                id = checksum,
                fileName = fileName,
                filePath = filePath,
                createdAt = createdAt,
                sizeBytes = file.length(),
                version = "1.0",
                checksum = checksum
            )

            // Cleanup
            tempFile.delete()

            Result.success(backupInfo)

        } catch (e: Exception) {
            tempFile?.delete()
            _backupProgress.value = BackupProgress(isInProgress = false)
            Result.failure(e)
        }
    }

    override suspend fun getBackupHistory(): Result<List<BackupInfo>> = withContext(ioDispatcher) {
        try {
            val backupDir = getBackupDirectory()
            val backupFiles = backupDir.listFiles { file ->
                file.name.startsWith("backup_") && file.name.endsWith(".enc")
            } ?: emptyArray()

            val backupInfos = backupFiles.mapNotNull { file ->
                try {
                    // Extract timestamp from filename
                    val timestamp = extractTimestampFromFileName(file.name)
                    val checksum = calculateFileChecksum(file)

                    BackupInfo(
                        id = checksum,
                        fileName = file.name,
                        filePath = file.absolutePath,
                        createdAt = timestamp,
                        sizeBytes = file.length(),
                        checksum = checksum
                    )
                } catch (e: Exception) {
                    null // Skip invalid files
                }
            }.sortedByDescending { it.createdAt }

            Result.success(backupInfos)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteBackup(backupId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val backupDir = getBackupDirectory()
            val backupFile = backupDir.listFiles { file ->
                calculateFileChecksum(file) == backupId
            }?.firstOrNull()

            if (backupFile != null && backupFile.exists()) {
                backupFile.delete()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Backup file tidak ditemukan"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun validateBackup(backupPath: String): Result<BackupValidationResult> = withContext(ioDispatcher) {
        try {
            val decryptedJson = decryptFile(backupPath)
            val backupData = json.decodeFromString<BackupData>(decryptedJson)

            val recordCounts = mapOf(
<<<<<<< HEAD
                "pengguna" to backupData.data.users.size,
                "kategori" to backupData.data.categories.size,
                "gudang" to backupData.data.warehouses.size,
                "product" to backupData.data.products.size,
                "pemasok" to backupData.data.suppliers.size,
                "penjualan" to backupData.data.sales.size,
                "itemPenjualan" to backupData.data.saleItems.size,
                "pembelian" to backupData.data.purchases.size,
                "itemPembelian" to backupData.data.purchaseItems.size,
                "pengeluaran" to backupData.data.expenses.size
=======
                "users" to backupData.data.users.size,
                "categories" to backupData.data.categories.size,
                "warehouses" to backupData.data.warehouses.size,
                "products" to backupData.data.products.size,
                "suppliers" to backupData.data.suppliers.size,
                "sales" to backupData.data.sales.size,
                "saleItems" to backupData.data.saleItems.size,
                "purchases" to backupData.data.purchases.size,
                "purchaseItems" to backupData.data.purchaseItems.size,
                "expenses" to backupData.data.expenses.size
>>>>>>> feat/ui-overhaul
            )


            // Recalculate checksum over the serialized backup data with the checksum field cleared
            // Try compact first (new format)
            val checksumBaseCompact = jsonCompact.encodeToString(backupData.copy(metadata = backupData.metadata.copy(checksum = "")))
            var isValid = backupData.metadata.checksum == calculateChecksum(checksumBaseCompact)

            if (!isValid) {
                // Try pretty print (old format)
                val checksumBasePretty = json.encodeToString(backupData.copy(metadata = backupData.metadata.copy(checksum = "")))
                isValid = backupData.metadata.checksum == calculateChecksum(checksumBasePretty)
            }

            Result.success(BackupValidationResult(
                isValid = isValid,
                version = backupData.version,
                createdAt = backupData.createdAt,
                recordCounts = recordCounts
            ))
        } catch (e: Exception) {
            Result.success(BackupValidationResult(
                isValid = false,
                version = null,
                createdAt = null,
                recordCounts = null,
                errors = listOf(e.message ?: "Error validating backup")
            ))
        }
    }

    private fun generateBackupFileName(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val formatter = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
        return "backup_${formatter.format(date)}.enc"
    }

    private fun createBackupFile(fileName: String): String {
        val backupDir = getBackupDirectory()
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return File(backupDir, fileName).absolutePath
    }

    private fun getBackupDirectory(): File {
        // Allow tests to override the backup directory for deterministic behavior
        backupDirectoryOverride?.let { return it }

        return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ChibyChibyBackup")
    }

    // For tests: override backup directory to a test-controlled location
    @kotlin.jvm.JvmName("setBackupDirectoryForTest")
    fun setBackupDirectoryForTest(dir: File?) {
        backupDirectoryOverride = dir
    }

    // Backing field for the override; null in production
    private var backupDirectoryOverride: File? = null

    private fun calculateChecksum(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun calculateFileChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hash = digest.digest()
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun extractTimestampFromFileName(fileName: String): Long {
        // Extract timestamp from filename like "backup_20241211_143022.enc"
        val pattern = "backup_(\\d{8})_(\\d{6})\\.enc".toRegex()
        val match = pattern.find(fileName)
        return if (match != null) {
            try {
                val (date, time) = match.destructured
                val dateTimeString = "${date}${time}" // yyyyMMddHHmmss
                val formatter = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault())
                formatter.parse(dateTimeString)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        } else {
            System.currentTimeMillis()
        }
    }

    private fun openBackupOutputStream(file: File): OutputStream {
        return try {
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileOutput()
        } catch (e: Exception) {
            // Fallback for test environments or devices where EncryptedFile is not available
            file.outputStream()
        }
    }

    private fun encryptAndSave(data: String, filePath: String) {
        // This method is deprecated by new streaming implementation but kept for reference if needed,
        // though logic is now inside createBackup.
        val file = File(filePath)
        openBackupOutputStream(file).use { output ->
            output.write(data.toByteArray())
        }
    }

    private fun decryptFile(filePath: String): String {
        val file = File(filePath)
        return try {
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileInput().use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            // Fallback to plain file read when decryption is not available
            file.inputStream().use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        }
    }
}
