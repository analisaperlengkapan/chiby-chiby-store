package com.chibychibystore.ui.inventory

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
import com.chibychibystore.ui.components.AppTopBar
import com.chibychibystore.ui.components.CardItem
import com.chibychibystore.ui.components.ErrorMessage
import com.chibychibystore.ui.components.LoadingIndicator

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
        val warehouse = viewModel.getWarehouseById(warehouseId)
        if (warehouse != null) {
            viewModel.selectWarehouse(warehouse)
        }
    }

    val selectedWarehouse = uiState.selectedWarehouse

    Scaffold(
        topBar = {
            AppTopBar(
                title = selectedWarehouse?.nama ?: "Detail Gudang",
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
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
                        navController.navigate(Screen.WarehouseEdit.createRoute(selectedWarehouse.id))
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
                    ErrorMessage(
                        message = uiState.error!!,
                        onRetry = {
                            selectedWarehouse?.let { viewModel.selectWarehouse(it) }
                        }
                    )
                }
                else -> {
                    WarehouseDetailContent(
                        warehouse = selectedWarehouse,
                        products = uiState.products,
                        onProductClick = { product ->
                            // TODO: Navigate to product detail or show product actions
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
    if (showTransferDialog && selectedProductForTransfer != null && selectedWarehouse != null) {
        StockTransferDialog(
            product = selectedProductForTransfer!!,
            fromWarehouse = selectedWarehouse!!,
            availableWarehouses = uiState.warehouses,
            onTransfer = { toWarehouseId, quantity ->
                viewModel.transferStock(
                    productId = selectedProductForTransfer!!.id,
                    fromWarehouseId = selectedWarehouse!!.id,
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
        WarehouseInfoHeader(warehouse = warehouse, productCount = products.size)

        // Products list
        if (products.isEmpty()) {
            WarehouseEmptyProductsState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products) { product ->
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
    productCount: Int
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
                text = warehouse.nama,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            warehouse.lokasi?.let { location ->
                if (location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
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

                val totalStock = products.sumOf { it.stok }
                Text(
                    text = "Total stok: $totalStock",
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
    CardItem(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                    text = product.nama,
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
                        text = "Stok: ${product.stok}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (product.stok <= product.minStok) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (product.stok <= product.minStok) {
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
                    text = "Rp ${product.hargaJual}",
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
            Icon(Icons.Default.ArrowBack, contentDescription = null)
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