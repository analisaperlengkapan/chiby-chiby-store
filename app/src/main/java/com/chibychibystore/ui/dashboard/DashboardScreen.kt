package com.chibychibystore.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.ui.components.ChibyCard
import com.chibychibystore.ui.components.ChibyScaffold
import com.chibychibystore.ui.components.shared.AppDrawer
import com.chibychibystore.ui.components.shared.BottomNavItem
import com.chibychibystore.ui.components.shared.BottomNavBar
import com.chibychibystore.ui.components.shared.LoadingIndicator
import com.chibychibystore.ui.components.special.ChartData
import com.chibychibystore.ui.components.special.ChartView
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.theme.ChibyPinkPrimary
import com.chibychibystore.ui.theme.ChibyPinkLight
import com.chibychibystore.ui.theme.ChibyYellowSecondary
import com.chibychibystore.ui.theme.Error
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    drawerState: DrawerState,
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    val formatCurrency = rememberCurrencyFormatter()
    val formatDate = rememberDateFormatter()
    
    val bottomNavItems = listOf(
        BottomNavItem("Dashboard", Icons.Default.Dashboard, Screen.Dashboard.route),
        BottomNavItem("Inventory", Icons.Default.Inventory, Screen.Inventory.route),
        BottomNavItem("Sales", Icons.Default.PointOfSale, Screen.SalesHistory.route),
        BottomNavItem("Reports", Icons.Default.Analytics, Screen.Reports.route),
        BottomNavItem("Settings", Icons.Default.Settings, Screen.Settings.route)
    )

    AppDrawer(
        drawerState = drawerState,
        currentRoute = currentRoute,
        onNavigateToRoute = onNavigateToRoute,
        onCloseDrawer = { scope.launch { drawerState.close() } }
    ) {
        ChibyScaffold(
            title = "Dashboard",
            bottomBar = {
                BottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = onNavigateToRoute
                )
            },
            onNavigateUp = null // Drawer handles navigation
        ) { paddingValues ->
            if (uiState.isLoading) {
                LoadingIndicator(
                    message = "Memuat data dashboard...",
                    modifier = Modifier.padding(paddingValues)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Section: Today's Sales
                    item {
                        ChibyCard(
                            containerColor = ChibyPinkLight,
                            elevation = 0,
                            onClick = { onNavigateToRoute(Screen.SalesHistory.route) }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Total Penjualan Hari Ini",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ChibyPinkPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formatCurrency(uiState.todaySales),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = ChibyPinkPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .background(ChibyPinkPrimary.copy(alpha = 0.1f), MaterialTheme.shapes.small)
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = ChibyPinkPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = " ${uiState.todayTransactionCount} Transaksi",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = ChibyPinkPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Alerts Section
                    if (uiState.lowStockItems.isNotEmpty()) {
                        item {
                            ChibyCard(
                                containerColor = Error.copy(alpha = 0.1f),
                                elevation = 0,
                                onClick = { onNavigateToRoute(Screen.Inventory.route) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Error,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.size(16.dp))
                                    Column {
                                        Text(
                                            text = "Stok Menipis",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Error
                                        )
                                        Text(
                                            text = "${uiState.lowStockItems.size} produk perlu restock segera.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Chart Section
                    item {
                        ChibyCard {
                            Text(
                                text = "Trend Penjualan (7 Hari)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            // Placeholder data for chart (should rely on real data if available in VM)
                            ChartView(
                                title = "",
                                data = listOf(
                                    ChartData("Sen", 120000f, ChibyPinkPrimary),
                                    ChartData("Sel", 150000f, ChibyPinkPrimary),
                                    ChartData("Rab", 180000f, ChibyPinkPrimary),
                                    ChartData("Kam", 140000f, ChibyPinkPrimary),
                                    ChartData("Jum", 200000f, ChibyPinkPrimary),
                                    ChartData("Sab", 220000f, ChibyPinkPrimary),
                                    ChartData("Min", uiState.todaySales.toFloat(), ChibyPinkPrimary)
                                )
                            )
                        }
                    }

                    // Recent Transactions List
                    if (uiState.recentTransactions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Transaksi Terbaru",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(uiState.recentTransactions.take(5)) { transaction ->
                            val paymentIcon = if (transaction.paymentMethod.name == "CASH") 
                                Icons.Default.PointOfSale else Icons.Default.Analytics
                                
                            ChibyCard(
                                modifier = Modifier.padding(bottom = 8.dp),
                                onClick = { onNavigateToRoute(Screen.SalesHistory.route) },
                                elevation = 1
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = paymentIcon,
                                            contentDescription = null,
                                            tint = ChibyPinkPrimary,
                                            modifier = Modifier.padding(end = 16.dp)
                                        )
                                        Column {
                                            Text(
                                                text = formatCurrency(transaction.totalAmount),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = formatDate(transaction.saleDate.time),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                ChibyYellowSecondary.copy(alpha = 0.2f),
                                                MaterialTheme.shapes.extraSmall
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = transaction.paymentMethod.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ChibyYellowSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberCurrencyFormatter(): (Double) -> String {
    val locale = remember { Locale.Builder().setLanguage("id").setRegion("ID").build() }
    val formatter = remember { NumberFormat.getCurrencyInstance(locale) }
    return { amount -> formatter.format(amount) }
}

@Composable
fun rememberDateFormatter(): (Long) -> String {
    val locale = remember { Locale.getDefault() }
    val formatter = remember { java.text.SimpleDateFormat("dd MMM, HH:mm", locale) }
    return { timestamp -> formatter.format(Date(timestamp)) }
}