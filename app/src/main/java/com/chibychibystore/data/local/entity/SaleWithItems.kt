package com.chibychibystore.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class SaleWithItems(
    @Embedded
    val sale: Sale,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItem>
)
