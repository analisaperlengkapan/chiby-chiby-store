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

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `promotion` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `value` REAL NOT NULL,
                        `minPurchaseAmount` REAL NOT NULL,
                        `maxDiscountAmount` REAL,
                        `isActive` INTEGER NOT NULL,
                        `startDate` INTEGER,
                        `endDate` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Create new table with warehouseId and correct indices/FKs
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pembelian_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `purchaseDate` INTEGER NOT NULL,
                        `supplierId` INTEGER NOT NULL,
                        `warehouseId` INTEGER NOT NULL DEFAULT 1,
                        `invoiceNumber` TEXT NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `notes` TEXT,
                        `receivedBy` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`supplierId`) REFERENCES `pemasok`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`receivedBy`) REFERENCES `pengguna`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`warehouseId`) REFERENCES `gudang`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())

                // Copy data from old table
                database.execSQL("""
                    INSERT INTO pembelian_new (id, purchaseDate, supplierId, invoiceNumber, totalAmount, notes, receivedBy, createdAt, updatedAt)
                    SELECT id, purchaseDate, supplierId, invoiceNumber, totalAmount, notes, receivedBy, createdAt, updatedAt FROM pembelian
                """)

                // Drop old table
                database.execSQL("DROP TABLE pembelian")

                // Rename new table
                database.execSQL("ALTER TABLE pembelian_new RENAME TO pembelian")

                // Recreate indices
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_pembelian_purchaseDate` ON `pembelian` (`purchaseDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_pembelian_supplierId` ON `pembelian` (`supplierId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_pembelian_receivedBy` ON `pembelian` (`receivedBy`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_pembelian_warehouseId` ON `pembelian` (`warehouseId`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pembelian_invoiceNumber` ON `pembelian` (`invoiceNumber`)")
            }
        }

        return Room.databaseBuilder(
            context,
            ChibyChibyDatabase::class.java,
            "chiby_chiby_database"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePenggunaDao(database: ChibyChibyDatabase) = database.penggunaDao()

    @Provides
    fun provideKategoriDao(database: ChibyChibyDatabase) = database.kategoriDao()

    @Provides
    fun provideGudangDao(database: ChibyChibyDatabase) = database.gudangDao()

    @Provides
    fun provideProdukDao(database: ChibyChibyDatabase) = database.produkDao()

    @Provides
    fun providePemasokDao(database: ChibyChibyDatabase) = database.pemasokDao()

    @Provides
    fun providePembelianDao(database: ChibyChibyDatabase) = database.pembelianDao()

    @Provides
    fun provideItemPembelianDao(database: ChibyChibyDatabase) = database.itemPembelianDao()

    @Provides
    fun providePenjualanDao(database: ChibyChibyDatabase) = database.penjualanDao()

    @Provides
    fun provideItemPenjualanDao(database: ChibyChibyDatabase) = database.itemPenjualanDao()

    @Provides
    fun providePengeluaranDao(database: ChibyChibyDatabase) = database.pengeluaranDao()

    @Provides
    fun providePenggunaSessionDao(database: ChibyChibyDatabase) = database.penggunaSessionDao()

    @Provides
    fun provideStokGudangDao(database: ChibyChibyDatabase) = database.stokGudangDao()

    @Provides
    fun providePromotionDao(database: ChibyChibyDatabase) = database.promotionDao()
}
