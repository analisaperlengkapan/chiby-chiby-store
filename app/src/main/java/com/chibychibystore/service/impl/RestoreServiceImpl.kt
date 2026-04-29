package com.chibychibystore.service.impl

import android.content.Context
import androidx.security.crypto.EncryptedFile
import com.chibychibystore.data.local.entity.*
import com.chibychibystore.service.RestoreService
import com.chibychibystore.service.RestoreProgress
import com.chibychibystore.service.RestoreResult
import com.chibychibystore.service.BackupPreview
import com.chibychibystore.service.BackupValidationResult
import androidx.security.crypto.MasterKey
import com.chibychibystore.data.backup.BackupData
import com.chibychibystore.repository.*
import androidx.room.withTransaction
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import com.chibychibystore.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
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
@Singleton
class RestoreServiceImpl @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val database: ChibyChibyDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val userRepository: PenggunaRepository,
    private val categoryRepository: KategoriRepository,
    private val warehouseRepository: GudangRepository,
    private val productRepository: ProdukRepository,
    private val supplierRepository: PemasokRepository,
    private val saleRepository: PenjualanRepository,
    private val saleItemRepository: ItemPenjualanRepository,
    private val purchaseRepository: PembelianRepository,
    private val purchaseItemRepository: ItemPembelianRepository,
    private val expenseRepository: PengeluaranRepository,
    private val shiftRepository: ShiftRepository,
    private val pelangganRepository: PelangganRepository,
    private val stokGudangRepository: StokGudangRepository,
    private val inventoryAuditRepository: InventoryAuditRepository
) : RestoreService {

    private val json = Json { prettyPrint = true }
    private val masterKey by lazy {
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }
    private val _restoreProgress = MutableStateFlow(RestoreProgress())
    override fun observeRestoreProgress(): StateFlow<RestoreProgress> = _restoreProgress

    override suspend fun restoreFromBackup(backupPath: String, clearExistingData: Boolean): com.chibychibystore.data.model.Result<RestoreResult> {
        return try {
            _restoreProgress.value = RestoreProgress(isInProgress = true, totalSteps = 17)

            // Step 1: Validate backup file
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memvalidasi file backup", progress = 1f / 17f, currentStepIndex = 1, totalSteps = 17)
            val validation = validateBackupFile(backupPath)
            if (!validation.isValid) {
                return com.chibychibystore.data.model.Result.failure(Exception("File backup tidak valid: ${validation.errors.joinToString()}"))
            }

            // Step 2: Decrypt and parse backup data
            _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Membaca data backup", progress = 2f / 17f, currentStepIndex = 2, totalSteps = 17)
            val backupData = loadBackupData(backupPath)

            // Wrap clear-and-restore in a single Room transaction so an exception
            // mid-way (or a crash that aborts the coroutine) can't leave the DB
            // half-populated — or fully empty after `clearAllTables` ran but no
            // data was restored. Any exception escaping this block rolls the
            // entire transaction back, including the clear. Per-row catches
            // inside the restoreXxx helpers continue to swallow individual
            // FK/constraint failures so transient bad rows don't abort the
            // entire restore.
            val txResult = database.withTransaction {
                // Step 3: Clear existing data
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Menghapus data lama", progress = 3f / 17f, currentStepIndex = 3, totalSteps = 17)
                if (clearExistingData) {
                    database.clearAllTables()
                }

                // Step 4: Restore users
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pengguna", progress = 4f / 17f, currentStepIndex = 4, totalSteps = 17)
                val usersRestored = restoreUsers(backupData.data.users)

                // Step 5: Restore categories
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data kategori", progress = 5f / 17f, currentStepIndex = 5, totalSteps = 17)
                val categoriesRestored = restoreCategories(backupData.data.categories)

                // Step 6: Restore warehouses
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data gudang", progress = 6f / 17f, currentStepIndex = 6, totalSteps = 17)
                val warehousesRestored = restoreGudangs(backupData.data.warehouses)

                // Step 7: Restore suppliers
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pemasok", progress = 7f / 17f, currentStepIndex = 7, totalSteps = 17)
                val suppliersRestored = restorePemasoks(backupData.data.suppliers)

                // Step 8: Restore products
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data product", progress = 8f / 17f, currentStepIndex = 8, totalSteps = 17)
                val productsRestored = restoreProduks(backupData.data.products)

                // Step 9: Restore per-warehouse stock. Depends on products and
                // warehouses (FK-referenced). Without this, a full restore would
                // leave stok_gudang empty while Produk.stockQuantity is repopulated
                // — breaking the per-warehouse breakdown that purchases, sales,
                // and audits all depend on.
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan stok gudang", progress = 9f / 17f, currentStepIndex = 9, totalSteps = 17)
                val stocksRestored = restoreStocks(backupData.data.stocks)

                // Restore shifts and customers BEFORE sales — Penjualan has FKs to both
                // (shiftId → shift, pelangganId → pelanggan). If we restored sales first
                // any sale with a non-null shiftId/pelangganId would hit an FK violation
                // and be silently dropped by the per-row catch in restorePenjualans.

                // Step 10: Restore shifts
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data shift", progress = 10f / 17f, currentStepIndex = 10, totalSteps = 17)
                val shiftsRestored = restoreShifts(backupData.data.shifts)

                // Step 11: Restore customers
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pelanggan", progress = 11f / 17f, currentStepIndex = 11, totalSteps = 17)
                val customersRestored = restorePelanggans(backupData.data.customers)

                // Step 12: Restore sales and items
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data penjualan", progress = 12f / 17f, currentStepIndex = 12, totalSteps = 17)
                val salesRestored = restorePenjualans(backupData.data.sales, backupData.data.saleItems)

                // Step 13: Restore purchases and items
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pembelian", progress = 13f / 17f, currentStepIndex = 13, totalSteps = 17)
                val purchasesRestored = restorePembelians(backupData.data.purchases, backupData.data.purchaseItems)

                // Step 14: Restore expenses
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data pengeluaran", progress = 14f / 17f, currentStepIndex = 14, totalSteps = 17)
                val expensesRestored = restorePengeluarans(backupData.data.expenses)

                // Step 15: Restore inventory audit headers. Depends on warehouses
                // and pengguna (FK targets via warehouseId / auditorId).
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan data audit", progress = 15f / 17f, currentStepIndex = 15, totalSteps = 17)
                val auditsRestored = restoreAudits(backupData.data.audits)

                // Step 16: Restore audit line items. Depends on audits (FK
                // target) and products. Restored after audits so item rows
                // don't get FK-rejected and silently dropped.
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Memulihkan item audit", progress = 16f / 17f, currentStepIndex = 16, totalSteps = 17)
                val auditItemsRestored = restoreAuditItems(backupData.data.auditItems)

                // Step 17: Finalize
                _restoreProgress.value = RestoreProgress(isInProgress = true, currentStep = "Finalisasi", progress = 1.0f, currentStepIndex = 17, totalSteps = 17)

                mapOf(
                    "pengguna" to usersRestored,
                    "kategori" to categoriesRestored,
                    "gudang" to warehousesRestored,
                    "product" to productsRestored,
                    "pemasok" to suppliersRestored,
                    "stokGudang" to stocksRestored,
                    "shift" to shiftsRestored,
                    "pelanggan" to customersRestored,
                    "penjualan" to salesRestored,
                    "pembelian" to purchasesRestored,
                    "pengeluaran" to expensesRestored,
                    "stokOpname" to auditsRestored,
                    "itemStokOpname" to auditItemsRestored
                )
            }

            _restoreProgress.value = RestoreProgress(isInProgress = false)

            com.chibychibystore.data.model.Result.success(RestoreResult(
                success = true,
                recordsRestored = txResult
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
                    "stokGudang" to backupData.data.stocks.size,
                    "shift" to backupData.data.shifts.size,
                    "pelanggan" to backupData.data.customers.size,
                    "penjualan" to backupData.data.sales.size,
                    "itemPenjualan" to backupData.data.saleItems.size,
                    "pembelian" to backupData.data.purchases.size,
                    "itemPembelian" to backupData.data.purchaseItems.size,
                    "pengeluaran" to backupData.data.expenses.size,
                    "stokOpname" to backupData.data.audits.size,
                    "itemStokOpname" to backupData.data.auditItems.size
                ),
                sizeBytes = file.length()
            ))
        } catch (e: Exception) {
            com.chibychibystore.data.model.Result.failure(e)
        }
    }

    private suspend fun validateBackupFile(backupPath: String): BackupValidationResult {
        return try {
            val decryptedJson = decryptFileAsync(backupPath)
            val backupData = json.decodeFromString<BackupData>(decryptedJson)

            val recordCounts = mapOf(
                "pengguna" to backupData.data.users.size,
                "kategori" to backupData.data.categories.size,
                "gudang" to backupData.data.warehouses.size,
                "product" to backupData.data.products.size,
                "pemasok" to backupData.data.suppliers.size,
                "stokGudang" to backupData.data.stocks.size,
                "shift" to backupData.data.shifts.size,
                "pelanggan" to backupData.data.customers.size,
                "penjualan" to backupData.data.sales.size,
                "itemPenjualan" to backupData.data.saleItems.size,
                "pembelian" to backupData.data.purchases.size,
                "itemPembelian" to backupData.data.purchaseItems.size,
                "pengeluaran" to backupData.data.expenses.size,
                "stokOpname" to backupData.data.audits.size,
                "itemStokOpname" to backupData.data.auditItems.size
            )

            // Successful decrypt + parse is sufficient to consider the backup
            // structurally valid. The previous comparison
            //     backupData.metadata.checksum == calculateChecksum(decryptedJson)
            // could never succeed: BackupServiceImpl writes the file by
            // computing SHA-256 over the prefix-with-EMPTY-checksum + body +
            // suffix, then re-emits the prefix with the REAL checksum baked in
            // (BackupServiceImpl.kt:79-93, 217-244). So `decryptedJson` (the
            // final file with the real checksum embedded) hashes to a different
            // digest than what was stored in `metadata.checksum`, making
            // `isValid` always false and rejecting every backup at the start of
            // restoreFromBackup. Treat parse-success as validity, mirroring the
            // public BackupServiceImpl.validateBackup which already does this
            // (BackupServiceImpl.kt:350).
            val isValid = true

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

    private suspend fun loadBackupData(backupPath: String): BackupData {
        val decryptedJson = decryptFileAsync(backupPath)
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

    internal suspend fun restoreProduks(products: List<Produk>): Int {
        // Try batch insertion first for performance
        val batchResult = productRepository.createProdukList(products)
        if (batchResult is com.chibychibystore.data.model.Result.Success) {
            return batchResult.data
        }

        // Fallback to individual insertion if batch fails
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
        if (sales.isEmpty()) return 0
        
        // Optimistic Batch Approach
        try {
            return database.withTransaction {
                val result = saleRepository.createPenjualanList(sales, items)
                if (result is com.chibychibystore.data.model.Result.Success) {
                    result.data
                } else {
                    // Trigger rollback and fallback
                    throw result.exceptionOrNull() ?: Exception("Failed to restore sales batch")
                }
            }
        } catch (e: Exception) {
            // Fallback to iterative approach (slower but resilient to partial failures)
            // Optimization: Group items by saleId once to avoid O(N*M) lookups
            val itemsBySaleId = items.groupBy { it.saleId }
            var count = 0
            
            for (sale in sales) {
                try {
                    val saleItems = itemsBySaleId[sale.id] ?: emptyList()

                    database.withTransaction {
                        val result = saleRepository.createPenjualan(sale, saleItems)
                        if (result is com.chibychibystore.data.model.Result.Failure) {
                            throw result.exception
                        }
                    }
                    count++
                } catch (ex: Exception) {
                    // Log error but continue
                }
            }
            return count
        }
    }

    private suspend fun restorePembelians(purchases: List<Pembelian>, items: List<ItemPembelian>): Int {
        var count = 0
        val itemsMap = items.groupBy { it.purchaseId }
        for (purchase in purchases) {
            try {
                val purchaseItems = itemsMap[purchase.id] ?: emptyList()

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

    private suspend fun restoreShifts(shifts: List<Shift>): Int {
        var count = 0
        for (shift in shifts) {
            try {
                val result = shiftRepository.createShift(shift)
                if (result is com.chibychibystore.data.model.Result.Success) {
                    count++
                }
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restorePelanggans(customers: List<Pelanggan>): Int {
        var count = 0
        for (customer in customers) {
            try {
                val result = pelangganRepository.createPelanggan(customer)
                if (result is com.chibychibystore.data.model.Result.Success) {
                    count++
                }
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    /**
     * Restore per-warehouse stock rows in a single batch INSERT (REPLACE on
     * conflict). Reports the number of rows that *would* be inserted on success
     * to match the per-row counters used by the other restoreXxx helpers.
     * On failure we fall back to per-row inserts so a single bad row doesn't
     * abort the entire batch.
     */
    private suspend fun restoreStocks(stocks: List<StokGudang>): Int {
        if (stocks.isEmpty()) return 0
        val batchResult = stokGudangRepository.insertStocks(stocks)
        if (batchResult is com.chibychibystore.data.model.Result.Success) {
            return stocks.size
        }
        var count = 0
        for (stock in stocks) {
            try {
                val r = stokGudangRepository.insertOrUpdateStock(stock)
                if (r is com.chibychibystore.data.model.Result.Success) {
                    count++
                }
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restoreAudits(audits: List<StokOpname>): Int {
        if (audits.isEmpty()) return 0
        val batchResult = inventoryAuditRepository.insertAudits(audits)
        if (batchResult is com.chibychibystore.data.model.Result.Success) {
            return audits.size
        }
        var count = 0
        for (audit in audits) {
            try {
                val r = inventoryAuditRepository.createAudit(audit)
                if (r is com.chibychibystore.data.model.Result.Success) {
                    count++
                }
            } catch (e: Exception) {
                // Log error but continue
            }
        }
        return count
    }

    private suspend fun restoreAuditItems(items: List<ItemStokOpname>): Int {
        if (items.isEmpty()) return 0
        // insertAuditItems uses OnConflictStrategy.REPLACE so the whole batch
        // either succeeds or throws together; treat all-or-nothing.
        val r = inventoryAuditRepository.insertAuditItems(items)
        return if (r is com.chibychibystore.data.model.Result.Success) items.size else 0
    }

    private fun calculateChecksum(data: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    private suspend fun decryptFileAsync(filePath: String): String {
        return withContext(ioDispatcher) {
            decryptFile(filePath)
        }
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
