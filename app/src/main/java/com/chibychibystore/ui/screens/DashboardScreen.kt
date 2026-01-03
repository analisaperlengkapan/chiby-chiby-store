package com.chibychibystore.ui.screens
import com.chibychibystore.ui.components.shared.AppTopBar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.chibychibystore.ui.components.shared.*
import com.chibychibystore.ui.reports.BarChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.ui.components.shared.AppDrawer
import com.chibychibystore.ui.components.shared.BottomNavBar
import com.chibychibystore.ui.components.shared.CardItem

import com.chibychibystore.ui.components.shared.LoadingIndicator
import androidx.compose.material.icons.filled.Warning
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
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
    
    // Optimized formatters
    val formatCurrency = rememberCurrencyFormatter()
    val formatDate = rememberDateFormatter()
    val formatDayName = rememberDayNameFormatter()

    // Theme references to avoid repeated Composable calls in LazyColumn items
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

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
        Scaffold(
            topBar = {
                AppTopBar(
                    title = "Chiby Chiby Store",
                    navigationIcon = Icons.Default.Menu,
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    actions = {
                        IconButton(onClick = { viewModel.refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = onNavigateToRoute
                )
            }
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
                    // Sales Summary Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ),
                            shape = shapes.extraLarge,
                            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Penjualan Hari Ini",
                                    style = typography.titleMedium,
                                    color = colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formatCurrency(uiState.todaySales),
                                    style = typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = colorScheme.primary.copy(alpha = 0.1f),
                                            shape = shapes.small
                                        )
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${uiState.todayTransactionCount} transaksi",
                                        style = typography.labelLarge,
                                        color = colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Low Stock Alert
                    if (uiState.lowStockItems.isNotEmpty()) {
                        item {
                            CardItem(
                                title = "Peringatan Stok Rendah",
                                subtitle = "${uiState.lowStockItems.size} produk stok rendah",
                                icon = Icons.Default.Warning,
                                onClick = { onNavigateToRoute(Screen.Inventory.route) }
                            )
                        }
                    }

                    // Recent Transactions
                    if (uiState.recentTransactions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Transaksi Terbaru",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(uiState.recentTransactions) { transaction ->
                            CardItem(
                                title = formatCurrency(transaction.totalAmount),
                                subtitle = "Tanggal: ${formatDate(transaction.saleDate.time)}",
                                onClick = { onNavigateToRoute(Screen.SalesHistory.route) }
                            )
                        }
                    }

                    // Sales Chart
                    if (uiState.salesTrend.isNotEmpty()) {
                        item {
                            val chartData = remember(uiState.salesTrend) {
                                uiState.salesTrend.map {
                                    formatDayName(it.date) to it.sales.toFloat()
                                }
                            }
                            BarChart(
                                title = "Trend Penjualan 7 Hari Terakhir",
                                data = chartData
                            )
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
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", locale) }
    return { timestamp -> formatter.format(Date(timestamp)) }
}

@Composable
fun rememberDayNameFormatter(): (java.time.LocalDate) -> String {
    val locale = remember { Locale("id", "ID") }
    val formatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEE", locale) }
    return { date -> date.format(formatter) }
}