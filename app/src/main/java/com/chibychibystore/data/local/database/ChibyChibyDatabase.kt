package com.chibychibystore.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.chibychibystore.data.local.dao.*
import com.chibychibystore.data.local.entity.*

@Database(
    entities = [
        User::class,
        Category::class,
        Warehouse::class,
        Product::class,
        Supplier::class,
        Purchase::class,
        PurchaseItem::class,
        Sale::class,
        SaleItem::class,
        Expense::class,
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
    abstract fun supplierDao(): SupplierDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun purchaseItemDao(): PurchaseItemDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun userSessionDao(): UserSessionDao
}
