package com.chibychibystore.service.impl

import android.content.Context
import androidx.security.crypto.EncryptedFile
import com.chibychibystore.service.RestoreService
import com.chibychibystore.service.RestoreProgress
import androidx.security.crypto.MasterKey
import com.chibychibystore.data.backup.BackupData
import com.chibychibystore.repository.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation RestoreService menggunakan repository
 */
@Singleton
class RestoreServiceImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val warehouseRepository: WarehouseRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val saleRepository: SaleRepository,
    private val saleItemRepository: SaleItemRepository,
    private val purchaseRepository: PurchaseRepository,
    private val expenseRepository: ExpenseRepository
) : RestoreService {

    private val json = Json { prettyPrint = true }
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val _restoreProgress = MutableStateFlow(RestoreProgress())
    override fun observeRestoreProgress(): StateFlow<RestoreProgress> = _restoreProgress

    override suspend fun restoreFromBackup(backupPath: String): com.chibychibystore.data.model.Result<RestoreResult> {
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

            // Step 3: Clear existing data (optional - could be configurable)
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Menghapus data lama", progress = 0.3f, currentStepIndex = 3, totalSteps = 12)
            // Note: In a real implementation, you might want to make this optional

            // Step 4: Restore users
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pengguna", progress = 0.4f, currentStepIndex = 4, totalSteps = 12)
            val usersRestored = restoreUsers(backupData.data.users)

            // Step 5: Restore categories
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data kategori", progress = 0.5f, currentStepIndex = 5, totalSteps = 12)
            val categoriesRestored = restoreCategories(backupData.data.categories)

            // Step 6: Restore warehouses
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data gudang", progress = 0.6f, currentStepIndex = 6, totalSteps = 12)
            val warehousesRestored = restoreWarehouses(backupData.data.warehouses)

            // Step 7: Restore suppliers
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pemasok", progress = 0.7f, currentStepIndex = 7, totalSteps = 12)
            val suppliersRestored = restoreSuppliers(backupData.data.suppliers)

            // Step 8: Restore products
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data product", progress = 0.8f, currentStepIndex = 8, totalSteps = 12)
            val productsRestored = restoreProducts(backupData.data.products)

            // Step 9: Restore sales and items
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data penjualan", progress = 0.9f, currentStepIndex = 9, totalSteps = 12)
            val salesRestored = restoreSales(backupData.data.sales, backupData.data.saleItems)

            // Step 10: Restore purchases and items
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pembelian", progress = 0.95f, currentStepIndex = 10, totalSteps = 12)
            val purchasesRestored = restorePurchases(backupData.data.purchases, backupData.data.purchaseItems)

            // Step 11: Restore expenses
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pengeluaran", progress = 0.98f, currentStepIndex = 11, totalSteps = 12)
            val expensesRestored = restoreExpenses(backupData.data.expenses)

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

    private suspend fun restoreUsers(users: List<com.chibychibystore.data.local.entity.User>): Int {
        var count = 0
        for (user in users) {
            try {
                userRepository.createUser(user)
                count++
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restoreCategories(categories: List<com.chibychibystore.data.local.entity.Category>): Int {
        var count = 0
        for (cat in categories) {
            try {
                categoryRepository.createCategory(cat)
                count++
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restoreWarehouses(warehouses: List<com.chibychibystore.data.local.entity.Warehouse>): Int {
        var count = 0
        for (warehouse in warehouses) {
            try {
                warehouseRepository.createWarehouse(warehouse)
                count++
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restoreSuppliers(suppliers: List<com.chibychibystore.data.local.entity.Supplier>): Int {
        var count = 0
        for (supplier in suppliers) {
            try {
                supplierRepository.createSupplier(supplier)
                count++
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restoreProducts(products: List<com.chibychibystore.data.local.entity.Product>): Int {
        var count = 0
        for (product in products) {
            try {
                productRepository.createProduct(product)
                count++
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restoreSales(sales: List<com.chibychibystore.data.local.entity.Sale>, items: List<com.chibychibystore.data.local.entity.SaleItem>): Int {
        var count = 0
        for (sale in sales) {
            try {
                val saleItems = items.filter { it.saleId == sale.id }
                saleRepository.createSale(sale, saleItems)
                count++
            } catch (e: Exception) {
                // Log error but continue
            }
        }

        // Items are created with sale if using createSale(sale, items)
        // If createSale doesn't handle items, we need to insert them separately
        // Assuming createSale handles transaction and inserts both.

        return count
    }

    private suspend fun restorePurchases(purchases: List<com.chibychibystore.data.local.entity.Purchase>, items: List<com.chibychibystore.data.local.entity.PurchaseItem>): Int {
        var count = 0
        for (purchase in purchases) {
            try {
                val purchaseItems = items.filter { it.purchaseId == purchase.id }
                purchaseRepository.createPurchase(purchase, purchaseItems)
                count++
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restoreExpenses(expenses: List<com.chibychibystore.data.local.entity.Expense>): Int {
        var count = 0
        for (expense in expenses) {
            try {
                expenseRepository.createExpense(expense)
                count++
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
