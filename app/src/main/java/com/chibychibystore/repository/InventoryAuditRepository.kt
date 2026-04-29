package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.InventoryAuditDao
import com.chibychibystore.data.local.entity.ItemStokOpname
import com.chibychibystore.data.local.entity.StokOpname
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryAuditRepository @Inject constructor(
    private val auditDao: InventoryAuditDao
) {
    fun getAllAudits(): Flow<List<StokOpname>> = auditDao.getAllAudits()

    suspend fun getAuditById(id: Long): Result<StokOpname?> {
        return try {
            Result.success(auditDao.getAuditById(id))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getAuditById", e))
        }
    }

    suspend fun createAudit(audit: StokOpname): Result<Long> {
        return try {
            Result.success(auditDao.insertAudit(audit))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createAudit", e))
        }
    }

    suspend fun updateAudit(audit: StokOpname): Result<Unit> {
        return try {
            auditDao.updateAudit(audit)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateAudit", e))
        }
    }

    suspend fun insertAuditItems(items: List<ItemStokOpname>): Result<Unit> {
        return try {
            auditDao.insertAuditItems(items)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("insertAuditItems", e))
        }
    }

    fun getItemsByAuditId(auditId: Long): Flow<List<ItemStokOpname>> = auditDao.getItemsByAuditId(auditId)

    /**
     * Snapshot of all audits, used by the backup pipeline. Distinct from
     * [getAllAudits] which returns a Flow appropriate for UI observation.
     */
    suspend fun getAllAuditsList(): Result<List<StokOpname>> {
        return try {
            Result.success(auditDao.getAllAuditsList())
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getAllAuditsList", e))
        }
    }

    /**
     * Snapshot of all audit line items across every audit. Used by the backup
     * pipeline so the audit trail is preserved across restore cycles.
     */
    suspend fun getAllAuditItems(): Result<List<ItemStokOpname>> {
        return try {
            Result.success(auditDao.getAllAuditItems())
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getAllAuditItems", e))
        }
    }

    suspend fun insertAudits(audits: List<StokOpname>): Result<Unit> {
        return try {
            audits.forEach { auditDao.insertAudit(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("insertAudits", e))
        }
    }
}
