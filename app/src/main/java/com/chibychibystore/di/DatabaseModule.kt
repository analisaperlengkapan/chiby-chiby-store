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
                // Determine a valid default warehouse id to backfill into existing pembelian rows.
                // We can't blindly INSERT OR IGNORE (1, 'Gudang Utama', ...) because the gudang.name
                // unique index may already contain 'Gudang Utama' at a different id, which would
                // silently fail and leave no row at id=1, breaking the FK we add below.
                val now = System.currentTimeMillis()
                var defaultWarehouseId: Long = 1L

                val hasIdOne = database.query("SELECT id FROM gudang WHERE id = 1").use {
                    it.moveToFirst()
                }

                if (!hasIdOne) {
                    val existingId: Long? = database.query("SELECT id FROM gudang ORDER BY id LIMIT 1").use {
                        if (it.moveToFirst()) it.getLong(0) else null
                    }
                    defaultWarehouseId = if (existingId != null) {
                        // Reuse the smallest existing gudang id rather than introducing a new row,
                        // since the name 'Gudang Utama' may already be taken at another id.
                        existingId
                    } else {
                        // No gudang exists at all; safe to insert a default one.
                        database.execSQL(
                            "INSERT INTO gudang (name, location, capacity, createdAt) VALUES ('Gudang Utama', NULL, 0, ?)",
                            arrayOf<Any>(now)
                        )
                        database.query("SELECT last_insert_rowid()").use {
                            if (it.moveToFirst()) it.getLong(0) else 1L
                        }
                    }
                }

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

                // Copy data from old table, backfilling warehouseId to a valid existing gudang id.
                database.execSQL("""
                    INSERT INTO pembelian_new (id, purchaseDate, supplierId, warehouseId, invoiceNumber, totalAmount, notes, receivedBy, createdAt, updatedAt)
                    SELECT id, purchaseDate, supplierId, $defaultWarehouseId, invoiceNumber, totalAmount, notes, receivedBy, createdAt, updatedAt FROM pembelian
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

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shift` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `kasirId` INTEGER NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER,
                        `startingCash` REAL NOT NULL,
                        `expectedCash` REAL NOT NULL DEFAULT 0.0,
                        `actualCash` REAL,
                        `totalSales` REAL NOT NULL DEFAULT 0.0,
                        `totalExpenses` REAL NOT NULL DEFAULT 0.0,
                        `notes` TEXT,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`kasirId`) REFERENCES `pengguna`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_kasirId` ON `shift` (`kasirId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_startTime` ON `shift` (`startTime`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_shift_endTime` ON `shift` (`endTime`)")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stok_opname` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `auditDate` INTEGER NOT NULL,
                        `warehouseId` INTEGER NOT NULL,
                        `auditorId` INTEGER NOT NULL,
                        `notes` TEXT,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`warehouseId`) REFERENCES `gudang`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`auditorId`) REFERENCES `pengguna`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `item_stok_opname` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `auditId` INTEGER NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `expectedQuantity` INTEGER NOT NULL,
                        `actualQuantity` INTEGER NOT NULL,
                        `difference` INTEGER NOT NULL,
                        `reason` TEXT,
                        FOREIGN KEY(`auditId`) REFERENCES `stok_opname`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`productId`) REFERENCES `produk`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_stok_opname_auditDate` ON `stok_opname` (`auditDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_stok_opname_warehouseId` ON `stok_opname` (`warehouseId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_stok_opname_auditorId` ON `stok_opname` (`auditorId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_item_stok_opname_auditId` ON `item_stok_opname` (`auditId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_item_stok_opname_productId` ON `item_stok_opname` (`productId`)")
            }
        }

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add shiftId to penjualan table
                database.execSQL("ALTER TABLE penjualan ADD COLUMN shiftId INTEGER")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_penjualan_shiftId` ON `penjualan` (`shiftId`)")
                // Foreign keys on existing tables aren't supported by ALTER TABLE,
                // but Room handles the mapping. For a full FK enforcement we'd need table recreation.
            }
        }

        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pelanggan` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT,
                        `email` TEXT,
                        `address` TEXT,
                        `point` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_pelanggan_name` ON `pelanggan` (`name`)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_pelanggan_phone` ON `pelanggan` (`phone`)")
            }
        }

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // The `warehouseId` column was added to the Penjualan entity at schema version 6
                // without a corresponding SQL migration. Older databases (which migrated from v5
                // through v6 prior to this column being declared on the entity) may therefore be
                // missing the column on disk. Detect and add it before the full table recreation
                // so that the subsequent `SELECT ... warehouseId ... FROM penjualan` does not
                // reference a non-existent column.
                val hasWarehouseId = database.query("PRAGMA table_info(`penjualan`)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    var found = false
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex) == "warehouseId") {
                            found = true
                            break
                        }
                    }
                    found
                }
                if (!hasWarehouseId) {
                    database.execSQL("ALTER TABLE penjualan ADD COLUMN warehouseId INTEGER NOT NULL DEFAULT 1")
                }

                // Recreate penjualan table to add proper FK constraints for shiftId and pelangganId.
                // SQLite ALTER TABLE cannot add FK constraints, so a full table recreation is required
                // here so that Room's post-migration schema validation succeeds.
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `penjualan_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `saleDate` INTEGER NOT NULL,
                        `totalAmount` REAL NOT NULL,
                        `tax` REAL NOT NULL DEFAULT 0.0,
                        `discount` REAL NOT NULL DEFAULT 0.0,
                        `paymentMethod` TEXT NOT NULL,
                        `cashierId` INTEGER NOT NULL,
                        `shiftId` INTEGER,
                        `pelangganId` INTEGER,
                        `warehouseId` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL,
                        `isRefunded` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`cashierId`) REFERENCES `pengguna`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`shiftId`) REFERENCES `shift`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
                        FOREIGN KEY(`pelangganId`) REFERENCES `pelanggan`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())

                // Copy existing data. pelangganId column does not yet exist on the old table here
                // (MIGRATION_9_10 only added shiftId), so it's omitted from the SELECT and defaults to NULL.
                //
                // Backfill `totalAmount` to the new semantics. Prior to this PR, the repository
                // layer (PenjualanRepository.createPenjualan) overwrote totalAmount with the raw
                // item subtotal, silently discarding tax and discount. Going forward the service
                // layer persists `max(0, subtotal + tax - discount)` (the actual amount paid) so
                // that aggregate queries like getTotalSalesByShift / getTotalRevenue and shift
                // cash reconciliation produce correct results. Without this backfill, old rows
                // would store the subtotal while new rows store the post-tax/discount total, and
                // mixing them in a single SUM() would yield meaningless aggregates. Use MAX(0, …)
                // to mirror the service-layer floor and avoid introducing negative totals if a
                // legacy row had discount > subtotal + tax.
                database.execSQL("""
                    INSERT INTO penjualan_new (id, saleDate, totalAmount, tax, discount, paymentMethod, cashierId, shiftId, warehouseId, createdAt, isRefunded)
                    SELECT id, saleDate, MAX(0, totalAmount + tax - discount), tax, discount, paymentMethod, cashierId, shiftId, warehouseId, createdAt, isRefunded FROM penjualan
                """)

                database.execSQL("DROP TABLE penjualan")
                database.execSQL("ALTER TABLE penjualan_new RENAME TO penjualan")

                // Recreate all indices to match the entity definition.
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_penjualan_saleDate` ON `penjualan` (`saleDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_penjualan_cashierId_saleDate` ON `penjualan` (`cashierId`, `saleDate`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_penjualan_paymentMethod` ON `penjualan` (`paymentMethod`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_penjualan_shiftId` ON `penjualan` (`shiftId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_penjualan_pelangganId` ON `penjualan` (`pelangganId`)")
            }
        }

        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add pointsRedeemed and pointsEarned columns to penjualan table
                database.execSQL("ALTER TABLE penjualan ADD COLUMN pointsRedeemed INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE penjualan ADD COLUMN pointsEarned INTEGER NOT NULL DEFAULT 0")
                // Backfill pointsEarned for pre-existing sales using the legacy
                // formula (totalAmount / 10000). Without this, refunding any sale
                // created before the migration would reverse zero loyalty points,
                // even though points were awarded at sale time — a silent regression.
                database.execSQL(
                    "UPDATE penjualan SET pointsEarned = CAST(totalAmount / 10000 AS INTEGER) " +
                        "WHERE pelangganId IS NOT NULL AND isRefunded = 0"
                )
            }
        }

        return Room.databaseBuilder(
            context,
            ChibyChibyDatabase::class.java,
            "chiby_chiby_database"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
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

    @Provides
    fun provideShiftDao(database: ChibyChibyDatabase) = database.shiftDao()

    @Provides
    fun provideInventoryAuditDao(database: ChibyChibyDatabase) = database.inventoryAuditDao()

    @Provides
    fun providePelangganDao(database: ChibyChibyDatabase) = database.pelangganDao()
}
