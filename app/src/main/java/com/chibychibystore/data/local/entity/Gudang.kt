package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chibychibystore.data.serialization.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
@Entity(
    tableName = "gudang",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class Gudang(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val location: String? = null,
    val capacity: Int = 0,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date()
)