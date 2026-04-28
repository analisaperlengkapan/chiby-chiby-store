package com.chibychibystore.di

import com.chibychibystore.repository.*
import com.chibychibystore.service.*
import com.chibychibystore.service.impl.*
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
    abstract fun bindPurchaseService(
        purchaseServiceImpl: PurchaseServiceImpl
    ): PurchaseService

    @Binds
    @Singleton
    abstract fun bindReportingService(
        reportingServiceImpl: ReportingServiceImpl
    ): ReportingService

    @Binds
    @Singleton
    abstract fun bindBarcodeService(
        barcodeServiceImpl: BarcodeServiceImpl
    ): BarcodeService

    @Binds
    @Singleton
    abstract fun bindExpenseService(
        expenseServiceImpl: ExpenseServiceImpl
    ): ExpenseService

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

    @Binds
    @Singleton
    abstract fun bindSupplierService(
        supplierServiceImpl: SupplierServiceImpl
    ): SupplierService

    @Binds
    @Singleton
    abstract fun bindPromoService(
        promoServiceImpl: PromoServiceImpl
    ): PromoService

    companion object {
        @Provides
        @Singleton
        fun provideBluetoothAdapter(): android.bluetooth.BluetoothAdapter? {
            return try {
                android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            } catch (e: Exception) {
                null
            }
        }
    }
}