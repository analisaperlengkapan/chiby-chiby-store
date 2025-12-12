package com.chibychibystore.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chibychibystore.data.local.dao.GudangDao
import com.chibychibystore.data.local.dao.ItemPembelianDao
import com.chibychibystore.data.local.dao.ItemPenjualanDao
import com.chibychibystore.data.local.dao.KategoriDao
import com.chibychibystore.data.local.dao.PemasokDao
import com.chibychibystore.data.local.dao.PembelianDao
import com.chibychibystore.data.local.dao.PengeluaranDao
import com.chibychibystore.data.local.dao.PenjualanDao
import com.chibychibystore.data.local.dao.PenggunaDao
import com.chibychibystore.data.local.dao.ProdukDao
import com.chibychibystore.data.local.dao.UserSessionDao
import com.chibychibystore.data.local.entity.UserSession
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.ItemPembelian
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Pengguna
import com.chibychibystore.data.local.entity.Produk

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
        UserSession::class
    ],
    version = 2,
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
    abstract fun userSessionDao(): UserSessionDao
}