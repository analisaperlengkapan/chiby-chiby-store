package com.chibychibystore.data.model

data class KategoriPenjualanDto(
    val kategoriId: Long,
    val jumlahTerjual: Long,
    val totalPendapatan: Double,
    val totalBiaya: Double
)
