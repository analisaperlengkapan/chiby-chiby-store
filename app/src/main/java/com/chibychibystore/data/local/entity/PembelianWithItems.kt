package com.chibychibystore.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PembelianWithItems(
    @Embedded
    val pembelian: Pembelian,
    @Relation(
        parentColumn = "id",
        entityColumn = "purchaseId"
    )
    val items: List<ItemPembelian>
)