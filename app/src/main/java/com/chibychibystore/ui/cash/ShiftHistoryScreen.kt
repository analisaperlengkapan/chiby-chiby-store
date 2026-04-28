package com.chibychibystore.ui.cash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Shift
import com.chibychibystore.data.local.entity.ShiftStatus
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.LoadingIndicator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftHistoryScreen(
    navController: NavController,
    viewModel: ShiftViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Riwayat Shift",
                onNavigationClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(message = "Memuat data...")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.shifts) { shift ->
                    ShiftHistoryItem(shift = shift)
                }
                if (uiState.shifts.isEmpty()) {
                    item {
                        Text("Belum ada riwayat shift.")
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftHistoryItem(shift: Shift) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Shift #${shift.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val statusColor = if (shift.status == ShiftStatus.OPEN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                Text(
                    text = shift.status.name,
                    color = statusColor,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Mulai: ${dateFormat.format(shift.startTime)}")
            shift.endTime?.let { Text("Selesai: ${dateFormat.format(it)}") }

            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Modal: ${currencyFormat.format(shift.startingCash)}")
                shift.actualCash?.let { Text("Aktual: ${currencyFormat.format(it)}") }
            }
        }
    }
}
