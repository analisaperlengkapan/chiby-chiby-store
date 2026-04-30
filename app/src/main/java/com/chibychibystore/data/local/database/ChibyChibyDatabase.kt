package com.chibychibystore.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chibychibystore.data.local.dao.*
import com.chibychibystore.data.local.entity.*

@Database(
    entities = [
        Pengguna::class,
        Kategori::class,
        Gudang::class,
        Produk::class,
        Pemasok::class,
        Pembelian::class,
        ItemPembelian::class,
        Penjualan::class,
        ItemPenjualan::class,
        Pengeluaran::class,
        PenggunaSession::class,
        StokGudang::class,
        Promotion::class,
        Shift::class,
        StokOpname::class,
        ItemStokOpname::class,
        Pelanggan::class
    ],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChibyChibyDatabase : RoomDatabase() {
    abstract fun penggunaDao(): PenggunaDao
    abstract fun kategoriDao(): KategoriDao
    abstract fun gudangDao(): GudangDao
    abstract fun produkDao(): ProdukDao
    abstract fun pemasokDao(): PemasokDao
    abstract fun pembelianDao(): PembelianDao
    abstract fun itemPembelianDao(): ItemPembelianDao
    abstract fun penjualanDao(): PenjualanDao
    abstract fun itemPenjualanDao(): ItemPenjualanDao
    abstract fun pengeluaranDao(): PengeluaranDao
    abstract fun penggunaSessionDao(): PenggunaSessionDao
    abstract fun stokGudangDao(): StokGudangDao
    abstract fun promotionDao(): PromotionDao
    abstract fun shiftDao(): ShiftDao
    abstract fun inventoryAuditDao(): InventoryAuditDao
    abstract fun pelangganDao(): PelangganDao
}
