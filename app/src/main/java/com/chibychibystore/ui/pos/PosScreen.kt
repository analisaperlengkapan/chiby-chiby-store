package com.chibychibystore.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavController
import com.chibychibystore.ui.navigation.Screen
import com.chibychibystore.ui.theme.Success
import com.chibychibystore.ui.theme.Error as CustomError
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.CardItem
import com.chibychibystore.ui.components.shared.ErrorMessage
import com.chibychibystore.ui.components.shared.LoadingIndicator

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

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Point of Sale",
                actions = {
                    IconButton(onClick = { viewModel.clearCart() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Cart")
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.BarcodeScanner.route)
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.searchProducts(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Main Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Left Panel - Product Search Results
                ProductSearchPanel(
                    searchResults = uiState.searchResults,
                    isSearching = uiState.isSearching,
                    onProductClick = { product ->
                        viewModel.addProductToCart(product)
                    },
                    formatCurrency = formatCurrency,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // Right Panel - Cart and Payment
                CartAndPaymentPanel(
                    uiState = uiState,
                    onUpdateQuantity = { productId, quantity ->
                        viewModel.updateCartItemQuantity(productId, quantity)
                    },
                    onRemoveItem = { productId ->
                        viewModel.removeCartItem(productId)
                    },
                    onPaymentMethodChange = { method ->
                        viewModel.setPaymentMethod(method)
                    },
                    onProcessPayment = {
                        viewModel.processPayment()
                    },
                    formatCurrency = formatCurrency,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }

    // Error/Success Messages
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
        }
    }

    // Receipt Dialog after successful payment
    val completedSaleId = uiState.completedSaleId
    if (uiState.showReceiptDialog && completedSaleId != null) {
        ReceiptDialog(
            saleId = completedSaleId,
            isPrinting = uiState.isPrintingReceipt,
            onPrintReceipt = { viewModel.printReceipt() },
            onStartNewTransaction = { viewModel.startNewTransaction() },
            onDismiss = { viewModel.dismissReceiptDialog() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Cari produk berdasarkan nama atau barcode...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
        },
        modifier = modifier,
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun ProductSearchPanel(
    searchResults: List<Produk>,
    isSearching: Boolean,
    onProductClick: (Produk) -> Unit,
    formatCurrency: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Hasil Pencarian",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ketik untuk mencari produk",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = searchResults,
                        key = { it.id }
                    ) { product ->
                        ProductSearchItem(
                            product = product,
                            formatCurrency = formatCurrency,
                            onClick = { onProductClick(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun rememberCurrencyFormatter(): (Double) -> String {
    val locale = remember { Locale("id", "ID") }
    val formatter = remember { NumberFormat.getCurrencyInstance(locale) }
    return { amount -> formatter.format(amount) }
}

@Composable
private fun ProductSearchItem(
    product: Produk,
    formatCurrency: (Double) -> String,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (product.stockQuantity > 0) "Stok: ${product.stockQuantity}" else "Stok Habis",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stockQuantity > 0) Success else colorScheme.error
                )
            }
            Text(
                text = formatCurrency(product.sellingPrice),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )
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
    formatCurrency: (Double) -> String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Keranjang Belanja",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keranjang kosong",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.cartItems) { cartItem ->
                        CartItemRow(
                            cartItem = cartItem,
                            formatCurrency = formatCurrency,
                            onUpdateQuantity = { quantity ->
                                onUpdateQuantity(cartItem.product.id, quantity)
                            },
                            onRemove = {
                                onRemoveItem(cartItem.product.id)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Summary
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onUpdateQuantity(cartItem.quantity - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease quantity")
                }

                Text(
                    text = cartItem.quantity.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                IconButton(
                    onClick = { onUpdateQuantity(cartItem.quantity + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase quantity")
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove item")
                }
            }

            Text(
                text = formatCurrency(cartItem.totalPrice),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PaymentSummary(
    uiState: PosUiState,
    onPaymentMethodChange: (String) -> Unit,
    onProcessPayment: () -> Unit,
    formatCurrency: (Double) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Payment Method Selection
            Text(
                "Metode Pembayaran",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("CASH" to "Tunai", "CARD" to "Kartu").forEach { (value, label) ->
                    val isSelected = uiState.paymentMethod == value
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPaymentMethodChange(value) },
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val colorScheme = MaterialTheme.colorScheme
                Text("Subtotal", style = MaterialTheme.typography.bodyLarge, color = colorScheme.onSurfaceVariant)
                Text(formatCurrency(uiState.subtotal), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pajak (10%)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCurrency(uiState.tax), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }

            if (uiState.discount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Diskon", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                    Text(
                        "-${formatCurrency(uiState.discount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TOTAL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    formatCurrency(uiState.total),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Process Payment Button
            Button(
                onClick = onProcessPayment,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState.cartItems.isNotEmpty() && !uiState.isProcessingPayment,
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                if (uiState.isProcessingPayment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Bayar Sekarang",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BarcodeScannerDialog(
    onDismiss: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan Barcode") },
        text = {
            Column {
                Text("Fitur barcode scanner akan diimplementasikan dengan ZXing library.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = "",
                    onValueChange = onBarcodeScanned,
                    label = { Text("Masukkan barcode manual") },
                    placeholder = { Text("Contoh: 123456789012") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
