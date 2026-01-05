@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.barcode

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.R
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.ui.components.shared.AppTopBar

/**
 * Label size options for barcode printing
 */
enum class LabelSize(val displayName: String, val width: Int, val height: Int) {
    SMALL("Kecil (2x1 cm)", 2, 1),
    MEDIUM("Sedang (3x2 cm)", 3, 2),
    LARGE("Besar (3x2 cm)", 3, 2),
    EXTRA_LARGE("Extra Besar (7x4 cm)", 7, 4)
}

/**
 * Barcode Print Screen
 * Mengelola pencetakan label barcode untuk product
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodePrintScreen(
    onNavigateBack: () -> Unit,
    viewModel: BarcodePrintViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.barcode_print_title),
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Produk Selection
            ProductSelectionSection(
                products = uiState.products,
                selectedProduct = uiState.selectedProduct,
                searchQuery = uiState.searchQuery,
                isLoading = uiState.isLoadingProducts,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onProductSelected = { viewModel.selectProduct(it) }
            )

            // Label Configuration
            if (uiState.selectedProduct != null) {
                LabelConfigurationSection(
                    selectedSize = uiState.selectedSize,
                    quantity = uiState.quantity,
                    isPrinting = uiState.isPrinting,
                    onSizeSelected = { viewModel.selectLabelSize(it) },
                    onQuantityChanged = { viewModel.updateQuantity(it) },
                    onPrintLabels = { viewModel.printLabels() }
                )
            }

            // Label Preview
            uiState.selectedProduct?.let { selectedProduct ->
                LabelPreviewSection(
                    product = selectedProduct,
                    labelSize = uiState.selectedSize,
                    quantity = uiState.quantity
                )
            }
        }
    }

    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or dialog for error
            viewModel.clearError()
        }
    }
}

@Composable
private fun ProductSelectionSection(
    products: List<Produk>,
    selectedProduct: Produk?,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onProductSelected: (Produk) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.barcode_select_product),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text(stringResource(R.string.barcode_search_product)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Selected Produk Display
            selectedProduct?.let { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Text(
                                text = "Barcode: ${product.barcode ?: "Tidak ada"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onProductSelected(product) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Ubah product")
                        }
                    }
                }
            }

            // Produk List
            if (products.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(products) { product ->
                        ProductListItem(
                            product = product,
                            isSelected = product.id == selectedProduct?.id,
                            onClick = { onProductSelected(product) }
                        )
                    }
                }
            } else if (!isLoading && searchQuery.isNotBlank()) {
                Text(
                    text = stringResource(R.string.barcode_no_products_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ProductListItem(
    product: Produk,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                Text(
                    text = "Stok: ${product.stockQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Terpilih",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LabelConfigurationSection(
    selectedSize: LabelSize,
    quantity: Int,
    isPrinting: Boolean,
    onSizeSelected: (LabelSize) -> Unit,
    onQuantityChanged: (Int) -> Unit,
    onPrintLabels: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.barcode_label_settings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Label Size Selection
            Text(
                text = stringResource(R.string.barcode_label_size),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LabelSize.entries.forEach { size ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSizeSelected(size) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = size == selectedSize,
                        onClick = { onSizeSelected(size) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = size.displayName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.barcode_quantity),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (quantity > 1) onQuantityChanged(quantity - 1) }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurangi")
                    }

                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    IconButton(
                        onClick = { if (quantity < 99) onQuantityChanged(quantity + 1) }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Print Button
            Button(
                onClick = onPrintLabels,
                enabled = !isPrinting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isPrinting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isPrinting)
                        stringResource(R.string.barcode_printing_labels)
                    else
                        stringResource(R.string.barcode_print_labels, quantity)
                )
            }
        }
    }
}

@Composable
private fun LabelPreviewSection(
    product: Produk,
    labelSize: LabelSize,
    quantity: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.barcode_label_preview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Produk Name
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Barcode Placeholder (would show actual barcode image)
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = product.barcode ?: "BARCODE",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.Black,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Price
                    Text(
                        text = "Rp ${product.sellingPrice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = androidx.compose.ui.graphics.Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.barcode_preview_note, labelSize.displayName, quantity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}