package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "pemasok",
    indices = [
        Index(value = ["name"])
    ]
)
data class Pemasok(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val contact: String? = null,
    val address: String? = null,
    val createdAt: Date = Date()
)