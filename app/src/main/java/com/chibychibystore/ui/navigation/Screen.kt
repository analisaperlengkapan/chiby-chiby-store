package com.chibychibystore.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Inventory : Screen("inventory")
    object ProductAdd : Screen("inventory/add")
    object ProductDetail : Screen("inventory/product/{productId}") {
        fun createRoute(productId: String) = "inventory/product/$productId"
    }
    object Warehouse : Screen("inventory/warehouse")
    object WarehouseList : Screen("inventory/warehouses")
    object WarehouseAdd : Screen("inventory/warehouse/add")
    object WarehouseEdit : Screen("inventory/warehouse/{warehouseId}/edit") {
        fun createRoute(warehouseId: String) = "inventory/warehouse/$warehouseId/edit"
    }
    object WarehouseDetail : Screen("inventory/warehouse/{warehouseId}") {
        fun createRoute(warehouseId: String) = "inventory/warehouse/$warehouseId"
    }
    object Pos : Screen("pos")
    object Sales : Screen("sales")
    object SalesHistory : Screen("sales/history")
    object PurchaseList : Screen("purchases")
    object PurchaseAdd : Screen("purchases/add")
    object PromotionList : Screen("promotions")
    object PromotionAdd : Screen("promotions/add")
    object PromotionEdit : Screen("promotions/{promotionId}/edit") {
        fun createRoute(promotionId: Long) = "promotions/$promotionId/edit"
    }
    object CashShift : Screen("cash_shift")
    object CashHistory : Screen("cash_history")
    object AuditList : Screen("audit_list")
    object AuditAdd : Screen("audit_add")
    object PelangganList : Screen("pelanggan")
    object PelangganAdd : Screen("pelanggan/add")
    object PelangganEdit : Screen("pelanggan/{pelangganId}/edit") {
        fun createRoute(pelangganId: Long) = "pelanggan/$pelangganId/edit"
    }
    object BarcodeScanner : Screen("barcode/scan")
    object BarcodePrint : Screen("barcode/print")
    object Reports : Screen("reports")
    object ExpenseList : Screen("expense/list")
    object ExpenseAdd : Screen("expense/add")
    object ExpenseDetail : Screen("expense/detail/{expenseId}") {
        fun createRoute(expenseId: Long) = "expense/detail/$expenseId"
    }
    object Backup : Screen("backup")
    object UserList : Screen("users")
    object UserAdd : Screen("users/add")
    object UserDetail : Screen("users/{userId}") {
        fun createRoute(userId: Long) = "users/$userId"
    }
    object Settings : Screen("settings")
    object SupplierList : Screen("suppliers")
}
