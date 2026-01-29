package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_sessions")
data class PenggunaSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val loginTime: Date = Date(),
    val lastActivityTime: Date = Date(),
    val isActive: Boolean = true,
    val deviceInfo: String? = null // Optional device identifier
)
