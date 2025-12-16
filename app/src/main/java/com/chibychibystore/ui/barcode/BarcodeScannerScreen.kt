@file:Suppress("DEPRECATION")
package com.chibychibystore.ui.barcode

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.chibychibystore.R
import com.chibychibystore.ui.components.shared.AppTopBar
import com.google.zxing.Result
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

/**
 * Barcode Scanner Screen
 * Menggunakan ZXing library untuk scan barcode melalui kamera
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScannerScreen(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: BarcodeScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    // Check camera permission on launch
    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.onPermissionResult(true)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
                AppTopBar(
                title = stringResource(R.string.barcode_scanner_title),
                navigationIcon = Icons.Filled.ArrowBack,
                onNavigationClick = onDismiss
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                !uiState.hasCameraPermission -> {
                    // Permission denied or not requested
                    PermissionRequiredContent(
                        onRequestPermission = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }

                uiState.isScanning -> {
                    // Camera scanner view
                    BarcodeScannerView(
                        onBarcodeScanned = { barcode ->
                            viewModel.onBarcodeDetected(barcode)
                            onBarcodeScanned(barcode)
                        },
                        onError = { error ->
                            viewModel.onScanError(error)
                        }
                    )

                    // Overlay with instructions
                    ScannerOverlay(
                        lastScanned = uiState.lastScannedBarcode,
                        isProcessing = uiState.isProcessing
                    )
                }

                else -> {
                    // Loading or error state
                    LoadingContent()
                }
            }

            // Manual input button
            FloatingActionButton(
                onClick = { viewModel.showManualInput() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.barcode_manual_input)
                )
            }
        }
    }

    // Manual input dialog
    if (uiState.showManualInput) {
        ManualInputDialog(
            onBarcodeEntered = { barcode ->
                viewModel.onManualBarcodeEntered(barcode)
                onBarcodeScanned(barcode)
            },
            onDismiss = { viewModel.hideManualInput() }
        )
    }
}

@Composable
private fun PermissionRequiredContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.barcode_camera_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.barcode_camera_permission_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.barcode_grant_permission))
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ScannerOverlay(
    lastScanned: String?,
    isProcessing: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.barcode_scan_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (lastScanned != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.barcode_last_scanned, lastScanned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.barcode_processing),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BarcodeScannerView(
    onBarcodeScanned: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current

    AndroidView(
        factory = { ctx ->
            DecoratedBarcodeView(ctx).apply {
                // Configure barcode view
                cameraSettings.isAutoFocusEnabled = true
                cameraSettings.isBarcodeSceneModeEnabled = true
                cameraSettings.isMeteringEnabled = true

                // Set barcode callback
                decodeContinuous(object : BarcodeCallback {
                    override fun barcodeResult(result: BarcodeResult?) {
                        result?.let {
                            val barcode = it.text
                            if (!barcode.isNullOrBlank()) {
                                Log.d("BarcodeScanner", "Scanned barcode: $barcode")
                                onBarcodeScanned(barcode)
                            }
                        }
                    }

                    override fun possibleResultPoints(resultPoints: MutableList<com.google.zxing.ResultPoint>?) {
                        // Optional: Handle possible result points for UI feedback
                    }
                })

                // Handle decode errors
                setStatusText("")
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            try {
                if (!view.isActivated) {
                    view.resume()
                }
            } catch (e: Exception) {
                Log.e("BarcodeScanner", "Error updating scanner view", e)
                onError(e.message ?: "Scanner error")
            }
        }
    )
}

@Composable
private fun ManualInputDialog(
    onBarcodeEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var barcodeText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.barcode_manual_input_title)) },
        text = {
            Column {
                Text(stringResource(R.string.barcode_manual_input_message))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = barcodeText,
                    onValueChange = { barcodeText = it },
                    label = { Text(stringResource(R.string.barcode_enter_code)) },
                    placeholder = { Text("123456789012") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (barcodeText.isNotBlank()) {
                        onBarcodeEntered(barcodeText.trim())
                    }
                },
                enabled = barcodeText.isNotBlank()
            ) {
                Text(stringResource(R.string.common_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}