package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "kategori",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class Kategori(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Date = Date()
)