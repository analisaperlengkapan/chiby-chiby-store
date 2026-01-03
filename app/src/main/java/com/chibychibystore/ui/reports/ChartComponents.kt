package com.chibychibystore.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Metric Card Component
 * Displays key performance indicators with title and value
 */
@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Bar Chart Component
 * Displays data as vertical bars.
 * Used in Dashboard and Reports screens.
 */
@Composable
fun BarChart(
    data: List<Pair<String, Float>>,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (data.isEmpty()) {
                Text(
                    text = "Tidak ada data untuk ditampilkan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center
                )
                return@Column
            }

            val maxValue = data.maxOfOrNull { it.second } ?: 1f
            val barColors = listOf(
                Color(0xFF2196F3), // Blue
                Color(0xFF4CAF50), // Green
                Color(0xFFFF9800), // Orange
                Color(0xFFE91E63), // Pink
                Color(0xFF9C27B0), // Purple
                Color(0xFF00BCD4), // Cyan
                Color(0xFF8BC34A), // Light Green
                Color(0xFFFF5722)  // Deep Orange
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val barWidth = canvasWidth / (data.size * 2f)
                val maxBarHeight = canvasHeight * 0.8f

                data.forEachIndexed { index, (label, value) ->
                    val barHeight = if (maxValue > 0) (value / maxValue) * maxBarHeight else 0f
                    val x = index * (barWidth * 2) + barWidth / 2
                    val y = canvasHeight - barHeight

                    // Draw bar
                    drawRect(
                        color = barColors[index % barColors.size],
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight)
                    )

                    // Draw value on top of bar
                    if (barHeight > 30f) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "%.0f".format(value),
                            x + barWidth / 2,
                            y - 5f,
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 24f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }

            // Legend
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(data) { (label, value) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(barColors[data.indexOfFirst { it.first == label } % barColors.size])
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$label: ${"%.0f".format(value)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Line Chart Component
 * Displays data as connected line points
 */
@Composable
fun LineChart(
    data: List<Pair<String, Float>>,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (data.isEmpty()) {
                Text(
                    text = "Tidak ada data untuk ditampilkan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center
                )
                return@Column
            }

            val maxValue = data.maxOfOrNull { it.second } ?: 1f

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val pointRadius = 6f
                val lineColor = Color(0xFF2196F3)

                val points = data.mapIndexed { index, (_, value) ->
                    val x = (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * canvasWidth
                    val y = canvasHeight - ((value / maxValue) * (canvasHeight * 0.8f))
                    Offset(x, y)
                }

                // Draw line
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = lineColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 3f
                    )
                }

                // Draw points
                points.forEach { point ->
                    drawCircle(
                        color = lineColor,
                        radius = pointRadius,
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = pointRadius - 2f,
                        center = point
                    )
                }

                // Draw grid lines
                val gridColor = Color.LightGray.copy(alpha = 0.3f)
                for (i in 0..4) {
                    val y = (canvasHeight * 0.8f) * (i.toFloat() / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, canvasHeight - y),
                        end = Offset(canvasWidth, canvasHeight - y),
                        strokeWidth = 1f
                    )
                }
            }

            // X-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                data.forEach { (label, _) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Pie Chart Component
 * Displays data as pie slices
 */
@Composable
fun PieChart(
    data: List<Pair<String, Float>>,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (data.isEmpty()) {
                Text(
                    text = "Tidak ada data untuk ditampilkan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp),
                    textAlign = TextAlign.Center
                )
                return@Column
            }

            val total = data.sumOf { it.second.toDouble() }.toFloat()
            val sliceColors = listOf(
                Color(0xFF2196F3), // Blue
                Color(0xFF4CAF50), // Green
                Color(0xFFFF9800), // Orange
                Color(0xFFE91E63), // Pink
                Color(0xFF9C27B0), // Purple
                Color(0xFF00BCD4), // Cyan
                Color(0xFF8BC34A), // Light Green
                Color(0xFFFF5722)  // Deep Orange
            )

            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .padding(16.dp)
            ) {
                val canvasSize = size.minDimension
                var startAngle = -90f

                data.forEachIndexed { index, (_, value) ->
                    val sweepAngle = (value / total) * 360f

                    drawArc(
                        color = sliceColors[index % sliceColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        size = Size(canvasSize, canvasSize)
                    )

                    startAngle += sweepAngle
                }
            }

            // Legend
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                data.forEachIndexed { index, (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(sliceColors[index % sliceColors.size])
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "${"%.1f".format((value / total) * 100)}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}