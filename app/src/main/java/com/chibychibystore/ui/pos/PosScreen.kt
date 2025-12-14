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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.ui.navigation.Screen
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
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")) }
    val snackbarHostState = remember { SnackbarHostState() }

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
    if (uiState.showReceiptDialog && uiState.completedSaleId != null) {
        ReceiptDialog(
            saleId = uiState.completedSaleId!!,
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
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Cari produk...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        modifier = modifier,
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun ProductSearchPanel(
    searchResults: List<Produk>,
    isSearching: Boolean,
    onProductClick: (Produk) -> Unit,
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
                            onClick = { onProductClick(product) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductSearchItem(
    product: Produk,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Stok: ${product.stockQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stockQuantity > 0) Color.Green else Color.Red
                )
            }
            Text(
                text = formatCurrency(product.sellingPrice),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
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
                onProcessPayment = onProcessPayment
            )
        }
    }
}

@Composable
private fun CartItemRow(
    cartItem: CartItem,
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
    onProcessPayment: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Payment Method
            Text(
                text = "Metode Pembayaran",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.paymentMethod == "CASH",
                    onClick = { onPaymentMethodChange("CASH") },
                    label = { Text("Tunai") }
                )
                FilterChip(
                    selected = uiState.paymentMethod == "CARD",
                    onClick = { onPaymentMethodChange("CARD") },
                    label = { Text("Kartu") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Totals
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                Text(formatCurrency(uiState.subtotal), style = MaterialTheme.typography.bodyMedium)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pajak:", style = MaterialTheme.typography.bodyMedium)
                Text(formatCurrency(uiState.tax), style = MaterialTheme.typography.bodyMedium)
            }

            if (uiState.discount > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Diskon:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "-${formatCurrency(uiState.discount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatCurrency(uiState.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Process Payment Button
            Button(
                onClick = onProcessPayment,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.cartItems.isNotEmpty() && !uiState.isProcessingPayment,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isProcessingPayment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Memproses...")
                } else {
                    Text("Proses Pembayaran")
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

private fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(amount)
}