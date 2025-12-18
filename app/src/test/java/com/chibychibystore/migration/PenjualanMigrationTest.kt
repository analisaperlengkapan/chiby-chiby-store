package com.chibychibystore.migration

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.chibychibystore.data.local.database.ChibyChibyDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import androidx.room.migration.Migration
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PenjualanMigrationTest {

    private lateinit var context: Context
    private val TEST_DB = "migration-test.db"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Ensure previous file removed
        File(context.filesDir, TEST_DB).delete()
        // Create a version 2 DB with penjualan table (without isRefunded) and a pengguna row
        val configuration = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DB)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // minimal pengguna table for FK
                    db.execSQL("""
                        CREATE TABLE pengguna (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          username TEXT NOT NULL,
                          passwordHash TEXT NOT NULL,
                          role TEXT NOT NULL,
                          permissions TEXT,
                          createdAt INTEGER NOT NULL,
                          updatedAt INTEGER NOT NULL
                        )
                    """.trimIndent())

                    db.execSQL("INSERT INTO pengguna (id, username, passwordHash, role, createdAt, updatedAt) VALUES (1, 'u', 'p', 'CASHIER', 0, 0)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pengguna_username ON pengguna(username)")

                    // kategori table as in version 2 (for schema validation)
                    db.execSQL("""
                        CREATE TABLE kategori (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          name TEXT NOT NULL,
                          description TEXT,
                          createdAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_kategori_name ON kategori(name)")

                    // penjualan table as in version 2 (no isRefunded)
                    db.execSQL("""
                        CREATE TABLE penjualan (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          saleDate INTEGER NOT NULL,
                          totalAmount REAL NOT NULL,
                          paymentMethod TEXT NOT NULL,
                          cashierId INTEGER NOT NULL,
                          createdAt INTEGER NOT NULL,
                          FOREIGN KEY(cashierId) REFERENCES pengguna(id) ON DELETE CASCADE
                        )
                    """.trimIndent())

                    db.execSQL("INSERT INTO penjualan (id, saleDate, totalAmount, paymentMethod, cashierId, createdAt) VALUES (1, 0, 100.0, 'CASH', 1, 0)")
                    db.execSQL("INSERT INTO penjualan (id, saleDate, totalAmount, paymentMethod, cashierId, createdAt) VALUES (2, 1, 200.0, 'CASH', 1, 1)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_penjualan_cashierId ON penjualan(cashierId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_penjualan_saleDate ON penjualan(saleDate)")

                    // gudang (warehouse) table
                    db.execSQL("""
                        CREATE TABLE gudang (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          name TEXT NOT NULL,
                          location TEXT,
                          capacity INTEGER NOT NULL,
                          createdAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_gudang_name ON gudang(name)")

                    // kategori table already created above

                    // pemasok (supplier) table
                    db.execSQL("""
                        CREATE TABLE pemasok (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          name TEXT NOT NULL,
                          contact TEXT,
                          address TEXT,
                          createdAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pemasok_name ON pemasok(name)")

                    // produk (product) table
                    db.execSQL("""
                        CREATE TABLE produk (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          name TEXT NOT NULL,
                          barcode TEXT,
                          categoryId INTEGER NOT NULL,
                          costPrice REAL NOT NULL,
                          sellingPrice REAL NOT NULL,
                          stockQuantity INTEGER NOT NULL,
                          warehouseId INTEGER NOT NULL,
                          minStock INTEGER NOT NULL,
                          createdAt INTEGER NOT NULL,
                          updatedAt INTEGER NOT NULL,
                          FOREIGN KEY(categoryId) REFERENCES kategori(id) ON DELETE CASCADE,
                          FOREIGN KEY(warehouseId) REFERENCES gudang(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_produk_barcode ON produk(barcode)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_produk_category ON produk(categoryId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_produk_warehouse ON produk(warehouseId)")

                    // pembelian (purchase) table
                    db.execSQL("""
                        CREATE TABLE pembelian (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          supplierId INTEGER NOT NULL,
                          purchaseDate INTEGER NOT NULL,
                          totalAmount REAL NOT NULL,
                          createdBy INTEGER NOT NULL,
                          createdAt INTEGER NOT NULL,
                          FOREIGN KEY(supplierId) REFERENCES pemasok(id) ON DELETE CASCADE,
                          FOREIGN KEY(createdBy) REFERENCES pengguna(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pembelian_purchaseDate ON pembelian(purchaseDate)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pembelian_supplierId ON pembelian(supplierId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pembelian_createdBy ON pembelian(createdBy)")

                    // item_pembelian table
                    db.execSQL("""
                        CREATE TABLE item_pembelian (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          purchaseId INTEGER NOT NULL,
                          productId INTEGER NOT NULL,
                          quantity INTEGER NOT NULL,
                          unitPrice REAL NOT NULL,
                          totalPrice REAL NOT NULL,
                          FOREIGN KEY(purchaseId) REFERENCES pembelian(id) ON DELETE CASCADE,
                          FOREIGN KEY(productId) REFERENCES produk(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_item_pembelian_purchase ON item_pembelian(purchaseId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_item_pembelian_product ON item_pembelian(productId)")

                    // item_penjualan table
                    db.execSQL("""
                        CREATE TABLE item_penjualan (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          saleId INTEGER NOT NULL,
                          productId INTEGER NOT NULL,
                          quantity INTEGER NOT NULL,
                          unitPrice REAL NOT NULL,
                          totalPrice REAL NOT NULL,
                          FOREIGN KEY(saleId) REFERENCES penjualan(id) ON DELETE CASCADE,
                          FOREIGN KEY(productId) REFERENCES produk(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_items_sale ON item_penjualan(saleId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_sale_items_product ON item_penjualan(productId)")

                    // pengeluaran (expenses) table
                    db.execSQL("""
                        CREATE TABLE pengeluaran (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          expenseDate INTEGER NOT NULL,
                          category TEXT NOT NULL,
                          amount REAL NOT NULL,
                          description TEXT,
                          approvedBy INTEGER,
                          createdBy INTEGER NOT NULL,
                          createdAt INTEGER NOT NULL,
                          FOREIGN KEY(approvedBy) REFERENCES pengguna(id) ON DELETE SET NULL,
                          FOREIGN KEY(createdBy) REFERENCES pengguna(id) ON DELETE CASCADE
                        )
                    """.trimIndent())
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pengeluaran_expenseDate ON pengeluaran(expenseDate)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pengeluaran_category ON pengeluaran(category)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pengeluaran_createdBy ON pengeluaran(createdBy)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_pengeluaran_approvedBy ON pengeluaran(approvedBy)")

                    // user_sessions table
                    db.execSQL("""
                        CREATE TABLE user_sessions (
                          id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          userId INTEGER NOT NULL,
                          loginTime INTEGER NOT NULL,
                          lastActivityTime INTEGER NOT NULL,
                          isActive INTEGER NOT NULL,
                          deviceInfo TEXT
                        )
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                }
            }).build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        helper.writableDatabase.close()
    }

    @After
    fun teardown() {
        File(context.filesDir, TEST_DB).delete()
    }

    @Test
    fun migration_2_3_addsIsRefundedDefaultZero() {
        // Define the same migration as in DatabaseModule
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE penjualan ADD COLUMN isRefunded INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE pengguna ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }

        // Open Room DB with migration
        val db = Room.databaseBuilder(context, ChibyChibyDatabase::class.java, TEST_DB)
            .addMigrations(MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        // Verify column exists with default 0 for multiple rows
        val sqLite = db.openHelper.readableDatabase
        val cursor = sqLite.query("SELECT isRefunded FROM penjualan WHERE id IN (1,2) ORDER BY id")
        cursor.use {
            it.moveToFirst()
            val first = it.getInt(0)
            assertEquals(0, first)

            it.moveToNext()
            val second = it.getInt(0)
            assertEquals(0, second)
        }

        // Verify isActive column in pengguna
        val cursor2 = sqLite.query("SELECT isActive FROM pengguna WHERE id = 1")
        cursor2.use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }

        db.close()
    }
}
