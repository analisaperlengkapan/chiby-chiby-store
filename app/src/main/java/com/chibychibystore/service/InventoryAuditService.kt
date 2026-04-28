package com.chibychibystore.service

import com.chibychibystore.data.local.entity.AuditStatus
import com.chibychibystore.data.local.entity.ItemStokOpname
import com.chibychibystore.data.local.entity.StokGudang
import com.chibychibystore.data.local.entity.StokOpname
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.InventoryAuditRepository
import com.chibychibystore.repository.StokGudangRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import androidx.room.withTransaction

@Singleton
class InventoryAuditService @Inject constructor(
    private val db: ChibyChibyDatabase,
    private val auditRepository: InventoryAuditRepository,
    private val stokGudangRepository: StokGudangRepository
) {
    fun observeAllAudits(): Flow<List<StokOpname>> = auditRepository.getAllAudits()

    suspend fun createAudit(audit: StokOpname, items: List<ItemStokOpname>): Result<Long> {
        return try {
            db.withTransaction {
                val createResult = auditRepository.createAudit(audit)
                if (createResult is Result.Failure) throw createResult.exception
                val auditId = (createResult as Result.Success).data
                val itemsWithId = items.map { it.copy(auditId = auditId) }
                val insertItemsResult = auditRepository.insertAuditItems(itemsWithId)
                if (insertItemsResult is Result.Failure) throw insertItemsResult.exception

                if (audit.status == AuditStatus.COMPLETED) {
                    // Adjust stock immediately if completed
                    applyStockAdjustments(itemsWithId, audit.warehouseId)
                }

                Result.success(auditId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeAudit(auditId: Long): Result<Unit> {
        return try {
            db.withTransaction {
                val getResult = auditRepository.getAuditById(auditId)
                if (getResult is Result.Failure) throw getResult.exception
                val audit = (getResult as Result.Success).data ?: throw Exception("Audit not found")
                if (audit.status != AuditStatus.DRAFT) throw Exception("Only draft audits can be completed")

                val items = auditRepository.getItemsByAuditId(auditId).firstOrNull() ?: emptyList()
                applyStockAdjustments(items, audit.warehouseId)

                val updateResult = auditRepository.updateAudit(audit.copy(status = AuditStatus.COMPLETED))
                if (updateResult is Result.Failure) throw updateResult.exception
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun applyStockAdjustments(items: List<ItemStokOpname>, warehouseId: Long) {
        items.forEach { item ->
            // Always set warehouse-specific stock to the actual counted quantity, even when
            // the recorded difference (actual - expected at audit-start) is zero. The expected
            // quantity is a snapshot taken when the audit started; concurrent operations (e.g.
            // sales) between starting and completing the audit may have moved the live stock
            // away from both expected and actual. Writing actualQuantity unconditionally ensures
            // the database reflects the physical count. This also syncs the product's total
            // stock via StokGudangRepository.
            val result = stokGudangRepository.insertOrUpdateStock(
                StokGudang(
                    productId = item.productId,
                    warehouseId = warehouseId,
                    quantity = item.actualQuantity
                )
            )
            if (result is Result.Failure) throw result.exception
        }
    }

    fun observeAuditItems(auditId: Long): Flow<List<ItemStokOpname>> = auditRepository.getItemsByAuditId(auditId)
}
