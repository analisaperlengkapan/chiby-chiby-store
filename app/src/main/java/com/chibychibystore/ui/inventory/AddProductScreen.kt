@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.inventory
import com.chibychibystore.ui.components.shared.ErrorMessage
import com.chibychibystore.ui.components.shared.LoadingIndicator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
// Using Icons.Filled.ArrowBack for navigation icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.ui.components.shared.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var editedProduct by remember {
        mutableStateOf(
            Produk(
                id = 0,
                name = "",
                barcode = null,
                categoryId = 0,
                costPrice = 0.0,
                sellingPrice = 0.0,
                stockQuantity = 0,
                warehouseId = 0,
                minStock = 0
            )
        )
    }

    // Initialize with empty product for adding
    LaunchedEffect(Unit) {
        // ProductDetailViewModel will default to create-mode when productId is null.
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Tambah Produk",
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = { navController.navigateUp() },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveProduct(editedProduct)
                        },
                        enabled = editedProduct.name.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
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
                    AddProductContent(
                        product = editedProduct,
                        onProductChange = { updatedProduct ->
                            editedProduct = updatedProduct
                        }
                    )
                }
            }
        }
    }

    // Success message display
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccessMessage()
            // Small delay before navigation to let user see the message
            kotlinx.coroutines.delay(1000)
            navController.navigateUp()
        }
    }
}

@Composable
private fun AddProductContent(
    product: Produk,
    onProductChange: (Produk) -> Unit
) {
    var editedProduct by remember { mutableStateOf(product) }

    // Update edited product when product changes
    LaunchedEffect(product) {
        editedProduct = product
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basic Information Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Informasi Dasar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )

                ProductTextField(
                    label = "Nama Produk",
                    value = editedProduct.name,
                    onValueChange = { editedProduct = editedProduct.copy(name = it) },
                    isRequired = true
                )

                ProductTextField(
                    label = "Barcode",
                    value = editedProduct.barcode ?: "",
                    onValueChange = { editedProduct = editedProduct.copy(barcode = it.takeIf { it.isNotBlank() }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // Pricing Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Harga",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )

                ProductTextField(
                    label = "Harga Beli",
                    value = editedProduct.costPrice.toString(),
                    onValueChange = { value ->
                        value.toDoubleOrNull()?.let { price ->
                            editedProduct = editedProduct.copy(costPrice = price)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isRequired = true
                )

                ProductTextField(
                    label = "Harga Jual",
                    value = editedProduct.sellingPrice.toString(),
                    onValueChange = { value ->
                        value.toDoubleOrNull()?.let { price ->
                            editedProduct = editedProduct.copy(sellingPrice = price)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isRequired = true
                )
            }
        }

        // Inventory Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Inventori",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )

                ProductTextField(
                    label = "Stok Awal",
                    value = editedProduct.stockQuantity.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { stock ->
                            editedProduct = editedProduct.copy(stockQuantity = stock)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isRequired = true
                )

                ProductTextField(
                    label = "Stok Minimum",
                    value = editedProduct.minStock.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { minStock ->
                            editedProduct = editedProduct.copy(minStock = minStock)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // Update the product in parent when edited
        LaunchedEffect(editedProduct) {
            onProductChange(editedProduct)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isRequired: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Row {
                Text(label)
                if (isRequired) {
                    Text(" *", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        keyboardOptions = keyboardOptions,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}