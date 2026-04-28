package com.chibychibystore.service

import com.chibychibystore.data.local.entity.AuditStatus
import com.chibychibystore.data.local.entity.ItemStokOpname
import com.chibychibystore.data.local.entity.StokOpname
import com.chibychibystore.data.model.Result
import com.chibychibystore.repository.InventoryAuditRepository
import com.chibychibystore.repository.ProdukRepository
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
    private val produkRepository: ProdukRepository
) {
    fun observeAllAudits(): Flow<List<StokOpname>> = auditRepository.getAllAudits()

    suspend fun createAudit(audit: StokOpname, items: List<ItemStokOpname>): Result<Long> {
        return try {
            db.withTransaction {
                val auditId = auditRepository.createAudit(audit).getOrNull() ?: throw Exception("Failed to create audit")
                val itemsWithId = items.map { it.copy(auditId = auditId) }
                auditRepository.insertAuditItems(itemsWithId)

                if (audit.status == AuditStatus.COMPLETED) {
                    // Adjust stock immediately if completed
                    applyStockAdjustments(itemsWithId)
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
                val audit = auditRepository.getAuditById(auditId).getOrNull() ?: throw Exception("Audit not found")
                if (audit.status != AuditStatus.DRAFT) throw Exception("Only draft audits can be completed")

                val items = auditRepository.getItemsByAuditId(auditId).firstOrNull() ?: emptyList()
                applyStockAdjustments(items)

                auditRepository.updateAudit(audit.copy(status = AuditStatus.COMPLETED))
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun applyStockAdjustments(items: List<ItemStokOpname>) {
        items.forEach { item ->
            if (item.difference != 0) {
                produkRepository.updateStock(item.productId.toString(), item.actualQuantity)
            }
        }
    }

    fun observeAuditItems(auditId: Long): Flow<List<ItemStokOpname>> = auditRepository.getItemsByAuditId(auditId)
}
