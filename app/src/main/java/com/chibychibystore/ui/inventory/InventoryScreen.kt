package com.chibychibystore.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Product
import com.chibychibystore.ui.components.ChibyButton
import com.chibychibystore.ui.components.ChibyCard
import com.chibychibystore.ui.components.ChibyInput
import com.chibychibystore.ui.components.ChibyScaffold
import com.chibychibystore.ui.components.shared.LoadingIndicator
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.theme.ChibyPinkPrimary
import com.chibychibystore.ui.theme.ChibyYellowSecondary
import com.chibychibystore.ui.theme.Error
import com.chibychibystore.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavController,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ChibyScaffold(
        title = "Inventory",
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.ProductAdd.route) },
                containerColor = ChibyPinkPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        onNavigateUp = { navController.navigateUp() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Search
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                ChibyInput(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::updateSearchQuery,
                    label = "Cari Produk...",
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = ChibyPinkPrimary)
                    }
                )
            }

            // Low Stock Alert
            if (uiState.lowStockProducts.isNotEmpty()) {
                ChibyCard(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    containerColor = Error.copy(alpha = 0.1f),
                    elevation = 0
                ) {
                    Text(
                        text = "⚠️ ${uiState.lowStockProducts.size} produk stok rendah",
                        style = MaterialTheme.typography.labelLarge,
                        color = Error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Content
            when {
                uiState.isLoading -> LoadingIndicator("Memuat inventory...")
                uiState.error != null -> {
                    ChibyCard(
                        modifier = Modifier.padding(16.dp),
                        containerColor = Error.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            color = Error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                uiState.products.isEmpty() -> {
                    EmptyInventoryState(
                        onAddProduct = { navController.navigate(Screen.ProductAdd.route) }
                    )
                }
                else -> {
                    ProductList(
                        products = uiState.products,
                        onProductClick = { product ->
                            navController.navigate(Screen.ProductDetail.createRoute(product.id.toString()))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInventoryState(onAddProduct: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Inventory,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Belum ada produk",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tambahkan produk pertama Anda untuk memulai",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        ChibyButton(
            text = "Tambah Produk",
            onClick = onAddProduct,
            modifier = Modifier.width(200.dp)
        )
    }
}

@Composable
private fun ProductList(
    products: List<Product>,
    onProductClick: (Product) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items = products, key = { it.id }) { product ->
            ProductListItem(product = product, onClick = { onProductClick(product) })
        }
    }
}

@Composable
private fun ProductListItem(
    product: Product,
    onClick: () -> Unit
) {
    ChibyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = 2
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (product.barcode != null) {
                    Text(
                        text = product.barcode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val isLowStock = product.stockQuantity <= product.minStock
            val statusColor = if (isLowStock) Error else Success
            val statusText = if (isLowStock) "Low Stock" else "In Stock"

            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Harga Jual",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rp ${product.sellingPrice}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ChibyPinkPrimary
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Stok",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${product.stockQuantity} Unit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}