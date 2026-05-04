package com.chibychibystore.service

import com.chibychibystore.data.local.entity.ItemStokOpname
import com.chibychibystore.data.local.entity.StokOpname
import com.chibychibystore.data.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface untuk layanan audit inventaris (stok opname)
 */
interface InventoryAuditService {
    fun observeAllAudits(): Flow<List<StokOpname>>
    suspend fun createAudit(audit: StokOpname, items: List<ItemStokOpname>): Result<Long>
    suspend fun completeAudit(auditId: Long): Result<Unit>
    fun observeAuditItems(auditId: Long): Flow<List<ItemStokOpname>>
}
