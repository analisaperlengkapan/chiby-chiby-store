package com.chibychibystore.service.impl

import android.content.Context
import androidx.security.crypto.EncryptedFile
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.service.RestoreService
import com.chibychibystore.service.RestoreProgress
import androidx.security.crypto.MasterKey
import com.chibychibystore.data.backup.BackupData
import com.chibychibystore.repository.*
import androidx.room.withTransaction
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton





/**
 * Implementation RestoreService menggunakan repository
 */
import com.chibychibystore.service.RestoreResult
import com.chibychibystore.service.BackupPreview
import com.chibychibystore.service.BackupValidationResult

/**
 * Implementation RestoreService menggunakan repository
 */
@Singleton
class RestoreServiceImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val database: ChibyChibyDatabase,
    private val userRepository: PenggunaRepository,
    private val categoryRepository: KategoriRepository,
    private val warehouseRepository: GudangRepository,
    private val productRepository: ProdukRepository,
    private val supplierRepository: PemasokRepository,
    private val saleRepository: PenjualanRepository,
    private val saleItemRepository: ItemPenjualanRepository,
    private val purchaseRepository: PembelianRepository,
    private val purchaseItemRepository: ItemPembelianRepository,
    private val expenseRepository: PengeluaranRepository
) : RestoreService {

    private val json = Json { prettyPrint = true }
    private val masterKey by lazy {
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }
    private val _restoreProgress = MutableStateFlow(RestoreProgress())
    override fun observeRestoreProgress(): StateFlow<RestoreProgress> = _restoreProgress

    override suspend fun restoreFromBackup(backupPath: String, clearExistingData: Boolean): com.chibychibystore.data.model.Result<RestoreResult> {
        return try {
            _restoreProgress.value = RestoreProgress(isInProgress = true, totalSteps = 12)

            // Step 1: Validate backup file
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memvalidasi file backup", progress = 0.1f, currentStepIndex = 1, totalSteps = 12)
            val validation = validateBackupFile(backupPath)
            if (!validation.isValid) {
                return com.chibychibystore.data.model.Result.failure(Exception("File backup tidak valid: ${validation.errors.joinToString()}"))
            }

            // Step 2: Decrypt and parse backup data
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Membaca data backup", progress = 0.2f, currentStepIndex = 2, totalSteps = 12)
            val backupData = loadBackupData(backupPath)

            // Step 3: Clear existing data
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Menghapus data lama", progress = 0.3f, currentStepIndex = 3, totalSteps = 12)
            if (clearExistingData) {
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }
            }

            // Step 4: Restore users
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pengguna", progress = 0.4f, currentStepIndex = 4, totalSteps = 12)
            val usersRestored = restoreUsers(backupData.data.users)

            // Step 5: Restore categories
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data kategori", progress = 0.5f, currentStepIndex = 5, totalSteps = 12)
            val categoriesRestored = restoreCategories(backupData.data.categories)

            // Step 6: Restore warehouses
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data gudang", progress = 0.6f, currentStepIndex = 6, totalSteps = 12)
            val warehousesRestored = restoreGudangs(backupData.data.warehouses)

            // Step 7: Restore suppliers
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pemasok", progress = 0.7f, currentStepIndex = 7, totalSteps = 12)
            val suppliersRestored = restorePemasoks(backupData.data.suppliers)

            // Step 8: Restore products
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data product", progress = 0.8f, currentStepIndex = 8, totalSteps = 12)
            val productsRestored = restoreProduks(backupData.data.products)

            // Step 9: Restore sales and items
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data penjualan", progress = 0.9f, currentStepIndex = 9, totalSteps = 12)
            val salesRestored = restorePenjualans(backupData.data.sales, backupData.data.saleItems)

            // Step 10: Restore purchases and items
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pembelian", progress = 0.95f, currentStepIndex = 10, totalSteps = 12)
            val purchasesRestored = restorePembelians(backupData.data.purchases, backupData.data.purchaseItems)

            // Step 11: Restore expenses
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pengeluaran", progress = 0.98f, currentStepIndex = 11, totalSteps = 12)
            val expensesRestored = restorePengeluarans(backupData.data.expenses)

            // Step 12: Finalize
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Finalisasi", progress = 1.0f, currentStepIndex = 12, totalSteps = 12)

            val recordsRestored = mapOf(
                "pengguna" to usersRestored,
                "kategori" to categoriesRestored,
                "gudang" to warehousesRestored,
                "product" to productsRestored,
                "pemasok" to suppliersRestored,
                "penjualan" to salesRestored,
                "pembelian" to purchasesRestored,
                "pengeluaran" to expensesRestored
            )

            _restoreProgress.value = RestoreProgress(isInProgress = false)

            com.chibychibystore.data.model.Result.success(RestoreResult(
                success = true,
                recordsRestored = recordsRestored
            ))

        } catch (e: Exception) {
            _restoreProgress.value = RestoreProgress(isInProgress = false)
            com.chibychibystore.data.model.Result.failure(e)
        }
    }

    override suspend fun previewBackup(backupPath: String): com.chibychibystore.data.model.Result<BackupPreview> {
        return try {
            val validation = validateBackupFile(backupPath)
            if (!validation.isValid) {
                return com.chibychibystore.data.model.Result.failure(Exception("File backup tidak valid"))
            }

            val backupData = loadBackupData(backupPath)
            val file = File(backupPath)

            com.chibychibystore.data.model.Result.success(BackupPreview(
                version = backupData.version,
                createdAt = backupData.createdAt,
                recordCounts = mapOf(
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
                ),
                sizeBytes = file.length()
            ))
        } catch (e: Exception) {
            com.chibychibystore.data.model.Result.failure(e)
        }
    }

    private fun validateBackupFile(backupPath: String): BackupValidationResult {
        return try {
            val decryptedJson = decryptFile(backupPath)
            val backupData = json.decodeFromString<BackupData>(decryptedJson)

            val recordCounts = mapOf(
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
            )

            val isValid = backupData.metadata.checksum == calculateChecksum(decryptedJson)

            BackupValidationResult(
                isValid = isValid,
                version = backupData.version,
                createdAt = backupData.createdAt,
                recordCounts = recordCounts
            )
        } catch (e: Exception) {
            BackupValidationResult(
                isValid = false,
                version = null,
                createdAt = null,
                recordCounts = null,
                errors = listOf(e.message ?: "Error validating backup")
            )
        }
    }

    private fun loadBackupData(backupPath: String): BackupData {
        val decryptedJson = decryptFile(backupPath)
        return json.decodeFromString<BackupData>(decryptedJson)
    }

    private suspend fun restoreUsers(users: List<Pengguna>): Int {
        val result = userRepository.createPenggunaList(users)
        return if (result is com.chibychibystore.data.model.Result.Success) {
            result.data
        } else {
            0
        }
    }

    internal suspend fun restoreCategories(categories: List<Kategori>): Int {
        val result = categoryRepository.createKategoriList(categories)
        return if (result is com.chibychibystore.data.model.Result.Success) {
            result.data
        } else {
            0
        }
    }

    private suspend fun restoreGudangs(warehouses: List<Gudang>): Int {
        return try {
            val result = warehouseRepository.createGudangList(warehouses)
            if (result is com.chibychibystore.data.model.Result.Success) {
                result.data
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun restorePemasoks(suppliers: List<Pemasok>): Int {
        val result = supplierRepository.createPemasokList(suppliers)
        return if (result is com.chibychibystore.data.model.Result.Success) {
            result.data
        } else {
            0
        }
    }

    private suspend fun restoreProduks(products: List<Produk>): Int {
        var count = 0
        for (product in products) {
            try {
                val result = productRepository.createProduk(product)
                if (result is com.chibychibystore.data.model.Result.Success) {
                    count++
                }
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restorePenjualans(sales: List<Penjualan>, items: List<ItemPenjualan>): Int {
        var count = 0
        for (sale in sales) {
            try {
                val saleItems = items.filter { it.saleId == sale.id }

                database.withTransaction {
                    val result = saleRepository.createPenjualan(sale, saleItems)
                    if (result is com.chibychibystore.data.model.Result.Failure) {
                        throw result.exception
                    }
                }
                count++
            } catch (e: Exception) {
                // Log error but continue
            }
        }

        return count
    }

    private suspend fun restorePembelians(purchases: List<Pembelian>, items: List<ItemPembelian>): Int {
        var count = 0
        for (purchase in purchases) {
            try {
                val purchaseItems = items.filter { it.purchaseId == purchase.id }

                database.withTransaction {
                    val result = purchaseRepository.createPembelian(purchase)

                    if (result is com.chibychibystore.data.model.Result.Success) {
                        val createdPurchaseId = result.data.id

                        val newItems = purchaseItems.map { it.copy(purchaseId = createdPurchaseId, id = 0) }

                        if (newItems.isNotEmpty()) {
                            val itemsResult = purchaseItemRepository.createItemPembelianList(newItems)
                            if (itemsResult is com.chibychibystore.data.model.Result.Failure) {
                                throw itemsResult.exception
                            }
                        }
                    } else {
                        throw result.exceptionOrNull() ?: Exception("Failed to create purchase")
                    }
                }
                count++
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restorePengeluarans(expenses: List<Pengeluaran>): Int {
        var count = 0
        for (expense in expenses) {
            try {
                val result = expenseRepository.createPengeluaran(expense)
                if (result is com.chibychibystore.data.model.Result.Success) {
                    count++
                }
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private fun calculateChecksum(data: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun decryptFile(filePath: String): String {
        val file = File(filePath)
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return encryptedFile.openFileInput().use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        }
    }
}
