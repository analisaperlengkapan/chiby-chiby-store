package com.chibychibystore.repository

import com.chibychibystore.data.local.dao.PromotionDao
import com.chibychibystore.data.local.entity.Promotion
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PromotionRepository @Inject constructor(
    private val promotionDao: PromotionDao
) {
    fun getAllPromotions(): Flow<List<Promotion>> = promotionDao.getAllPromotions()

    fun getActivePromotions(): Flow<List<Promotion>> = promotionDao.getActivePromotions()

    fun getActivePromotionsForDate(date: Date): Flow<List<Promotion>> = promotionDao.getActivePromotionsForDate(date)

    suspend fun getPromotionById(id: Long): Promotion? = promotionDao.getPromotionById(id)

    suspend fun insertPromotion(promotion: Promotion): Long = promotionDao.insertPromotion(promotion)

    suspend fun updatePromotion(promotion: Promotion) = promotionDao.updatePromotion(promotion)

    suspend fun deletePromotion(promotion: Promotion) = promotionDao.deletePromotion(promotion)

    suspend fun deletePromotionById(id: Long) = promotionDao.deletePromotionById(id)
}
