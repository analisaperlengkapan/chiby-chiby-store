package com.chibychibystore.ui.components.special

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chibychibystore.data.local.entity.ExpenseCategory

@Composable
fun BarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    // Map to ChartData
    val chartData = data.map { (label, value) ->
        ChartData(
            label = label,
            value = value.toFloat(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
        )
    }
    
    ChartView(
        title = "Bar Chart", // Default title or add parameter
        data = chartData,
        modifier = modifier
    )
}

@Composable
fun LineChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
     // Reuse ChartView definition as BarChart for now (LineChart logic is different but for build success we use what we have)
     // Ideally implement LineChart using Canvas, but we use ChartView for now
     val chartData = data.map { (label, value) ->
        ChartData(
            label = label,
            value = value.toFloat(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.secondary
        )
    }
    
    ChartView(
        title = "Line Chart",
        data = chartData,
        modifier = modifier
    )
}

@Composable
fun PieChart(
    title: String,
    data: Map<ExpenseCategory, Double>,
    modifier: Modifier = Modifier
) {
    // PieChart implementation or placeholder
    // We map types to ChartData to reuse ChartView logic (showing bars instead of pie for now)
    // To properly implement PieChart, we need arc drawing.
    // For compilation fix, we use ChartView.
    
    val chartData = data.entries.map { (category, amount) ->
        ChartData(
            label = category.displayName,
            value = amount.toFloat(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
        )
    }
    
    ChartView(
        title = title,
        data = chartData,
        modifier = modifier
    )
}
