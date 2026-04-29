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
    tableName = "stok_opname",
    indices = [
        Index(value = ["auditDate"]),
        Index(value = ["warehouseId"]),
        Index(value = ["auditorId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Gudang::class,
            parentColumns = ["id"],
            childColumns = ["warehouseId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Pengguna::class,
            parentColumns = ["id"],
            childColumns = ["auditorId"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class StokOpname(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @Serializable(with = DateSerializer::class)
    val auditDate: Date = Date(),
    val warehouseId: Long,
    val auditorId: Long,
    val notes: String? = null,
    val status: AuditStatus = AuditStatus.DRAFT,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date = Date()
)

@Serializable
enum class AuditStatus {
    DRAFT,
    COMPLETED,
    CANCELLED
}
