package com.chibychibystore.usecase

import com.chibychibystore.service.DataSeedingService
import javax.inject.Inject

/**
 * Use case untuk data seeding
 */
class DataSeedingUseCase @Inject constructor(
    private val dataSeedingService: DataSeedingService
) {

    /**
     * Execute data seeding untuk initial/demo data
     */
    suspend operator fun invoke(): Result<Unit> {
        return try {
            dataSeedingService.seedInitialData()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}