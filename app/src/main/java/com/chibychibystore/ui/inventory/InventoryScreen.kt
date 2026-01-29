package com.chibychibystore.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.R
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.ui.components.ChibyButton
import com.chibychibystore.ui.components.ChibyCard
import com.chibychibystore.ui.components.ChibyInput
import com.chibychibystore.ui.components.ChibyScaffold
import com.chibychibystore.ui.components.shared.LoadingIndicator
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.theme.ChibyPinkPrimary
import com.chibychibystore.ui.theme.Error
import com.chibychibystore.ui.theme.Success
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavController,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen for scanned barcode result
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val scannedBarcode = savedStateHandle?.getLiveData<String>("scanned_barcode")?.observeAsState()?.value

    LaunchedEffect(scannedBarcode) {
        scannedBarcode?.let { barcode ->
            if (barcode.isNotBlank()) {
                viewModel.updateSearchQuery(barcode)
                // Clear the result to avoid re-triggering
                savedStateHandle?.remove<String>("scanned_barcode")
            }
        }
    }

    // Show Snackbar on Error
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }
    
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = ChibyPinkPrimary)
                    },
                    trailingIcon = {
                        IconButton(onClick = { navController.navigate(Screen.BarcodeScanner.route) }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = ChibyPinkPrimary)
                        }
                    }
                )
            }

            // Product List
            if (uiState.products.isEmpty() && uiState.searchQuery.isEmpty()) {
                // Initial loading or truly empty
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator("Memuat produk...")
                }
            } else if (uiState.products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Produk tidak ditemukan",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.products, key = { it.id }) { product ->
                        ProductListItem(
                            product = product,
                            onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id.toString())) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductListItem(
    product: Produk,
    onClick: () -> Unit
) {
    val formatCurrency = rememberCurrencyFormatter()
    
    ChibyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = 2
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Barcode: ${product.barcode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Stok: ${product.stockQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stockQuantity > 5) Success else Error,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = formatCurrency(product.sellingPrice),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ChibyPinkPrimary
            )
        }
    }
}

@Composable
fun rememberCurrencyFormatter(): (Double) -> String {
    val locale = remember { Locale("id", "ID") }
    val formatter = remember { NumberFormat.getCurrencyInstance(locale) }
    return { amount -> formatter.format(amount) }
}
