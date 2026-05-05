package com.chibychibystore.util

/**
 * Constants for permission strings used in Role-Based Access Control (RBAC).
 */
object Permissions {
    // Inventory
    const val VIEW_INVENTORY = "VIEW_INVENTORY"
    const val EDIT_INVENTORY = "EDIT_INVENTORY"
    const val MANAGE_WAREHOUSES = "MANAGE_WAREHOUSES"
    const val PRINT_BARCODE_LABELS = "PRINT_BARCODE_LABELS"
    const val VIEW_INVENTORY_REPORTS = "VIEW_INVENTORY_REPORTS"

    // Sales
    const val CREATE_SALES = "CREATE_SALES"
    const val VIEW_DAILY_SALES_REPORT = "VIEW_DAILY_SALES_REPORT"
    const val VIEW_SALES_REPORTS = "VIEW_SALES_REPORTS"
    const val APPROVE_LARGE_TRANSACTIONS = "APPROVE_LARGE_TRANSACTIONS"

    // Financial
    const val VIEW_FINANCIAL_REPORTS = "VIEW_FINANCIAL_REPORTS"

    // User Management
    const val MANAGE_USERS = "MANAGE_USERS"
}
