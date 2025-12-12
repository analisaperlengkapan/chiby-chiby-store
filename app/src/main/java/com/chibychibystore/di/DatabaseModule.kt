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
        return Room.databaseBuilder(
            context,
            ChibyChibyDatabase::class.java,
            "chiby_chiby_database"
        )
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
}