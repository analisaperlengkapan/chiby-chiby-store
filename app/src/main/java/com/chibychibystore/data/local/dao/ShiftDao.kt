package com.chibychibystore.data.local.dao

import androidx.room.*
import com.chibychibystore.data.local.entity.Shift
import com.chibychibystore.data.local.entity.ShiftStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shift ORDER BY startTime DESC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shift WHERE id = :id")
    suspend fun getShiftById(id: Long): Shift?

    @Query("SELECT * FROM shift WHERE kasirId = :kasirId AND status = 'OPEN' LIMIT 1")
    suspend fun getOpenShiftByKasir(kasirId: Long): Shift?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShift(shift: Shift): Long

    @Update
    suspend fun updateShift(shift: Shift)

    @Query("SELECT * FROM shift WHERE startTime BETWEEN :start AND :end")
    fun getShiftsByDateRange(start: Date, end: Date): Flow<List<Shift>>
}
