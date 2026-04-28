package com.chibychibystore.ui.promotion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.local.entity.PromotionType
import com.chibychibystore.ui.components.DatePickerDialog
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionAddEditScreen(
    navController: NavController,
    promotionId: Long = 0,
    viewModel: PromotionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(PromotionType.PERCENTAGE) }
    var value by remember { mutableStateOf("") }
    var minPurchaseAmount by remember { mutableStateOf("0") }
    var maxDiscountAmount by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    // Preserve the original createdAt across edits so saving doesn't reset it
    // to the current time (the Promotion entity defaults createdAt to Date()).
    var originalCreatedAt by remember { mutableStateOf<Date?>(null) }

    var isLoadingInitial by remember { mutableStateOf(promotionId != 0L) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(promotionId) {
        if (promotionId != 0L) {
            val promo = viewModel.getPromotionById(promotionId)
            if (promo != null) {
                name = promo.name
                description = promo.description
                type = promo.type
                value = if (type == PromotionType.PERCENTAGE) (promo.value * 100).toInt().toString() else promo.value.toString()
                minPurchaseAmount = promo.minPurchaseAmount.toString()
                maxDiscountAmount = promo.maxDiscountAmount?.toString() ?: ""
                isActive = promo.isActive
                startDate = promo.startDate
                endDate = promo.endDate
                originalCreatedAt = promo.createdAt
            }
            isLoadingInitial = false
        }
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (promotionId == 0L) "Tambah Promosi" else "Edit Promosi",
                onNavigationClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        if (isLoadingInitial) {
            LoadingIndicator(message = "Memuat data...")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Promosi") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tipe Promosi", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = type == PromotionType.PERCENTAGE,
                        onClick = { type = PromotionType.PERCENTAGE }
                    )
                    Text("Persentase (%)")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = type == PromotionType.FIXED_AMOUNT,
                        onClick = { type = PromotionType.FIXED_AMOUNT }
                    )
                    Text("Nominal Tetap (Rp)")
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (type == PromotionType.PERCENTAGE) "Nilai Persentase" else "Nilai Nominal") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { if (type == PromotionType.PERCENTAGE) Text("%") else Text("Rp") }
                )

                OutlinedTextField(
                    value = minPurchaseAmount,
                    onValueChange = { minPurchaseAmount = it },
                    label = { Text("Minimal Belanja") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("Rp ") }
                )

                if (type == PromotionType.PERCENTAGE) {
                    OutlinedTextField(
                        value = maxDiscountAmount,
                        onValueChange = { maxDiscountAmount = it },
                        label = { Text("Maksimal Diskon (Opsional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        prefix = { Text("Rp ") }
                    )
                }

                Text("Periode Promosi", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate?.let { dateFormat.format(it) } ?: "Pilih Tanggal",
                        onValueChange = {},
                        label = { Text("Mulai") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showStartDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = endDate?.let { dateFormat.format(it) } ?: "Pilih Tanggal",
                        onValueChange = {},
                        label = { Text("Berakhir") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { showEndDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                            }
                        }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Text("Aktifkan Promosi")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val valDouble = value.toDoubleOrNull() ?: 0.0
                        val finalValue = if (type == PromotionType.PERCENTAGE) valDouble / 100.0 else valDouble

                        val promo = Promotion(
                            id = promotionId,
                            name = name,
                            description = description,
                            type = type,
                            value = finalValue,
                            minPurchaseAmount = minPurchaseAmount.toDoubleOrNull() ?: 0.0,
                            maxDiscountAmount = maxDiscountAmount.toDoubleOrNull(),
                            isActive = isActive,
                            startDate = startDate,
                            endDate = endDate,
                            createdAt = originalCreatedAt ?: Date(),
                            updatedAt = Date()
                        )
                        viewModel.savePromotion(promo)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && value.isNotBlank() && !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Simpan Promosi")
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            initialDate = startDate,
            onDateSelected = { startDate = it },
            onDismiss = { showStartDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            initialDate = endDate,
            onDateSelected = { endDate = it },
            onDismiss = { showEndDatePicker = false }
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
