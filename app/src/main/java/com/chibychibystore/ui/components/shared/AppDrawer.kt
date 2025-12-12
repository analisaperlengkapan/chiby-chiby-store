package com.chibychibystore.ui.components.shared

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chibychibystore.ui.navigation.Screen

data class DrawerNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerItems = listOf(
        DrawerNavItem("Point of Sale", Icons.Default.PointOfSale, Screen.Pos.route),
        DrawerNavItem("Manajemen Gudang", Icons.Default.Warehouse, Screen.WarehouseList.route),
        DrawerNavItem("Manajemen Pengeluaran", Icons.Default.AccountBalanceWallet, Screen.ExpenseList.route),
        DrawerNavItem("Manajemen Pengguna", Icons.Default.Group, Screen.UserList.route),
        DrawerNavItem("Barcode Scanner", Icons.Default.QrCodeScanner, Screen.BarcodeScanner.route),
        DrawerNavItem("Cetak Label Barcode", Icons.Default.Print, Screen.BarcodePrint.route),
        DrawerNavItem("Backup & Restore", Icons.Default.Backup, "backup")
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Fitur Lanjutan",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp)
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
            }
        },
        content = content
    )
}