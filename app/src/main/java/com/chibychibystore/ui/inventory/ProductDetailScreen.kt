@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.inventory
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.ui.components.shared.ErrorMessage
import com.chibychibystore.ui.components.shared.LoadingIndicator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.R
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.dialogs.ConfirmDialog
import com.chibychibystore.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val product = uiState.product
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Listen for scanned barcode result
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val scannedBarcode = savedStateHandle?.getLiveData<String>("scanned_barcode")?.observeAsState()?.value

    LaunchedEffect(scannedBarcode) {
        val barcode = scannedBarcode as? String
        if (barcode != null && barcode.isNotBlank()) {
            // We need to update the local product state ...
            uiState.product?.let { current ->
                 viewModel.updateLocalProduct(current.copy(barcode = barcode))
            }
            // Clear the result
            savedStateHandle?.remove<String>("scanned_barcode")
        }
    }

    // Load product when screen opens
    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    Scaffold(
        topBar = {
            ProductDetailTopBar(
                uiState = uiState,
                onBackClick = { navController.navigateUp() },
                onEditClick = { viewModel.toggleEditMode() },
                onSaveClick = { viewModel.saveProduct(uiState.product ?: return@ProductDetailTopBar) },
                onCancelClick = { viewModel.cancelEdit() },
                onDeleteClick = { showDeleteDialog = true }
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
                product != null -> {
                    ProductDetailContent(
                        product = product,
                        isEditing = uiState.isEditing,
                        onProductChange = { updatedProduct ->
                            // Update the product in UI state via ViewModel helper
                            viewModel.updateLocalProduct(updatedProduct)
                        },
                        onScanClick = { navController.navigate(Screen.BarcodeScanner.route) },
                        onGenerateClick = {
                            scope.launch {
                                val newBarcode = viewModel.generateBarcodeValue()
                                uiState.product?.let { current ->
                                    viewModel.updateLocalProduct(current.copy(barcode = newBarcode))
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Hapus Product?",
            message = "Product ${uiState.product?.name} akan dihapus. Tindakan ini tidak dapat dibatalkan.",
            confirmText = "Hapus",
            dismissText = "Batal",
            onConfirm = {
                viewModel.deleteProduct()
                showDeleteDialog = false
                navController.navigateUp()
            },
            onDismiss = { showDeleteDialog = false }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailTopBar(
    uiState: ProductDetailUiState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    AppTopBar(
        title = if (uiState.isEditing) stringResource(R.string.common_edit) + " Product" else "Detail Product",
        navigationIcon = Icons.Filled.ArrowBack,
        onNavigationClick = onBackClick,
        actions = {
            if (uiState.isEditing) {
                IconButton(onClick = onSaveClick) {
                    Icon(Icons.Default.Save, contentDescription = stringResource(R.string.common_save))
                }
                IconButton(onClick = onCancelClick) {
                    Icon(Icons.Default.Cancel, contentDescription = stringResource(R.string.common_cancel))
                }
            } else {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.common_edit))
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.common_delete))
                }
            }
        }
    )
}

@Composable
private fun ProductDetailContent(
    product: Produk,
    isEditing: Boolean,
    onProductChange: (Produk) -> Unit,
    onScanClick: () -> Unit,
    onGenerateClick: () -> Unit
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
                    label = "Nama Product",
                    value = editedProduct.name,
                    onValueChange = { editedProduct = editedProduct.copy(name = it) },
                    enabled = isEditing,
                    isRequired = true
                )

                ProductTextField(
                    label = "Barcode",
                    value = editedProduct.barcode ?: "",
                    onValueChange = { editedProduct = editedProduct.copy(barcode = it.takeIf { it.isNotBlank() }) },
                    enabled = isEditing,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = if (isEditing) {
                        {
                            Row {
                                IconButton(onClick = onScanClick) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.barcode_scan_action))
                                }
                                IconButton(onClick = onGenerateClick) {
                                    Icon(Icons.Default.Autorenew, contentDescription = stringResource(R.string.barcode_generate_auto))
                                }
                            }
                        }
                    } else null
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
                    enabled = isEditing,
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
                    enabled = isEditing,
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
                    label = "Stok",
                    value = editedProduct.stockQuantity.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { stock ->
                            editedProduct = editedProduct.copy(stockQuantity = stock)
                        }
                    },
                    enabled = isEditing,
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
                    enabled = isEditing,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }

        // Update the product in parent when edited
        if (isEditing) {
            LaunchedEffect(editedProduct) {
                onProductChange(editedProduct)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isRequired: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
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
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = trailingIcon
    )
}