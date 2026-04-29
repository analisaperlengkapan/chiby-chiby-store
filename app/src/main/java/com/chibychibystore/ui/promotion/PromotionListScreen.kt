package com.chibychibystore.ui.promotion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.chibychibystore.data.local.entity.Promotion
import com.chibychibystore.data.local.entity.PromotionType
import com.chibychibystore.ui.components.shared.AppTopBar
import com.chibychibystore.ui.components.shared.LoadingIndicator
import com.chibychibystore.ui.navigation.Screen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionListScreen(
    navController: NavController,
    viewModel: PromotionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Manajemen Promosi",
                onNavigationClick = { navController.popBackStack() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.PromotionAdd.route) }) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Promosi")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            LoadingIndicator(message = "Memuat data promosi...")
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.promotions) { promotion ->
                    PromotionItem(
                        promotion = promotion,
                        onEdit = { navController.navigate(Screen.PromotionEdit.createRoute(promotion.id)) },
                        onDelete = { viewModel.deletePromotion(promotion) }
                    )
                }
                if (uiState.promotions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Belum ada data promosi.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionItem(
    promotion: Promotion,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = promotion.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = promotion.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val valueText = if (promotion.type == PromotionType.PERCENTAGE) {
                    // Preserve fractional percentages so the list display matches the
                    // value used by PromoServiceImpl.calculateDiscountForPromo (which
                    // multiplies subtotal by promo.value). Truncating with .toInt()
                    // here would render 12.5% as "12%", contradicting the edit screen
                    // (PromotionAddEditScreen.kt:60-68) that shows the full precision
                    // and confusing users about the actual discount applied.
                    val pct = promotion.value * 100
                    val pctText = if (pct == pct.toLong().toDouble()) pct.toLong().toString() else pct.toString()
                    "$pctText%"
                } else {
                    currencyFormat.format(promotion.value)
                }

                Text(
                    text = "Diskon: $valueText",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                val statusText = if (promotion.isActive) "Aktif" else "Non-aktif"
                val statusColor = if (promotion.isActive) Color(0xFF4CAF50) else Color.Gray

                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Min. Belanja: ${currencyFormat.format(promotion.minPurchaseAmount)}",
                style = MaterialTheme.typography.labelSmall
            )

            if (promotion.startDate != null && promotion.endDate != null) {
                Text(
                    text = "Periode: ${dateFormat.format(promotion.startDate)} - ${dateFormat.format(promotion.endDate)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
