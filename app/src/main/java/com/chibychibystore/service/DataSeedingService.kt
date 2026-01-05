package com.chibychibystore.service

import com.chibychibystore.data.local.entity.*
import com.chibychibystore.repository.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataSeedingService @Inject constructor(
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val warehouseRepository: WarehouseRepository,
    private val productRepository: ProductRepository,
    private val supplierRepository: SupplierRepository,
    private val expenseRepository: ExpenseRepository
) {

    suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        // Seed default users
        seedUsers()

        // Seed categories
        seedCategories()

        // Seed warehouses
        seedWarehouses()

        // Seed suppliers
        seedSuppliers()

        // Seed products
        seedProducts()

        // Seed expenses
        seedExpenses()
    }

    private suspend fun seedUsers() {
        val defaultUsers = listOf(
            User(
                id = 0,
                username = "owner",
                passwordHash = hashPassword("owner123"),
                role = Role.OWNER
            ),
            User(
                id = 0,
                username = "manager",
                passwordHash = hashPassword("manager123"),
                role = Role.MANAGER
            ),
            User(
                id = 0,
                username = "cashier",
                passwordHash = hashPassword("cashier123"),
                role = Role.CASHIER
            ),
            User(
                id = 0,
                username = "warehouse",
                passwordHash = hashPassword("warehouse123"),
                role = Role.WAREHOUSE
            )
        )

        defaultUsers.forEach { user ->
            userRepository.createUser(user)
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private suspend fun seedCategories() {
        val defaultCategories = listOf(
            Category(id = 0, name = "Makanan", description = "Product makanan"),
            Category(id = 0, name = "Minuman", description = "Product minuman"),
            Category(id = 0, name = "Pakaian", description = "Product pakaian"),
            Category(id = 0, name = "Elektronik", description = "Product elektronik")
        )

        defaultCategories.forEach { category ->
            categoryRepository.createCategory(category)
        }
    }

    private suspend fun seedWarehouses() {
        val defaultWarehouses = listOf(
            Warehouse(id = 0, name = "Gudang Utama", location = "Jakarta"),
            Warehouse(id = 0, name = "Gudang Cabang", location = "Bandung")
        )

        defaultWarehouses.forEach { warehouse ->
            warehouseRepository.createWarehouse(warehouse)
        }
    }

    private suspend fun seedSuppliers() {
        val defaultSuppliers = listOf(
            Supplier(id = 0, name = "PT Supplier A", contact = "021-123456", address = "Jl. Supplier A"),
            Supplier(id = 0, name = "CV Supplier B", contact = "021-654321", address = "Jl. Supplier B")
        )

        defaultSuppliers.forEach { supplier ->
            supplierRepository.createSupplier(supplier)
        }
    }

    private suspend fun seedProducts() {
        val defaultProducts = listOf(
            Product(
                id = 0,
                name = "Nasi Goreng",
                barcode = "123456789012",
                categoryId = 1, // Assuming category IDs start from 1
                warehouseId = 1,
                costPrice = 15000.0,
                sellingPrice = 20000.0,
                stockQuantity = 100,
                minStock = 10
            ),
            Product(
                id = 0,
                name = "Teh Botol",
                barcode = "123456789013",
                categoryId = 2,
                warehouseId = 1,
                costPrice = 3000.0,
                sellingPrice = 5000.0,
                stockQuantity = 200,
                minStock = 20
            ),
            Product(
                id = 0,
                name = "Kaos Polos",
                barcode = "123456789014",
                categoryId = 3,
                warehouseId = 2,
                costPrice = 25000.0,
                sellingPrice = 35000.0,
                stockQuantity = 50,
                minStock = 5
            ),
            Product(
                id = 0,
                name = "Handphone",
                barcode = "123456789015",
                categoryId = 4,
                warehouseId = 2,
                costPrice = 1000000.0,
                sellingPrice = 1200000.0,
                stockQuantity = 10,
                minStock = 2
            )
        )

        defaultProducts.forEach { product ->
            productRepository.createProduct(product)
        }
    }

    private suspend fun seedExpenses() {
        val defaultExpenses = listOf(
            Expense(
                id = 0,
                expenseDate = java.util.Date(),
                category = ExpenseCategory.RENT_LEASE,
                amount = 5000000.0, // Rp 5 juta sewa bulanan
                description = "Sewa toko bulan ini",
                approvedBy = 1, // Owner
                createdBy = 1
            ),
            Expense(
                id = 0,
                expenseDate = java.util.Date(),
                category = ExpenseCategory.UTILITIES,
                amount = 800000.0, // Rp 800 ribu listrik
                description = "Tagihan listrik bulan ini",
                approvedBy = 1,
                createdBy = 1
            ),
            Expense(
                id = 0,
                expenseDate = java.util.Date(),
                category = ExpenseCategory.SALARIES_WAGES,
                amount = 3000000.0, // Rp 3 juta gaji karyawan
                description = "Gaji karyawan bulan ini",
                approvedBy = 1,
                createdBy = 1
            ),
            Expense(
                id = 0,
                expenseDate = java.util.Date(),
                category = ExpenseCategory.SUPPLIES_MAINTENANCE,
                amount = 500000.0, // Rp 500 ribu supplies
                description = "Pembelian supplies toko",
                approvedBy = 1,
                createdBy = 1
            ),
            Expense(
                id = 0,
                expenseDate = java.util.Date(),
                category = ExpenseCategory.INVENTORY_PURCHASES,
                amount = 2000000.0, // Rp 2 juta pembelian inventory
                description = "Pembelian inventory dari supplier",
                approvedBy = 1,
                createdBy = 1
            )
        )

        defaultExpenses.forEach { expense ->
            expenseRepository.createExpense(expense)
        }
    }
}
