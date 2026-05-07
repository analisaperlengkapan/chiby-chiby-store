package com.chibychibystore.ui.barcode

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chibychibystore.R
import com.chibychibystore.ui.components.ChibyScaffold
import com.chibychibystore.ui.components.special.BarcodeScanner

@Composable
fun BarcodeScannerScreen(
    onNavigateBack: () -> Unit,
    onBarcodeScanned: (String) -> Unit
) {
    ChibyScaffold(
        title = stringResource(R.string.barcode_scanner_title),
        onNavigateUp = onNavigateBack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            BarcodeScanner(
                onBarcodeScanned = { barcode ->
                    onBarcodeScanned(barcode)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
