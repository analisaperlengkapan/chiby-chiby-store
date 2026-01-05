package com.chibychibystore.ui.supplier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.data.local.entity.Pemasok
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierListScreen(
    navController: androidx.navigation.NavController,
    viewModel: SupplierViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Daftar Pemasok",
                onNavigationClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.onAddSupplierClick() }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Pemasok")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Cari pemasok...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                singleLine = true
            )

            if (uiState.isLoading) {
                LoadingIndicator(message = "Memuat data pemasok...")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.suppliers, key = { it.id }) { supplier ->
                        SupplierItem(
                            supplier = supplier,
                            onEditClick = { viewModel.onEditSupplierClick(supplier) },
                            onDeleteClick = { viewModel.deleteSupplier(supplier.id) }
                        )
                    }
                    if (uiState.suppliers.isEmpty()) {
                        item {
                            Text(
                                text = "Belum ada data pemasok",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        if (uiState.isAddEditDialogVisible) {
            SupplierDialog(
                supplier = uiState.selectedSupplier,
                onDismiss = { viewModel.onDismissAddEditDialog() },
                onSave = { formData ->
                    viewModel.saveSupplier(formData)
                }
            )
        }

        uiState.error?.let { error ->
             AlertDialog(
                onDismissRequest = { viewModel.onErrorShown() },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.onErrorShown() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun SupplierItem(
    supplier: Pemasok,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = supplier.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (!supplier.contact.isNullOrBlank()) {
                        Text(
                            text = supplier.contact,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus")
                    }
                }
            }
            if (!supplier.address.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = supplier.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SupplierDialog(
    supplier: Pemasok?,
    onDismiss: () -> Unit,
    onSave: (SupplierFormData) -> Unit
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var phone by remember { mutableStateOf(supplier?.contact ?: "") }
    var email by remember { mutableStateOf("") }
    var isNameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (supplier == null) "Tambah Pemasok" else "Edit Pemasok") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isNameError = it.isBlank()
                    },
                    label = { Text("Nama Pemasok *") },
                    isError = isNameError,
                    supportingText = if (isNameError) { { Text("Nama wajib diisi") } } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telepon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Alamat") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isNameError = true
                    } else {
                        onSave(
                            SupplierFormData(
                                name = name,
                                address = address.ifBlank { null },
                                phone = phone.ifBlank { null },
                                email = email.ifBlank { null }
                            )
                        )
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
