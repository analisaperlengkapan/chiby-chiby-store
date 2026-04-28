package com.chibychibystore.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        DrawerNavItem("Manajemen Pembelian", Icons.Default.ShoppingBag, Screen.PurchaseList.route),
        DrawerNavItem("Manajemen Warehouse", Icons.Default.Warehouse, Screen.WarehouseList.route),
        DrawerNavItem("Manajemen Expense", Icons.Default.AccountBalanceWallet, Screen.ExpenseList.route),
        DrawerNavItem("Manajemen User", Icons.Default.Group, Screen.UserList.route),
        DrawerNavItem("Barcode Scanner", Icons.Default.QrCodeScanner, Screen.BarcodeScanner.route),
        DrawerNavItem("Cetak Label Barcode", Icons.Default.Print, Screen.BarcodePrint.route),
        DrawerNavItem("Backup & Restore", Icons.Default.Backup, "backup")
    )

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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "C", 
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
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
            }
        },
        content = content
    )
}