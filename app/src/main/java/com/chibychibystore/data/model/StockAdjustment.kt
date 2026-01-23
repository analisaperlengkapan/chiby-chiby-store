package com.chibychibystore.data.model

data class StockAdjustment(
    val productId: Long,
    val warehouseId: Long,
    val delta: Int
)
