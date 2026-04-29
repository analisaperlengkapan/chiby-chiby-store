package com.chibychibystore.data.local.dao

import androidx.room.*
import com.chibychibystore.data.local.entity.ItemStokOpname
import com.chibychibystore.data.local.entity.StokOpname
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryAuditDao {
    @Query("SELECT * FROM stok_opname ORDER BY auditDate DESC")
    fun getAllAudits(): Flow<List<StokOpname>>

    @Query("SELECT * FROM stok_opname WHERE id = :id")
    suspend fun getAuditById(id: Long): StokOpname?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAudit(audit: StokOpname): Long

    // Batch insert used by the restore pipeline. Uses REPLACE on conflict so
    // re-running a restore is idempotent, and Room wraps the @Insert in a
    // single transaction giving all-or-nothing semantics — a single bad row
    // throws and rolls back the whole batch instead of leaving partial state.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudits(audits: List<StokOpname>)

    @Update
    suspend fun updateAudit(audit: StokOpname)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditItems(items: List<ItemStokOpname>)

    @Query("SELECT * FROM item_stok_opname WHERE auditId = :auditId")
    fun getItemsByAuditId(auditId: Long): Flow<List<ItemStokOpname>>

    // Used by backup to enumerate every audit-line across all audits in a single
    // query. The per-audit Flow query above can't be reused for a full backup
    // dump without iterating every audit and collecting its Flow.
    @Query("SELECT * FROM item_stok_opname")
    suspend fun getAllAuditItems(): List<ItemStokOpname>

    @Query("SELECT * FROM stok_opname")
    suspend fun getAllAuditsList(): List<StokOpname>
}
