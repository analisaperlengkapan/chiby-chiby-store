package com.chibychibystore.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
data class PurchaseWithItems(
    @Embedded val purchase: Purchase,
    @Relation(
        parentColumn = "id",
        entityColumn = "purchaseId"
    )
    val items: List<PurchaseItem>
)
