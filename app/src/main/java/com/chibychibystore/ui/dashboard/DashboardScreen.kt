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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Penjualan
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.service.DataTren
import com.chibychibystore.ui.components.ChibyCard
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.LoadingIndicator
import com.chibychibystore.ui.components.special.ChartData
import com.chibychibystore.ui.components.special.ChartView
import com.chibychibystore.ui.navigation.Screen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    drawerState: androidx.compose.material3.DrawerState,
    currentRoute: String,
    onNavigateToRoute: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Dashboard",
                navigationIcon = Icons.Default.Menu,
                onNavigationClick = { scope.launch { drawerState.open() } },
                actions = {}
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingIndicator()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // KPI Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            KpiCard(
                                title = "Penjualan Hari Ini",
                                value = formatCurrency(uiState.todaySales),
                                icon = Icons.Default.Analytics,
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "Transaksi",
                                value = uiState.todayTransactionCount.toString(),
                                icon = Icons.Default.Dashboard, // Using Dashboard/Receipt icon substitute
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Low Stock Alerts
                    if (uiState.lowStockItems.isNotEmpty()) {
                        item {
                            LowStockCard(
                                items = uiState.lowStockItems,
                                onItemClick = { produk ->
                                    navController.navigate(Screen.ProductDetail.createRoute(produk.id.toString()))
                                }
                            )
                        }
                    }

                    // Sales Trend
                    item {
                        val chartData = remember(uiState.salesTrend) {
                            uiState.salesTrend.map { trend ->
                                ChartData(
                                    label = trend.tanggal.dayOfWeek.name.take(3),
                                    value = trend.penjualan.toFloat(),
                                    date = trend.tanggal
                                )
                            }
                        }
                        
                        ChibyCard {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                               Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                               Text("Tren Penjualan (7 Hari)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            ChartView(
                                data = chartData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    }

                    // Recent Transactions
                    item {
                        RecentTransactionsCard(
                            transactions = uiState.recentTransactions,
                            onItemClick = { penjualan ->
                                // Navigate to Sales Detail if available, or just list
                                // for now we can navigate to history or do nothing/show toast
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    ChibyCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LowStockCard(
    items: List<Produk>,
    onItemClick: (Produk) -> Unit
) {
    ChibyCard(
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
               Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
               Text("Stok Menipis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            items.take(5).forEach { produk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = produk.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Stok: ${produk.stockQuantity} (Min: ${produk.minStock})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentTransactionsCard(
    transactions: List<Penjualan>,
    onItemClick: (Penjualan) -> Unit
) {
    ChibyCard {
        val dateFormatter = remember { SimpleDateFormat("dd/MM HH:mm", Locale("id", "ID")) }
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
               Icon(Icons.Default.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
               Text("Transaksi Terakhir", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (transactions.isEmpty()) {
                Text(
                    text = "Belum ada transaksi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                transactions.take(5).forEach { penjualan ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "#${penjualan.id}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = dateFormatter.format(penjualan.saleDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatCurrency(penjualan.totalAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = penjualan.paymentMethod.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(amount)
}