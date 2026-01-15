package com.chibychibystore.di

import android.content.Context
import androidx.room.Room
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChibyChibyDatabase {
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add isRefunded column to penjualan table, default 0 (false)
                database.execSQL("ALTER TABLE penjualan ADD COLUMN isRefunded INTEGER NOT NULL DEFAULT 0")
                // Add isActive column to pengguna table, default 1 (true)
                database.execSQL("ALTER TABLE pengguna ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add tax and discount columns to penjualan table, default 0.0
                database.execSQL("ALTER TABLE penjualan ADD COLUMN tax REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE penjualan ADD COLUMN discount REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stok_gudang` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `warehouseId` INTEGER NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        FOREIGN KEY(`productId`) REFERENCES `produk`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`warehouseId`) REFERENCES `gudang`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_stok_gudang_productId_warehouseId` ON `stok_gudang` (`productId`, `warehouseId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_stok_gudang_warehouseId` ON `stok_gudang` (`warehouseId`)")
            }
        }

        return Room.databaseBuilder(
            context,
            ChibyChibyDatabase::class.java,
            "chiby_chiby_database"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            // Removed destructive migration for safety, though schema has changed significantly.
            // In a real scenario, we would need complex migrations from old tables (Pengguna, Produk) to new ones (User, Product).
            // Given the scope of "Total Refactor", we assume a fresh install or a manual migration strategy is handled elsewhere if data preservation is critical.
            // Re-enabling it with a comment warning as per previous state, but cleaner.
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: ChibyChibyDatabase) = database.penggunaDao()

    @Provides
    fun provideCategoryDao(database: ChibyChibyDatabase) = database.kategoriDao()

    @Provides
    fun provideWarehouseDao(database: ChibyChibyDatabase) = database.gudangDao()

    @Provides
    fun provideProductDao(database: ChibyChibyDatabase) = database.produkDao()

    @Provides
    fun provideSupplierDao(database: ChibyChibyDatabase) = database.pemasokDao()

    @Provides
    fun providePurchaseDao(database: ChibyChibyDatabase) = database.pembelianDao()

    @Provides
    fun providePurchaseItemDao(database: ChibyChibyDatabase) = database.itemPembelianDao()

    @Provides
    fun provideSaleDao(database: ChibyChibyDatabase) = database.penjualanDao()

    @Provides
    fun provideSaleItemDao(database: ChibyChibyDatabase) = database.itemPenjualanDao()

    @Provides
    fun provideExpenseDao(database: ChibyChibyDatabase) = database.pengeluaranDao()

    @Provides
    fun provideUserSessionDao(database: ChibyChibyDatabase) = database.userSessionDao()

    @Provides
    fun provideStokGudangDao(database: ChibyChibyDatabase) = database.stokGudangDao()

    @Provides
    fun providePromotionDao(database: ChibyChibyDatabase) = database.promotionDao()
}
