package com.chibychibystore.ui.components.special

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chibychibystore.BuildConfig

@Composable
fun BarcodeScanner(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isScanning: Boolean = true
) {
    var isProcessing by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary

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
            // Camera preview placeholder (would be replaced with actual camera view)
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
                    topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                    size = Size(rectSize, rectSize)
                )

                // Draw scanning rectangle border
                drawRect(
                    color = primaryColor,
                    topLeft = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                    size = Size(rectSize, rectSize),
                    style = Stroke(width = 3f)
                )

                // Draw corner brackets
                val cornerLength = 30f
                val cornerWidth = 3f

                // Top-left corner
                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(rectLeft, rectTop + cornerLength),
                    end = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                    strokeWidth = cornerWidth
                )
                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(rectLeft, rectTop),
                    end = androidx.compose.ui.geometry.Offset(rectLeft + cornerLength, rectTop),
                    strokeWidth = cornerWidth
                )

                // Top-right corner
                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(rectLeft + rectSize - cornerLength, rectTop),
                    end = androidx.compose.ui.geometry.Offset(rectLeft + rectSize, rectTop),
                    strokeWidth = cornerWidth
                )
                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(rectLeft + rectSize, rectTop),
                    end = androidx.compose.ui.geometry.Offset(rectLeft + rectSize, rectTop + cornerLength),
                    strokeWidth = cornerWidth
                )

                // Bottom-left corner
                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(rectLeft, rectTop + rectSize - cornerLength),
                    end = androidx.compose.ui.geometry.Offset(rectLeft, rectTop + rectSize),
                    strokeWidth = cornerWidth
                )
                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(rectLeft, rectTop + rectSize),
                    end = androidx.compose.ui.geometry.Offset(rectLeft + cornerLength, rectTop + rectSize),
                    strokeWidth = cornerWidth
                )

                // Bottom-right corner
                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(rectLeft + rectSize - cornerLength, rectTop + rectSize),
                    end = androidx.compose.ui.geometry.Offset(rectLeft + rectSize, rectTop + rectSize),
                    strokeWidth = cornerWidth
                )
                drawLine(
                    color = primaryColor,
                    start = androidx.compose.ui.geometry.Offset(rectLeft + rectSize, rectTop + rectSize - cornerLength),
                    end = androidx.compose.ui.geometry.Offset(rectLeft + rectSize, rectTop + rectSize),
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