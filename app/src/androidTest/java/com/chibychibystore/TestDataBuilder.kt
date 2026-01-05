package com.chibychibystore

import com.chibychibystore.data.local.entity.KategoriPengeluaran
import com.chibychibystore.data.local.entity.Pengeluaran
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.ItemPenjualan
import com.chibychibystore.data.local.entity.PaymentMethod
import java.util.*

/**
 * Builder class for creating test data for financial integration tests
 */
object TestDataBuilder {

    /**
     * Create test expenses with various categories and amounts
     */
    fun createTestExpenses(createdBy: Long = 1, approvedBy: Long? = 1): List<Pengeluaran> {
        val baseDate = Date()
        return listOf(
            // COGS expenses
            Pengeluaran(
                id = 0,
                expenseDate = baseDate,
                category = KategoriPengeluaran.INVENTORY_PURCHASES,
                amount = 500000.0, // Rp 500k inventory purchase
                description = "Pembelian inventory makanan",
                approvedBy = approvedBy,
                createdBy = createdBy
            ),

            // Operating expenses
            Pengeluaran(
                id = 0,
                expenseDate = baseDate,
                category = KategoriPengeluaran.RENT_LEASE,
                amount = 3000000.0, // Rp 3M rent
                description = "Sewa toko bulan Desember",
                approvedBy = approvedBy,
                createdBy = createdBy
            ),

            Pengeluaran(
                id = 0,
                expenseDate = baseDate,
                category = KategoriPengeluaran.UTILITIES,
                amount = 500000.0, // Rp 500k utilities
                description = "Tagihan listrik dan air",
                approvedBy = approvedBy,
                createdBy = createdBy
            ),

            Pengeluaran(
                id = 0,
                expenseDate = baseDate,
                category = KategoriPengeluaran.SALARIES_WAGES,
                amount = 2000000.0, // Rp 2M salaries
                description = "Gaji karyawan",
                approvedBy = approvedBy,
                createdBy = createdBy
            ),

            Pengeluaran(
                id = 0,
                expenseDate = baseDate,
                category = KategoriPengeluaran.SUPPLIES_MAINTENANCE,
                amount = 300000.0, // Rp 300k supplies
                description = "Pembelian supplies toko",
                approvedBy = approvedBy,
                createdBy = createdBy
            ),

            // Large expense requiring approval
            Pengeluaran(
                id = 0,
                expenseDate = baseDate,
                category = KategoriPengeluaran.MARKETING_ADVERTISING,
                amount = 2000000.0, // Rp 2M marketing (above threshold)
                description = "Kampanye iklan sosial media",
                approvedBy = null, // Not approved yet
                createdBy = createdBy
            )
        )
    }

    /**
     * Create test sales for cash flow testing
     */
    fun createTestSales(cashierId: Long = 1): List<Pair<Penjualan, List<ItemPenjualan>>> {
        val baseDate = Date()
        return listOf(
            // Sale 1: Single item
            Pair(
                Penjualan(
                    id = 0,
                    saleDate = baseDate,
                    totalAmount = 20000.0,
                    paymentMethod = PaymentMethod.CASH,
                    cashierId = cashierId
                ),
                listOf(
                    ItemPenjualan(
                        id = 0,
                        saleId = 0, // Will be set after insertion
                        productId = 1,
                        quantity = 1,
                        unitPrice = 20000.0,
                        totalPrice = 20000.0
                    )
                )
            ),

            // Sale 2: Multiple items
            Pair(
                Penjualan(
                    id = 0,
                    saleDate = baseDate,
                    totalAmount = 60000.0,
                    paymentMethod = PaymentMethod.CASH,
                    cashierId = cashierId
                ),
                listOf(
                    ItemPenjualan(
                        id = 0,
                        saleId = 0, // Will be set after insertion
                        productId = 1,
                        quantity = 3,
                        unitPrice = 20000.0,
                        totalPrice = 60000.0
                    )
                )
            )
        )
    }

    /**
     * Get expected financial calculations for test validation
     */
    object ExpectedCalculations {
        // Based on test data above
        const val TOTAL_SALES_REVENUE = 80000.0 // 20000 + 60000
        const val TOTAL_COGS_EXPENSES = 500000.0 // Inventory purchases
        const val TOTAL_OPERATING_EXPENSES = 5800000.0 // Rent + utilities + salaries + supplies
        const val TOTAL_EXPENSES = 6300000.0 // COGS + operating

        // Gross profit = sales - COGS = 80000 - 500000 = negative (expected for test)
        const val GROSS_PROFIT = -420000.0

        // Net profit = gross profit - operating expenses
        const val NET_PROFIT = -4620000.0

        // Operating cash flow = sales - operating expenses - COGS
        // Unapproved expenses (2M marketing) are excluded from cash flow
        // Operating Expenses (Paid) = 5.8M - 2.0M = 3.8M
        // Cash Flow = 80k - 3.8M - 500k = -4.22M
        const val OPERATING_CASH_FLOW = -4220000.0

        // Inventory value (cost basis) = 100 units * 15000 = 1,500,000
        const val INVENTORY_VALUE = 1500000.0

        // Assets = inventory + cash (assuming cash = 0 for test)
        const val TOTAL_ASSETS = 1500000.0

        // Liabilities = Unapproved expenses (Marketing 2M)
        const val TOTAL_LIABILITIES = 2000000.0

        // Equity = assets - liabilities
        const val TOTAL_EQUITY = -500000.0
    }
}
