package com.chibychibystore.ui.inventory
import com.chibychibystore.data.local.entity.Produk

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.CardItem
import com.chibychibystore.ui.components.shared.ErrorMessage
import com.chibychibystore.ui.components.shared.LoadingIndicator
import com.chibychibystore.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseListScreen(
    navController: NavController,
    viewModel: WarehouseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manajemen Gudang",
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.WarehouseAdd.route) }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Gudang")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.warehouses.isEmpty() -> {
                    LoadingIndicator()
                }
                uiState.error != null -> {
                    ErrorMessage(
                        message = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }
                uiState.warehouses.isEmpty() -> {
                    WarehouseEmptyState(onAddWarehouse = {
                        navController.navigate(Screen.Warehouse.route)
                    })
                }
                else -> {
                    WarehouseListContent(
                        warehouses = uiState.warehouses,
                        allWarehouseStock = uiState.allWarehouseStock,
                        onWarehouseClick = { warehouse ->
                            navController.navigate(Screen.WarehouseDetail.createRoute(warehouse.id.toString()))
                        },
                        onRefresh = { viewModel.refresh() }
                    )
                }
            }

            // Success message snackbar
            uiState.successMessage?.let { message ->
                LaunchedEffect(message) {
                    // Show snackbar and auto-clear after delay
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearSuccessMessage()
                }
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(message)
                }
            }
        }
    }
}

@Composable
private fun WarehouseListContent(
    warehouses: List<Gudang>,
    allWarehouseStock: Map<Gudang, List<com.chibychibystore.data.local.entity.Produk>>,
    onWarehouseClick: (Gudang) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Summary header
        WarehouseSummaryHeader(
            totalWarehouses = warehouses.size,
            totalProducts = allWarehouseStock.values.sumOf { it.size }
        )

        // Warehouse list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(warehouses) { warehouse ->
                WarehouseListItem(
                    warehouse = warehouse,
                    products = allWarehouseStock[warehouse] ?: emptyList(),
                    onClick = { onWarehouseClick(warehouse) }
                )
            }
        }
    }
}

@Composable
private fun WarehouseSummaryHeader(
    totalWarehouses: Int,
    totalProducts: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = totalWarehouses.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Gudang",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            VerticalDivider()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = totalProducts.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Produk",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun WarehouseListItem(
    warehouse: Gudang,
    products: List<com.chibychibystore.data.local.entity.Produk>,
    onClick: () -> Unit
) {
    CardItem(
        title = warehouse.name,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    if (!warehouse.location.isNullOrBlank()) {
                        Text(
                            text = warehouse.location!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${products.size} produk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val totalStock = products.sumOf { it.stockQuantity }
                    Text(
                        text = "Total stok: $totalStock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (products.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))

                // Show top 3 products
                val topProducts = products.take(3)
                Text(
                    text = "Produk: ${topProducts.joinToString(", ") { it.name }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (products.size > 3) {
                    Text(
                        text = "+${products.size - 3} produk lainnya",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseEmptyState(
    onAddWarehouse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warehouse,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Belum ada gudang",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tambahkan gudang pertama untuk mulai mengelola inventori",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddWarehouse,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tambah Gudang")
        }
    }
}

@Composable
private fun VerticalDivider() {
    Divider(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}