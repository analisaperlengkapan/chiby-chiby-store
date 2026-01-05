package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chibychibystore.data.serialization.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
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
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date()
)
