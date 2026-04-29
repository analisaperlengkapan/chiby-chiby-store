package com.chibychibystore.ui.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.AuditStatus
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditAddScreen(
    navController: NavController,
    viewModel: AuditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val auditItems by viewModel.auditItems.collectAsState()

    var selectedWarehouseId by remember { mutableStateOf(-1L) }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Mulai Stok Opname",
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
            if (selectedWarehouseId == -1L) {
                Text("Pilih Gudang", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                uiState.warehouses.forEach { warehouse ->
                    OutlinedButton(
                        onClick = {
                            selectedWarehouseId = warehouse.id
                            viewModel.startNewAudit(warehouse.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(warehouse.name)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                Text("Daftar Produk di Gudang", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(auditItems, key = { it.product.id }) { item ->
                        AuditItemRow(
                            item = item,
                            onActualChange = { viewModel.updateActualQuantity(item.product.id, it) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Catatan Audit") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.saveAudit(selectedWarehouseId, notes, AuditStatus.COMPLETED) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Selesaikan & Sesuaikan Stok")
                }
            }
        }
    }

    if (uiState.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(uiState.error!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun AuditItemRow(item: AuditItemInput, onActualChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.product.name, style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sistem: ${item.expectedQuantity}", style = MaterialTheme.typography.bodySmall)

                // Key the remembered text on the product id so the field re-initializes
                // if the parent ever swaps in a different product at this slot. We do
                // NOT key on `item.actualQuantity` because that would clobber the user's
                // in-progress edits whenever onActualChange fires and the parent emits
                // a new AuditItemInput with the updated quantity.
                var textValue by remember(item.product.id) {
                    mutableStateOf(item.actualQuantity.toString())
                }

                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        it.toIntOrNull()?.let { onActualChange(it) }
                    },
                    label = { Text("Aktual") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp)
                )
            }
        }
    }
}
