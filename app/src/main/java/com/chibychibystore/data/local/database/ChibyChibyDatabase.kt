package com.chibychibystore.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chibychibystore.data.local.dao.WarehouseDao
import com.chibychibystore.data.local.dao.ItemPembelianDao
import com.chibychibystore.data.local.dao.SaleItemDao
import com.chibychibystore.data.local.dao.CategoryDao
import com.chibychibystore.data.local.dao.PemasokDao
import com.chibychibystore.data.local.dao.PembelianDao
import com.chibychibystore.data.local.dao.PengeluaranDao
import com.chibychibystore.data.local.dao.SaleDao
import com.chibychibystore.data.local.dao.UserDao
import com.chibychibystore.data.local.dao.ProductDao
import com.chibychibystore.data.local.dao.UserSessionDao
import com.chibychibystore.data.local.entity.UserSession
import com.chibychibystore.data.local.entity.Warehouse
import com.chibychibystore.data.local.entity.ItemPembelian
import com.chibychibystore.data.local.entity.SaleItem
import com.chibychibystore.data.local.entity.Category
import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.data.local.entity.Pembelian
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.local.entity.Sale
import com.chibychibystore.data.local.entity.User
import com.chibychibystore.data.local.entity.Product

@Database(
    entities = [
        User::class,
        Category::class,
        Warehouse::class,
        Product::class,
        Pemasok::class,
        Pembelian::class,
        ItemPembelian::class,
        Sale::class,
        SaleItem::class,
        Pengeluaran::class,
        UserSession::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ChibyChibyDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun productDao(): ProductDao
    abstract fun pemasokDao(): PemasokDao
    abstract fun pembelianDao(): PembelianDao
    abstract fun itemPembelianDao(): ItemPembelianDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun pengeluaranDao(): PengeluaranDao
    abstract fun userSessionDao(): UserSessionDao
}
