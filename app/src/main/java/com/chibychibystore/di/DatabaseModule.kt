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

        return Room.databaseBuilder(
            context,
            ChibyChibyDatabase::class.java,
            "chiby_chiby_database"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
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
    fun provideUserSessionDao(database: ChibyChibyDatabase) = database.userSessionDao()
}