package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "pengguna",
    indices = [
        Index(value = ["username"], unique = true)
    ]
)
data class Pengguna(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val role: Role,
    val permissions: String? = null, // JSON string
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

enum class Role(val displayName: String) {
    OWNER("Owner"),
    MANAGER("Manager"),
    CASHIER("Kasir"),
    WAREHOUSE("Staff Gudang")
}