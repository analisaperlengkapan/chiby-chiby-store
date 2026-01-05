package com.chibychibystore.ui.components.special

/**
 * Data model for Chart visualization
 */
data class ChartData(
    val label: String,
    val value: Float,
    val date: java.time.LocalDate? = null
)
