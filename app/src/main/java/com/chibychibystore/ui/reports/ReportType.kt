package com.chibychibystore.ui.reports

/**
 * Enum untuk tipe laporan yang tersedia
 */
enum class ReportType(val displayName: String) {
    GROSS_SALES("Sale Kotor"),
    PROFIT_MARGIN("Margin Keuntungan"),
    NET_PROFIT("Keuntungan Bersih"),
    SALES_BY_PRODUCT("Sale per Product"),
    SALES_BY_CATEGORY("Sale per Category"),
    SALES_TREND("Trend Sale"),
    INCOME_STATEMENT("Laporan Laba Rugi"),
    CASH_FLOW("Arus Kas"),
    EXPENSE_REPORT("Laporan Expense"),
    BALANCE_SHEET("Laporan Neraca")
}