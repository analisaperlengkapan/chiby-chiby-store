package com.chibychibystore.ui.components.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chibychibystore.R
import com.chibychibystore.ui.navigation.Screen

data class DrawerNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

/**
 * Single source of truth for the drawer menu. Kept public so the navigation
 * tests can assert that every listed destination is registered in the nav graph
 * and that every intended top-level screen has an entry here.
 *
 * The drawer is the only production entry point to the modules after login, so
 * a screen missing from this list is unreachable (the logout action, for
 * instance, lives only on [Screen.Settings]).
 */
val drawerNavItems: List<DrawerNavItem> = listOf(
    DrawerNavItem("Dashboard", Icons.Default.Dashboard, Screen.Dashboard.route),
    DrawerNavItem("Point of Sale", Icons.Default.PointOfSale, Screen.Pos.route),
    DrawerNavItem("Inventory", Icons.Default.Inventory, Screen.Inventory.route),
    DrawerNavItem("Riwayat Penjualan", Icons.Default.Receipt, Screen.SalesHistory.route),
    DrawerNavItem("Manajemen Pembelian", Icons.Default.ShoppingBag, Screen.PurchaseList.route),
    DrawerNavItem("Manajemen Warehouse", Icons.Default.Warehouse, Screen.WarehouseList.route),
    DrawerNavItem("Manajemen Expense", Icons.Default.AccountBalanceWallet, Screen.ExpenseList.route),
    DrawerNavItem("Manajemen User", Icons.Default.Group, Screen.UserList.route),
    DrawerNavItem("Manajemen Promosi", Icons.Default.LocalOffer, Screen.PromotionList.route),
    DrawerNavItem("Manajemen Pelanggan", Icons.Default.Person, Screen.PelangganList.route),
    DrawerNavItem("Manajemen Kas", Icons.Default.PointOfSale, Screen.CashShift.route),
    DrawerNavItem("Riwayat Shift", Icons.Default.History, Screen.CashHistory.route),
    DrawerNavItem("Stok Opname", Icons.Default.FactCheck, Screen.AuditList.route),
    DrawerNavItem("Daftar Pemasok", Icons.Default.LocalShipping, Screen.SupplierList.route),
    DrawerNavItem("Laporan", Icons.Default.Assessment, Screen.Reports.route),
    DrawerNavItem("Barcode Scanner", Icons.Default.QrCodeScanner, Screen.BarcodeScanner.route),
    DrawerNavItem("Cetak Label Barcode", Icons.Default.Print, Screen.BarcodePrint.route),
    DrawerNavItem("Backup & Restore", Icons.Default.Backup, Screen.Backup.route),
    DrawerNavItem("Pengaturan", Icons.Default.Settings, Screen.Settings.route)
)

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerItems = drawerNavItems

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.85f),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerTonalElevation = 0.dp
            ) {
                // Header Branding
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(vertical = 32.dp, horizontal = 24.dp)
                ) {
                    Column {
                        Image(
                            painter = painterResource(R.drawable.chiby_logo),
                            contentDescription = "Logo Chiby Chiby Store",
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    shape = CircleShape
                                )
                                .padding(6.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chiby Chiby Store",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Premium POS System",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The item list scrolls: with this many modules it is taller than
                // a phone screen, and a fixed column would clip the last entries.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "FITUR LANJUTAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        letterSpacing = 1.2.sp
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    drawerItems.forEach { item ->
                        val isSelected = currentRoute == item.route

                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                onNavigateToRoute(item.route)
                                onCloseDrawer()
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
        content = content
    )
}