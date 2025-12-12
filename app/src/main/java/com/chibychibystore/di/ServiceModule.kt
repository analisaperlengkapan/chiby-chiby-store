package com.chibychibystore.di

import com.chibychibystore.repository.*
import com.chibychibystore.service.*
import com.chibychibystore.service.printer.PrinterService
import com.chibychibystore.service.printer.PrinterServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {

    @Binds
    @Singleton
    abstract fun bindAuthService(
        authServiceImpl: AuthServiceImpl
    ): AuthService

    @Binds
    @Singleton
    abstract fun bindUserManagementService(
        userManagementServiceImpl: UserManagementServiceImpl
    ): UserManagementService

    @Binds
    @Singleton
    abstract fun bindDataSeedingService(
        dataSeedingService: DataSeedingService
    ): DataSeedingService

    @Binds
    @Singleton
    abstract fun bindPenggunaRepository(
        penggunaRepository: PenggunaRepository
    ): PenggunaRepository

    @Binds
    @Singleton
    abstract fun bindUserSessionRepository(
        userSessionRepository: UserSessionRepository
    ): UserSessionRepository

    @Binds
    @Singleton
    abstract fun bindKategoriRepository(
        kategoriRepository: KategoriRepository
    ): KategoriRepository

    @Binds
    @Singleton
    abstract fun bindGudangRepository(
        gudangRepository: GudangRepository
    ): GudangRepository

    @Binds
    @Singleton
    abstract fun bindProdukRepository(
        produkRepository: ProdukRepository
    ): ProdukRepository

    @Binds
    @Singleton
    abstract fun bindPemasokRepository(
        pemasokRepository: PemasokRepository
    ): PemasokRepository

    @Binds
    @Singleton
    abstract fun bindPenjualanRepository(
        penjualanRepository: PenjualanRepository
    ): PenjualanRepository

    @Binds
    @Singleton
    abstract fun bindItemPenjualanRepository(
        itemPenjualanRepository: ItemPenjualanRepository
    ): ItemPenjualanRepository

    @Binds
    @Singleton
    abstract fun bindItemPembelianRepository(
        itemPembelianRepository: ItemPembelianRepository
    ): ItemPembelianRepository

    @Binds
    @Singleton
    abstract fun bindPembelianRepository(
        pembelianRepository: PembelianRepository
    ): PembelianRepository

    @Binds
    @Singleton
    abstract fun bindPengeluaranRepository(
        pengeluaranRepository: PengeluaranRepository
    ): PengeluaranRepository

    @Binds
    @Singleton
    abstract fun bindProductService(
        productServiceImpl: ProductServiceImpl
    ): ProductService

    @Binds
    @Singleton
    abstract fun bindWarehouseService(
        warehouseServiceImpl: WarehouseServiceImpl
    ): WarehouseService

    @Binds
    @Singleton
    abstract fun bindSaleService(
        saleServiceImpl: SaleServiceImpl
    ): SaleService

    @Binds
    @Singleton
    abstract fun bindReportingService(
        reportingService: ReportingService
    ): ReportingService

    @Binds
    @Singleton
    abstract fun bindExpenseService(
        expenseService: ExpenseService
    ): ExpenseService

    @Binds
    @Singleton
    abstract fun bindCashManagementService(
        cashManagementService: CashManagementService
    ): CashManagementService

    @Binds
    @Singleton
    abstract fun bindBarcodeService(
        barcodeServiceImpl: BarcodeServiceImpl
    ): BarcodeService

    @Binds
    @Singleton
    abstract fun bindBackupService(
        backupServiceImpl: BackupServiceImpl
    ): BackupService

    @Binds
    @Singleton
    abstract fun bindRestoreService(
        restoreServiceImpl: RestoreServiceImpl
    ): RestoreService

    @Binds
    @Singleton
    abstract fun bindPrinterService(
        printerServiceImpl: PrinterServiceImpl
    ): PrinterService


    companion object {
        @Provides
        @Singleton
        fun provideAuthUseCases(
            login: com.chibychibystore.usecase.LoginUseCase,
            logout: com.chibychibystore.usecase.LogoutUseCase,
            changePassword: com.chibychibystore.usecase.ChangePasswordUseCase,
            getCurrentUser: com.chibychibystore.usecase.GetCurrentUserUseCase,
            checkPermission: com.chibychibystore.usecase.CheckPermissionUseCase
        ): com.chibychibystore.usecase.AuthUseCases {
            return com.chibychibystore.usecase.AuthUseCases(
                login, logout, changePassword, getCurrentUser, checkPermission
            )
        }
    }
}