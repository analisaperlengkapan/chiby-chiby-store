package com.chibychibystore.ui.audit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.StokOpname
import com.chibychibystore.data.local.entity.AuditStatus
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.LoadingIndicator
import com.chibychibystore.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditListScreen(
    navController: NavController,
    viewModel: AuditViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Stok Opname",
                onNavigationClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.AuditAdd.route) }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Audit")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(message = "Memuat data audit...")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.audits) { audit ->
                    AuditHistoryItem(audit = audit)
                }
                if (uiState.audits.isEmpty()) {
                    item {
                        Text("Belum ada riwayat stok opname.")
                    }
                }
            }
        }
    }
}

@Composable
fun AuditHistoryItem(audit: StokOpname) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

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
                    text = "Audit #${audit.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = audit.status.name,
                    color = if (audit.status == AuditStatus.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tanggal: ${dateFormat.format(audit.auditDate)}")
            Text("Gudang ID: ${audit.warehouseId}")
            if (!audit.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Catatan: ${audit.notes}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
