package com.chibychibystore.ui.reports

/**
 * Enum untuk tipe laporan yang tersedia
 */
enum class ReportType(val displayName: String) {
    GROSS_SALES("Penjualan Kotor"),
    PROFIT_MARGIN("Margin Keuntungan"),
    NET_PROFIT("Keuntungan Bersih"),
    SALES_BY_PRODUCT("Penjualan per Produk"),
    SALES_BY_CATEGORY("Penjualan per Kategori"),
    SALES_TREND("Trend Penjualan"),
    INCOME_STATEMENT("Laporan Laba Rugi"),
    CASH_FLOW("Arus Kas"),
    EXPENSE_REPORT("Laporan Pengeluaran"),
    BALANCE_SHEET("Laporan Neraca")
}