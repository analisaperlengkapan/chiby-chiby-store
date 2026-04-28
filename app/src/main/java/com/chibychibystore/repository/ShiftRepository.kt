package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.ShiftDao
import com.chibychibystore.data.local.entity.Shift
import com.chibychibystore.data.model.Result
import com.chibychibystore.error.ChibyChibyException
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShiftRepository @Inject constructor(
    private val shiftDao: ShiftDao
) {
    fun getAllShifts(): Flow<List<Shift>> = shiftDao.getAllShifts()

    suspend fun getShiftById(id: Long): Result<Shift?> {
        return try {
            Result.success(shiftDao.getShiftById(id))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getShiftById", e))
        }
    }

    suspend fun getOpenShiftByKasir(kasirId: Long): Result<Shift?> {
        return try {
            Result.success(shiftDao.getOpenShiftByKasir(kasirId))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("getOpenShiftByKasir", e))
        }
    }

    suspend fun createShift(shift: Shift): Result<Long> {
        return try {
            Result.success(shiftDao.insertShift(shift))
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("createShift", e))
        }
    }

    suspend fun updateShift(shift: Shift): Result<Unit> {
        return try {
            shiftDao.updateShift(shift)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ChibyChibyException.DatabaseError("updateShift", e))
        }
    }
}
