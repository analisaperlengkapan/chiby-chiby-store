package com.chibychibystore.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PenjualanWithItems(
    @Embedded
    val sale: Penjualan,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<ItemPenjualan>
)
