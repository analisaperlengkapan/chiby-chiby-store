package com.chibychibystore.ui.pos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavController
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
import com.chibychibystore.ui.theme.White
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    navController: NavController,
    viewModel: PosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val formatCurrency = rememberCurrencyFormatter()

    // Listen for scan results from BarcodeScannerScreen
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val scannedBarcode by savedStateHandle?.getLiveData<String>("scanned_barcode")?.observeAsState() ?: mutableStateOf(null)

    LaunchedEffect(scannedBarcode) {
        scannedBarcode?.let { barcode ->
            viewModel.onBarcodeScanned(barcode)
            savedStateHandle?.remove<String>("scanned_barcode")
        }
    }

    ChibyScaffold(
        title = "Point of Sale",
        onNavigateUp = { navController.navigateUp() }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Action Bar with Search
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChibyInput(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        label = "Cari Produk...",
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = ChibyPinkPrimary)
                        },
                        trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        } else null
                    )

                    IconButton(
                        onClick = { navController.navigate(Screen.BarcodeScanner.route) },
                        modifier = Modifier
                            .size(56.dp)
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan",
                            tint = ChibyPinkPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Left Panel - Product Results
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .padding(end = 16.dp)
                    ) {
                        ProductSearchPanel(
                            searchResults = uiState.searchResults,
                            isSearching = uiState.isSearching,
                            onProductClick = { product -> viewModel.addProductToCart(product) },
                            formatCurrency = formatCurrency
                        )
                    }

                    // Right Panel - Cart Summary
                    Box(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight()
                    ) {
                        CartAndPaymentPanel(
                            uiState = uiState,
                            onUpdateQuantity = { productId, qty -> viewModel.updateCartItemQuantity(productId, qty) },
                            onRemoveItem = { productId -> viewModel.removeCartItem(productId) },
                            onPaymentMethodChange = { viewModel.setPaymentMethod(it) },
                            onProcessPayment = { viewModel.processPayment() },
                            onClearCart = { viewModel.clearCart() },
                            formatCurrency = formatCurrency
                        )
                    }
                }
            }

            // Snackbar Host
             SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // Effects for Toast/Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            viewModel.clearSuccessMessage()
        }
    }

    // Receipt Dialog (Implement separately or keep if exists)
}

@Composable
private fun ProductSearchPanel(
    searchResults: List<Produk>,
    isSearching: Boolean,
    onProductClick: (Produk) -> Unit,
    formatCurrency: (Double) -> String
) {
    if (isSearching) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingIndicator("Mencari produk...")
        }
    } else if (searchResults.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.SearchOff,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(searchResults, key = { it.id }) { product ->
                ChibyCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onProductClick(product) },
                    elevation = 2
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
        }
    }
}

@Composable
private fun CartAndPaymentPanel(
    uiState: PosUiState,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onProcessPayment: () -> Unit,
    onClearCart: () -> Unit,
    formatCurrency: (Double) -> String
) {
    ChibyCard(
        modifier = Modifier.fillMaxSize(),
        elevation = 4
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Keranjang Belanja",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.cartItems.isNotEmpty()) {
                    IconButton(onClick = onClearCart) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Error)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Cart Items
            if (uiState.cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Keranjang Kosong",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.cartItems) { item ->
                        CartItemRow(
                            cartItem = item,
                            formatCurrency = formatCurrency,
                            onUpdateQuantity = { qty -> onUpdateQuantity(item.product.id, qty) },
                            onRemove = { onRemoveItem(item.product.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Payment Summary Section
            PaymentSummary(
                uiState = uiState,
                onPaymentMethodChange = onPaymentMethodChange,
                onProcessPayment = onProcessPayment,
                formatCurrency = formatCurrency
            )
        }
    }
}

@Composable
private fun CartItemRow(
    cartItem: CartItem,
    formatCurrency: (Double) -> String,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cartItem.product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatCurrency(cartItem.unitPrice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onUpdateQuantity(cartItem.quantity - 1) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, tint = ChibyPinkPrimary)
            }
            Text(
                text = "${cartItem.quantity}",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { onUpdateQuantity(cartItem.quantity + 1) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = ChibyPinkPrimary)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatCurrency(cartItem.totalPrice),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PaymentSummary(
    uiState: PosUiState,
    onPaymentMethodChange: (String) -> Unit,
    onProcessPayment: () -> Unit,
    formatCurrency: (Double) -> String
) {
    Column {
        // Payment Method Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val methods = listOf("CASH" to "Tunai", "QRIS" to "QRIS")
            methods.forEach { (key, label) ->
                val isSelected = uiState.paymentMethod == key
                val containerColor = if (isSelected) ChibyPinkPrimary else MaterialTheme.colorScheme.surface
                val contentColor = if (isSelected) White else MaterialTheme.colorScheme.onSurface

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { onPaymentMethodChange(key) },
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    shape = MaterialTheme.shapes.medium,
                    border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.3f)) else null
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = contentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Totals
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
            Text(formatCurrency(uiState.subtotal), style = MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Pajak", style = MaterialTheme.typography.bodyMedium)
            Text(formatCurrency(uiState.tax), style = MaterialTheme.typography.bodyMedium)
        }
        if (uiState.discount > 0) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Diskon", style = MaterialTheme.typography.bodyMedium, color = Success)
                Text("-${formatCurrency(uiState.discount)}", style = MaterialTheme.typography.bodyMedium, color = Success)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Total Tagihan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                formatCurrency(uiState.total),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = ChibyPinkPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        ChibyButton(
            text = "Bayar Sekarang",
            onClick = onProcessPayment,
            enabled = uiState.cartItems.isNotEmpty(),
            isLoading = uiState.isProcessingPayment
        )
    }
}

@Composable
fun rememberCurrencyFormatter(): (Double) -> String {
    val locale = remember { Locale("id", "ID") }
    val formatter = remember { NumberFormat.getCurrencyInstance(locale) }
    return { amount -> formatter.format(amount) }
}
