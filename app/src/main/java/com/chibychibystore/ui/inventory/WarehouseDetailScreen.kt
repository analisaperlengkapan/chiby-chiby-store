@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.inventory
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.ErrorMessage
import com.chibychibystore.ui.components.shared.LoadingIndicator

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
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.ui.components.shared.CardItem
import com.chibychibystore.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseDetailScreen(
    navController: NavController,
    warehouseId: String,
    viewModel: WarehouseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialog state
    var showTransferDialog by remember { mutableStateOf(false) }
    var selectedProductForTransfer by remember { mutableStateOf<Produk?>(null) }

    // Load warehouse data when screen opens
    LaunchedEffect(warehouseId) {
        val warehouseIdLong = warehouseId.toLongOrNull() ?: return@LaunchedEffect
        val warehouse = viewModel.getWarehouseById(warehouseIdLong)
        if (warehouse != null) {
            viewModel.selectWarehouse(warehouse)
        }
    }

    val selectedWarehouse = uiState.selectedWarehouse

    Scaffold(
        topBar = {
            AppTopBar(
                title = selectedWarehouse?.name ?: "Detail Gudang",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = { navController.navigateUp() },
                actions = {
                    // Transfer stock button
                    IconButton(
                        onClick = {
                            showTransferDialog = true
                        },
                        enabled = selectedWarehouse != null && uiState.products.isNotEmpty()
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer Stok")
                    }

                    // Edit warehouse button
                    IconButton(onClick = {
                        val warehouse = selectedWarehouse ?: return@IconButton
                        navController.navigate(Screen.WarehouseEdit.createRoute(warehouse.id.toString()))
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Gudang")
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
                selectedWarehouse == null -> {
                    WarehouseNotFoundState(onBack = { navController.navigateUp() })
                }
                uiState.isLoading -> {
                    LoadingIndicator()
                }
                uiState.error != null -> {
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer,
                            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                        ),
                        shape = androidx.compose.material3.MaterialTheme.shapes.medium
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            androidx.compose.material3.Text(text = uiState.error ?: "")
                        }
                    }
                }
                else -> {
                    WarehouseDetailContent(
                        warehouse = selectedWarehouse,
                        products = uiState.products,
                        onProductClick = { product ->
                            navController.navigate(Screen.ProductDetail.createRoute(product.id.toString()))
                        },
                        onTransferStock = { product ->
                            selectedProductForTransfer = product
                            showTransferDialog = true
                        }
                    )
                }
            }

            // Success message snackbar
            uiState.successMessage?.let { message ->
                LaunchedEffect(message) {
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

    // Transfer dialog
    selectedProductForTransfer?.let { product ->
        selectedWarehouse?.let { warehouse ->
            if (showTransferDialog) {
                StockTransferDialog(
                    product = product,
                    fromWarehouse = warehouse,
                    availableWarehouses = uiState.warehouses,
                    onTransfer = { toWarehouseId, quantity ->
                        viewModel.transferStock(
                            productId = product.id,
                            fromWarehouseId = warehouse.id,
                            toWarehouseId = toWarehouseId,
                            quantity = quantity
                        )
                    },
                    onDismiss = {
                        showTransferDialog = false
                        selectedProductForTransfer = null
                    }
                )
            }
        }
    }
}

@Composable
private fun WarehouseDetailContent(
    warehouse: Gudang,
    products: List<Produk>,
    onProductClick: (Produk) -> Unit,
    onTransferStock: (Produk) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Warehouse info header
        WarehouseInfoHeader(
            warehouse = warehouse,
            productCount = products.size,
            totalStock = products.sumOf { it.stockQuantity }
        )

        // Products list
        if (products.isEmpty()) {
            WarehouseEmptyProductsState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = products,
                    key = { it.id }
                ) { product ->
                    WarehouseProductItem(
                        product = product,
                        onClick = { onProductClick(product) },
                        onTransferClick = { onTransferStock(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseInfoHeader(
    warehouse: Gudang,
    productCount: Int,
    totalStock: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = warehouse.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            warehouse.location?.takeIf { it.isNotBlank() }?.let { location ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$productCount produk",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = "Total stockQuantity: $totalStock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun WarehouseProductItem(
    product: Produk,
    onClick: () -> Unit,
    onTransferClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                product.barcode?.let { barcode ->
                    Text(
                        text = "Barcode: $barcode",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stok: ${product.stockQuantity}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (product.stockQuantity <= product.minStock) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (product.stockQuantity <= product.minStock) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Stok rendah",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrency(product.sellingPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                IconButton(
                    onClick = onTransferClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Transfer stok",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseNotFoundState(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Gudang tidak ditemukan",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Gudang yang Anda cari tidak tersedia atau telah dihapus",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Kembali")
        }
    }
}

@Composable
private fun WarehouseEmptyProductsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inventory,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Belum ada produk",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Gudang ini belum memiliki produk. Tambahkan produk dari menu Inventory.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun formatCurrency(amount: Double): String {
    val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
    return format.format(amount)
}