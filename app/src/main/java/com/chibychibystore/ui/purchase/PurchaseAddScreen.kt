package com.chibychibystore.ui.purchase

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.LoadingIndicator
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseAddScreen(
    navController: NavController,
    viewModel: PurchaseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cart by viewModel.cart.collectAsState()

    var selectedSupplierId by remember { mutableStateOf(-1L) }
    var selectedWarehouseId by remember { mutableStateOf(-1L) }
    var invoiceNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var showProductDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Tambah Pembelian",
                onNavigationClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Supplier Selection
            Text("Pilih Pemasok", style = MaterialTheme.typography.titleSmall)
            var expandedSupplier by remember { mutableStateOf(false) }
            val selectedSupplierName = uiState.suppliers.find { it.id == selectedSupplierId }?.name ?: "Pilih Pemasok"

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedSupplier = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedSupplierName)
                }
                DropdownMenu(
                    expanded = expandedSupplier,
                    onDismissRequest = { expandedSupplier = false }
                ) {
                    uiState.suppliers.forEach { supplier ->
                        DropdownMenuItem(
                            text = { Text(supplier.name) },
                            onClick = {
                                selectedSupplierId = supplier.id
                                expandedSupplier = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Warehouse Selection
            Text("Pilih Gudang Tujuan", style = MaterialTheme.typography.titleSmall)
            var expandedWarehouse by remember { mutableStateOf(false) }
            val selectedWarehouseName = uiState.warehouses.find { it.id == selectedWarehouseId }?.name ?: "Pilih Gudang"

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { expandedWarehouse = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedWarehouseName)
                }
                DropdownMenu(
                    expanded = expandedWarehouse,
                    onDismissRequest = { expandedWarehouse = false }
                ) {
                    uiState.warehouses.forEach { warehouse ->
                        DropdownMenuItem(
                            text = { Text(warehouse.name) },
                            onClick = {
                                selectedWarehouseId = warehouse.id
                                expandedWarehouse = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = invoiceNumber,
                onValueChange = { invoiceNumber = it },
                label = { Text("Nomor Invoice") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan (Opsional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Item Pembelian", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { showProductDialog = true }) {
                    Text("Tambah Item")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(cart) { item ->
                    CartItemRow(item = item, onDelete = { viewModel.removeFromCart(item.product.id) })
                }
                if (cart.isEmpty()) {
                    item {
                        Text("Keranjang masih kosong.", modifier = Modifier.padding(vertical = 16.dp))
                    }
                }
            }

            val total = cart.sumOf { it.quantity * it.unitPrice }
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", style = MaterialTheme.typography.titleLarge)
                Text(currencyFormat.format(total), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.createPurchase(selectedSupplierId, selectedWarehouseId, invoiceNumber, notes) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedSupplierId != -1L && selectedWarehouseId != -1L && invoiceNumber.isNotBlank() && cart.isNotEmpty() && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Simpan Pembelian")
                }
            }
        }
    }

    if (showProductDialog) {
        ProductSelectionDialog(
            products = uiState.products,
            onDismiss = { showProductDialog = false },
            onProductSelected = { product, qty, price ->
                viewModel.addToCart(product, qty, price)
                showProductDialog = false
            }
        )
    }

    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CartItemRow(item: CartItemPurchase, onDelete: () -> Unit) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, style = MaterialTheme.typography.bodyLarge)
                Text("${item.quantity} x ${currencyFormat.format(item.unitPrice)}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ProductSelectionDialog(
    products: List<Produk>,
    onDismiss: () -> Unit,
    onProductSelected: (Produk, Int, Double) -> Unit
) {
    var selectedProduct by remember { mutableStateOf<Produk?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Produk") },
        text = {
            Column {
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedProduct?.name ?: "Pilih Produk")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = { Text(product.name) },
                                onClick = {
                                    selectedProduct = product
                                    unitPrice = product.costPrice.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Jumlah") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("Harga Satuan") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = selectedProduct
                    val q = quantity.toIntOrNull() ?: 0
                    val pr = unitPrice.toDoubleOrNull() ?: 0.0
                    if (p != null && q > 0 && pr >= 0) {
                        onProductSelected(p, q, pr)
                    }
                },
                enabled = selectedProduct != null
            ) {
                Text("Tambah")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
