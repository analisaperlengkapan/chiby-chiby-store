package com.chibychibystore.ui.components.special

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.chibychibystore.BuildConfig
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun BarcodeScanner(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isScanning: Boolean = true
) {
    var isProcessing by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val currentIsScanning by rememberUpdatedState(isScanning)
    val currentOnBarcodeDetected by rememberUpdatedState(onBarcodeDetected)

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val executor = ContextCompat.getMainExecutor(ctx)

                    cameraProviderFuture.addListener({
                        try {
                            // Check permission before proceeding
                            val hasPermission = ContextCompat.checkSelfPermission(
                                ctx,
                                android.Manifest.permission.CAMERA
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also {
                                        it.setAnalyzer(
                                            analysisExecutor,
                                            BarcodeAnalyzer { barcode ->
                                                if (currentIsScanning) {
                                                    currentOnBarcodeDetected(barcode)
                                                }
                                            }
                                        )
                                    }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) {
                                    Log.e("BarcodeScanner", "Camera binding failed", e)
                                }
                            } else {
                                Log.w("BarcodeScanner", "Camera permission missing")
                            }
                        } catch (e: Exception) {
                             Log.e("BarcodeScanner", "Camera provider init failed", e)
                        }
                    }, executor)

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanner Overlay
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Draw scanner overlay
                val canvasWidth = size.width
                val canvasHeight = size.height
                val rectSize = 200f
                val rectLeft = (canvasWidth - rectSize) / 2
                val rectTop = (canvasHeight - rectSize) / 2

                // Draw semi-transparent overlay
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    size = size
                )

                // Clear the scanning area
                drawRect(
                    color = Color.Transparent,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectSize, rectSize)
                )

                // Draw scanning rectangle border
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectSize, rectSize),
                    style = Stroke(width = 3f)
                )

                // Draw corner brackets
                val cornerLength = 30f
                val cornerWidth = 3f

                // Top-left corner
                drawLine(
                    color = primaryColor,
                    start = Offset(rectLeft, rectTop + cornerLength),
                    end = Offset(rectLeft, rectTop),
                    strokeWidth = cornerWidth
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(rectLeft, rectTop),
                    end = Offset(rectLeft + cornerLength, rectTop),
                    strokeWidth = cornerWidth
                )

                // Top-right corner
                drawLine(
                    color = primaryColor,
                    start = Offset(rectLeft + rectSize - cornerLength, rectTop),
                    end = Offset(rectLeft + rectSize, rectTop),
                    strokeWidth = cornerWidth
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(rectLeft + rectSize, rectTop),
                    end = Offset(rectLeft + rectSize, rectTop + cornerLength),
                    strokeWidth = cornerWidth
                )

                // Bottom-left corner
                drawLine(
                    color = primaryColor,
                    start = Offset(rectLeft, rectTop + rectSize - cornerLength),
                    end = Offset(rectLeft, rectTop + rectSize),
                    strokeWidth = cornerWidth
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(rectLeft, rectTop + rectSize),
                    end = Offset(rectLeft + cornerLength, rectTop + rectSize),
                    strokeWidth = cornerWidth
                )

                // Bottom-right corner
                drawLine(
                    color = primaryColor,
                    start = Offset(rectLeft + rectSize - cornerLength, rectTop + rectSize),
                    end = Offset(rectLeft + rectSize, rectTop + rectSize),
                    strokeWidth = cornerWidth
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(rectLeft + rectSize, rectTop + rectSize - cornerLength),
                    end = Offset(rectLeft + rectSize, rectTop + rectSize),
                    strokeWidth = cornerWidth
                )
            }

            if (isScanning) {
                Text(
                    text = "Arahkan kamera ke barcode",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                )

                // Helpful debug: allow simulation of barcode detection in preview/dev builds
                if (BuildConfig.DEBUG) {
                    androidx.compose.material3.Button(
                        onClick = { onBarcodeDetected("SIMULATED_BARCODE") },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(text = "Simulate Scan")
                    }
                }
            }

            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.TopCenter)
                        .padding(top = 32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val map = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.QR_CODE,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39
            )
        )
        setHints(map)
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage != null) {
            val buffer = image.planes[0].buffer
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            val height = image.height
            val width = image.width
            val stride = image.planes[0].rowStride

            // Create a luminance source from the image data
            // Note: CameraX standard is YUV_420_888, the first plane is Y (luminance)
            val source = PlanarYUVLuminanceSource(
                data,
                stride,
                height,
                0,
                0,
                width,
                height,
                false
            )

            // Handle rotation
            val rotationDegrees = image.imageInfo.rotationDegrees
            var rotatedSource: LuminanceSource = source

            // To convert from CW rotation needed (rotationDegrees) to CCW operations:
            // 90 CW = 270 CCW (3 rotations)
            // 180 CW = 180 CCW (2 rotations)
            // 270 CW = 90 CCW (1 rotation)
            // 0 CW = 0 CCW
            // Formula: (4 - (degrees / 90)) % 4
            val rotations = (4 - (rotationDegrees / 90)) % 4
            repeat(rotations) {
                rotatedSource = rotatedSource.rotateCounterClockwise()
            }

            val binaryBitmap = BinaryBitmap(HybridBinarizer(rotatedSource))

            try {
                val result = reader.decodeWithState(binaryBitmap)
                onBarcodeDetected(result.text)
            } catch (e: Exception) {
                // NotFoundException is common, ignore
            } finally {
                reader.reset()
                image.close()
            }
        } else {
            image.close()
        }
    }
}
