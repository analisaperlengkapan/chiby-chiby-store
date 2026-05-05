package com.chibychibystore.data.model

data class ProdukTerpopulerDto(
    val produkId: Long,
    val jumlahTerjual: Int,
    val totalPendapatan: Double,
    val totalBiaya: Double = 0.0
)
