package com.chibychibystore.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chibychibystore.data.serialization.DateSerializer
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
@Entity(
    tableName = "shift",
    indices = [
        Index(value = ["kasirId"]),
        Index(value = ["startTime"]),
        Index(value = ["endTime"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Pengguna::class,
            parentColumns = ["id"],
            childColumns = ["kasirId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Shift(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kasirId: Long,
    @Serializable(with = DateSerializer::class)
    val startTime: Date = Date(),
    @Serializable(with = DateSerializer::class)
    val endTime: Date? = null,
    val startingCash: Double,
    val expectedCash: Double = 0.0, // Calculated from sales
    val actualCash: Double? = null, // Entered at closing
    val totalSales: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val notes: String? = null,
    val status: ShiftStatus = ShiftStatus.OPEN,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date()
)

enum class ShiftStatus {
    OPEN,
    CLOSED
}
