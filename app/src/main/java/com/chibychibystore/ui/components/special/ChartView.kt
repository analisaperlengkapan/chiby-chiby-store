package com.chibychibystore.ui.components.special

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter

@Composable
fun ChartView(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Tidak ada data", color = textColor)
        }
        return
    }

    val maxValue = remember(data) { data.maxOfOrNull { it.value } ?: 0f }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM") }

    Canvas(modifier = modifier.padding(top = 20.dp, bottom = 20.dp, start = 10.dp, end = 10.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = width / (data.size * 2f)
        val spacing = width / data.size

        // Draw Bars
        data.forEachIndexed { index, item ->
            val barHeight = if (maxValue > 0) (item.value / maxValue) * height else 0f
            val x = index * spacing + (spacing - barWidth) / 2
            val y = height - barHeight

            drawRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )

            // Draw Value Text
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    item.value.toInt().toString(),
                    x + barWidth / 2,
                    y - 10f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }

            // Draw Label (Date or Label)
            item.date?.let { date ->
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        date.format(dateFormatter),
                        x + barWidth / 2,
                        height + 30f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            } ?: run {
                drawContext.canvas.nativeCanvas.apply {
                     drawText(
                        item.label.take(3),
                        x + barWidth / 2,
                        height + 30f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
        
        // Draw baseline
        drawLine(
            color = textColor.copy(alpha = 0.5f),
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 2f
        )
    }
}
