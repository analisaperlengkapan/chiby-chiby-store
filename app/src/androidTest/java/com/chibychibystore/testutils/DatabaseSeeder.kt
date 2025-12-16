package com.chibychibystore.testutils

import com.chibychibystore.data.local.AppDatabase
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.Kategori
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

/**
 * Database seeding utilities for integration tests
 */
class DatabaseSeeder(private val database: AppDatabase) {

    /**
     * Seed all test data
     */
    fun seedAllData() = runBlocking {
        seedCategories()
        seedWarehouses()
        seedProducts()
        seedSales()
        seedSaleItems()
        seedExpenses()
    }

    /**
     * Seed test categories
     */
    suspend fun seedCategories(categories: List<Kategori> = TestDataBuilder.testCategories) {
        categories.forEach { category ->
            database.kategoriDao().insertKategori(category)
        }
    }

    /**
     * Seed test warehouses
     */
    suspend fun seedWarehouses(warehouses: List<Gudang> = TestDataBuilder.testWarehouses) {
        warehouses.forEach { warehouse ->
            database.gudangDao().insertGudang(warehouse)
        }
    }

    /**
     * Seed test products
     */
    suspend fun seedProducts(products: List<Produk> = TestDataBuilder.testProducts) {
        products.forEach { product ->
            database.produkDao().insertProduk(product)
        }
    }

    /**
     * Seed test sales
     */
    suspend fun seedSales(sales: List<Penjualan> = TestDataBuilder.testSales) {
        sales.forEach { sale ->
            database.penjualanDao().insertPenjualan(sale)
        }
    }

    /**
     * Seed test sale items
     */
    suspend fun seedSaleItems(saleItems: List<ItemPenjualan> = TestDataBuilder.testSaleItems) {
        saleItems.forEach { item ->
            database.itemPenjualanDao().insertItemPenjualan(item)
        }
    }

    /**
     * Seed test expenses
     */
    suspend fun seedExpenses(expenses: List<Pengeluaran> = TestDataBuilder.testExpenses) {
        expenses.forEach { expense ->
            database.pengeluaranDao().insertPengeluaran(expense)
        }
    }

    /**
     * Clear all data
     */
    fun clearAllData() = runBlocking {
        database.clearAllTables()
    }

    /**
     * Seed minimal data for specific tests
     */
    suspend fun seedMinimalData() {
        seedCategories(listOf(TestDataBuilder.testCategories.first()))
        seedWarehouses(listOf(TestDataBuilder.testWarehouses.first()))
        seedProducts(listOf(TestDataBuilder.testProducts.first()))
        seedSales(listOf(TestDataBuilder.testSales.first()))
        seedSaleItems(TestDataBuilder.testSaleItems.filter { it.saleId == TestDataBuilder.testSales.first().id })
        seedExpenses(listOf(TestDataBuilder.testExpenses.first()))
    }

    /**
     * Get current product count
     */
    suspend fun getProductCount(): Int {
        return database.produkDao().getAllProduk().first().size
    }

    /**
     * Get current warehouse count
     */
    suspend fun getWarehouseCount(): Int {
        return database.gudangDao().getAllGudang().first().size
    }

    /**
     * Get current category count
     */
    suspend fun getCategoryCount(): Int {
        return database.kategoriDao().getAllKategori().first().size
    }

    /**
     * Get current sales count
     */
    suspend fun getSalesCount(): Int {
        return database.penjualanDao().getAllPenjualan().first().size
    }

    /**
     * Get current sale items count
     */
    suspend fun getSaleItemsCount(): Int {
        val sales = database.penjualanDao().getAllPenjualan().first()
        var total = 0
        for (sale in sales) {
            total += database.itemPenjualanDao().getItemCountBySaleId(sale.id)
        }
        return total
    }

    /**
     * Get current expenses count
     */
    suspend fun getExpensesCount(): Int {
        return database.pengeluaranDao().getAllPengeluaran().first().size
    }
}