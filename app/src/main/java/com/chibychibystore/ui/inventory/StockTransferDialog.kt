package com.chibychibystore.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.chibychibystore.data.local.entity.Warehouse
import com.chibychibystore.data.local.entity.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTransferDialog(
    product: Product,
    fromWarehouse: Warehouse,
    availableWarehouses: List<Warehouse>,
    onTransfer: (toWarehouseId: Long, quantity: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedWarehouse by remember { mutableStateOf<Warehouse?>(null) }
    var quantity by remember { mutableStateOf("") }
    var quantityError by remember { mutableStateOf<String?>(null) }

    // Reset state when dialog opens
    LaunchedEffect(product) {
        selectedWarehouse = null
        quantity = ""
        quantityError = null
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transfer Stok",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Dari: ${fromWarehouse.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Stok tersedia: ${product.stockQuantity}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Target warehouse selection
                Text(
                    text = "Pilih Warehouse Tujuan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter out the source warehouse
                val targetWarehouses = availableWarehouses.filter { it.id != fromWarehouse.id }

                if (targetWarehouses.isEmpty()) {
                    Text(
                        text = "Tidak ada gudang lain yang tersedia",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        targetWarehouses.forEach { warehouse ->
                            WarehouseSelectionCard(
                                warehouse = warehouse,
                                isSelected = selectedWarehouse?.id == warehouse.id,
                                onClick = { selectedWarehouse = warehouse }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quantity input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        quantity = it
                        quantityError = validateQuantity(it, product.stockQuantity)
                    },
                    label = { Text("Jumlah Transfer") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = quantityError != null,
                    supportingText = {
                        quantityError?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val qty = quantity.toIntOrNull()
                            if (selectedWarehouse != null && qty != null && qty > 0 && qty <= product.stockQuantity) {
                                selectedWarehouse?.let { onTransfer(it.id, qty) }
                                onDismiss()
                            }
                        },
                        enabled = selectedWarehouse != null &&
                                quantity.toIntOrNull()?.let { it > 0 && it <= product.stockQuantity } == true
                    ) {
                        Text("Transfer")
                    }
                }
            }
        }
    }
}

@Composable
private fun WarehouseSelectionCard(
    warehouse: Warehouse,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder()
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = warehouse.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )

                warehouse.location?.let { location ->
                    if (location.isNotBlank()) {
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun validateQuantity(input: String, maxStock: Int): String? {
    val quantity = input.toIntOrNull()
    return when {
        input.isBlank() -> null
        quantity == null -> "Jumlah harus berupa angka"
        quantity <= 0 -> "Jumlah harus lebih dari 0"
        quantity > maxStock -> "Jumlah tidak boleh melebihi stok tersedia ($maxStock)"
        else -> null
    }
}