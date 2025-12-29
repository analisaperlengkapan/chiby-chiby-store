package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chibychibystore.data.serialization.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
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
    /**
     * Indicates if the user account is active.
     * Inactive users cannot log in.
     */
    val isActive: Boolean = true,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date(),
    @Serializable(with = DateSerializer::class)
    val updatedAt: Date = Date()
)

@Serializable
enum class Role(val displayName: String) {
    OWNER("Owner"),
    MANAGER("Manager"),
    CASHIER("Kasir"),
    WAREHOUSE("Staff Gudang")
}