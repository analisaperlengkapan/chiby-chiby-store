package com.chibychibystore.data.local.dao

import androidx.room.*
import com.chibychibystore.data.local.entity.Promotion
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PromotionDao {

    @Query("SELECT * FROM promotion")
    fun getAllPromotions(): Flow<List<Promotion>>

    @Query("SELECT * FROM promotion WHERE id = :id")
    suspend fun getPromotionById(id: Long): Promotion?

    @Query("SELECT * FROM promotion WHERE isActive = 1")
    fun getActivePromotions(): Flow<List<Promotion>>

    @Query("SELECT * FROM promotion WHERE isActive = 1 AND (startDate IS NULL OR startDate <= :date) AND (endDate IS NULL OR endDate >= :date)")
    fun getActivePromotionsForDate(date: Date): Flow<List<Promotion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotion(promotion: Promotion): Long

    @Update
    suspend fun updatePromotion(promotion: Promotion)

    @Delete
    suspend fun deletePromotion(promotion: Promotion)

    @Query("DELETE FROM promotion WHERE id = :id")
    suspend fun deletePromotionById(id: Long)
}
