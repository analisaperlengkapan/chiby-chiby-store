package com.chibychibystore.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.chibychibystore.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    initialDate: Date? = null,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.time ?: System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Date(millis)
                    onDateSelected(selectedDate)
                }
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    initialStartDate: Date? = null,
    initialEndDate: Date? = null,
    onDateRangeSelected: (Date?, Date?) -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var isSelectingStart by remember { mutableStateOf(true) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (isSelectingStart) startDate?.time else endDate?.time ?: System.currentTimeMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedDate = Date(millis)
                    if (isSelectingStart) {
                        startDate = selectedDate
                        isSelectingStart = false
                        // Reset date picker for end date selection
                    } else {
                        endDate = selectedDate
                        onDateRangeSelected(startDate, endDate)
                        onDismiss()
                    }
                }
            }) {
                Text(if (isSelectingStart) "Pilih Tanggal Mulai" else "Pilih Tanggal Akhir")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (!isSelectingStart) {
                    isSelectingStart = true
                } else {
                    onDismiss()
                }
            }) {
                Text(if (isSelectingStart) "Batal" else "Kembali")
            }
        }
    ) {
        Column {
            Text(
                text = if (isSelectingStart) "Pilih Tanggal Mulai" else "Pilih Tanggal Akhir",
                style = MaterialTheme.typography.titleMedium,
                modifier = androidx.compose.ui.Modifier.padding(16.dp)
            )
            DatePicker(state = datePickerState)
        }
    }
}