package com.chibychibystore.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Gudang
import com.chibychibystore.ui.components.AppTopBar
import com.chibychibystore.ui.components.ErrorMessage
import com.chibychibystore.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWarehouseScreen(
    navController: NavController,
    viewModel: WarehouseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var warehouseName by remember { mutableStateOf("") }
    var warehouseLocation by remember { mutableStateOf("") }
    var warehouseCapacity by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var capacityError by remember { mutableStateOf<String?>(null) }

    // Reset errors when inputs change
    LaunchedEffect(warehouseName) { nameError = null }
    LaunchedEffect(warehouseLocation) { locationError = null }
    LaunchedEffect(warehouseCapacity) { capacityError = null }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Tambah Gudang",
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val validationResult = validateInputs(warehouseName, warehouseLocation, warehouseCapacity)
                            if (validationResult.isValid) {
                                val capacity = warehouseCapacity.toIntOrNull() ?: 0
                                viewModel.createWarehouse(warehouseName, warehouseLocation, capacity)
                            } else {
                                nameError = validationResult.nameError
                                locationError = validationResult.locationError
                                capacityError = validationResult.capacityError
                            }
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                    }
                }
            )
        }
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
                    ErrorMessage(
                        message = uiState.error!!,
                        onRetry = { /* Clear error */ }
                    )
                }
                uiState.successMessage != null -> {
                    // Navigate back on success
                    LaunchedEffect(uiState.successMessage) {
                        navController.navigateUp()
                    }
                }
                else -> {
                    WarehouseForm(
                        name = warehouseName,
                        onNameChange = { warehouseName = it },
                        location = warehouseLocation,
                        onLocationChange = { warehouseLocation = it },
                        capacity = warehouseCapacity,
                        onCapacityChange = { warehouseCapacity = it },
                        nameError = nameError,
                        locationError = locationError,
                        capacityError = capacityError
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWarehouseScreen(
    navController: NavController,
    warehouseId: String,
    viewModel: WarehouseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var warehouseName by remember { mutableStateOf("") }
    var warehouseLocation by remember { mutableStateOf("") }
    var warehouseCapacity by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var capacityError by remember { mutableStateOf<String?>(null) }

    // Load warehouse data
    LaunchedEffect(warehouseId) {
        val warehouse = viewModel.getWarehouseById(warehouseId)
        warehouse?.let {
            warehouseName = it.nama
            warehouseLocation = it.lokasi
            warehouseCapacity = it.kapasitas.toString()
        }
    }

    // Reset errors when inputs change
    LaunchedEffect(warehouseName) { nameError = null }
    LaunchedEffect(warehouseLocation) { locationError = null }
    LaunchedEffect(warehouseCapacity) { capacityError = null }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Edit Gudang",
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val validationResult = validateInputs(warehouseName, warehouseLocation, warehouseCapacity)
                            if (validationResult.isValid) {
                                val capacity = warehouseCapacity.toIntOrNull() ?: 0
                                viewModel.updateWarehouse(warehouseId, warehouseName, warehouseLocation, capacity)
                            } else {
                                nameError = validationResult.nameError
                                locationError = validationResult.locationError
                                capacityError = validationResult.capacityError
                            }
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                    }
                }
            )
        }
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
                    ErrorMessage(
                        message = uiState.error!!,
                        onRetry = { /* Clear error */ }
                    )
                }
                uiState.successMessage != null -> {
                    // Navigate back on success
                    LaunchedEffect(uiState.successMessage) {
                        navController.navigateUp()
                    }
                }
                else -> {
                    WarehouseForm(
                        name = warehouseName,
                        onNameChange = { warehouseName = it },
                        location = warehouseLocation,
                        onLocationChange = { warehouseLocation = it },
                        capacity = warehouseCapacity,
                        onCapacityChange = { warehouseCapacity = it },
                        nameError = nameError,
                        locationError = locationError,
                        capacityError = capacityError
                    )
                }
            }
        }
    }
}

@Composable
private fun WarehouseForm(
    name: String,
    onNameChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    capacity: String,
    onCapacityChange: (String) -> Unit,
    nameError: String?,
    locationError: String?,
    capacityError: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warehouse Name
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nama Gudang") },
            placeholder = { Text("Masukkan nama gudang") },
            isError = nameError != null,
            supportingText = nameError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Warehouse Location
        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Lokasi Gudang") },
            placeholder = { Text("Masukkan lokasi gudang") },
            isError = locationError != null,
            supportingText = locationError?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Warehouse Capacity
        OutlinedTextField(
            value = capacity,
            onValueChange = onCapacityChange,
            label = { Text("Kapasitas Gudang") },
            placeholder = { Text("Masukkan kapasitas (opsional)") },
            isError = capacityError != null,
            supportingText = {
                Text("Kapasitas maksimal gudang dalam unit")
                capacityError?.let { Text(it) }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Informasi Gudang",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Nama gudang harus unik",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Lokasi membantu identifikasi gudang",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Kapasitas digunakan untuk monitoring ruang",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun validateInputs(name: String, location: String, capacity: String): ValidationResult {
    var nameError: String? = null
    var locationError: String? = null
    var capacityError: String? = null

    if (name.isBlank()) {
        nameError = "Nama gudang tidak boleh kosong"
    }

    if (location.isBlank()) {
        locationError = "Lokasi gudang tidak boleh kosong"
    }

    val capacityInt = capacity.toIntOrNull()
    if (capacity.isNotBlank() && (capacityInt == null || capacityInt < 0)) {
        capacityError = "Kapasitas harus berupa angka positif"
    }

    return ValidationResult(
        isValid = nameError == null && locationError == null && capacityError == null,
        nameError = nameError,
        locationError = locationError,
        capacityError = capacityError
    )
}

private data class ValidationResult(
    val isValid: Boolean,
    val nameError: String?,
    val locationError: String?,
    val capacityError: String?
)
