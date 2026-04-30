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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.R
import com.chibychibystore.data.local.entity.Produk
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    viewModel: ProductDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    // Listen for scanned barcode result
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    val scannedBarcode = savedStateHandle?.getLiveData<String>("scanned_barcode")?.observeAsState()?.value

    LaunchedEffect(scannedBarcode) {
        scannedBarcode?.let { barcode ->
            if (barcode.isNotBlank()) {
                editedProduct = editedProduct.copy(barcode = barcode)
                // Clear the result to avoid re-triggering
                savedStateHandle?.remove<String>("scanned_barcode")
            }
        }
    }

    // Initialize with empty product for adding
    LaunchedEffect(Unit) {
        // ProductDetailViewModel will default to create-mode when productId is null.
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.pos_add_product), // Reusing similar string or add new one if strictly "Tambah Product"
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = { navController.navigateUp() },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveProduct(editedProduct)
                        },
                        enabled = editedProduct.name.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = stringResource(R.string.common_save))
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
                        navController = navController,
                        viewModel = viewModel,
                        scope = scope,
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
    navController: NavController,
    viewModel: ProductDetailViewModel,
    scope: kotlinx.coroutines.CoroutineScope,
    onProductChange: (Produk) -> Unit
) {
    var editedProduct by remember { mutableStateOf(product) }
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // In a real app, you'd copy this file to internal storage
            // For now, we'll just store the string URI
            editedProduct = editedProduct.copy(imagePath = it.toString())
        }
    }

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
        // Image Section
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Foto Produk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (editedProduct.imagePath != null) {
                        // In a real app, use Coil or Glide to load the image
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Foto Produk",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            Icons.Default.AddAPhoto,
                            contentDescription = "Tambah Foto",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (editedProduct.imagePath != null) {
                    TextButton(onClick = { editedProduct = editedProduct.copy(imagePath = null) }) {
                        Text("Hapus Foto", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

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
                    isRequired = true
                )

                ProductTextField(
                    label = "Barcode",
                    value = editedProduct.barcode ?: "",
                    onValueChange = { editedProduct = editedProduct.copy(barcode = it.takeIf { it.isNotBlank() }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { navController.navigate(Screen.BarcodeScanner.route) }) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.barcode_scan_action))
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    val newBarcode = viewModel.generateBarcodeValue()
                                    editedProduct = editedProduct.copy(barcode = newBarcode)
                                }
                            }) {
                                Icon(Icons.Default.Autorenew, contentDescription = stringResource(R.string.barcode_generate_auto))
                            }
                        }
                    }
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
        keyboardOptions = keyboardOptions,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        trailingIcon = trailingIcon
    )
}