package com.fabbixmb.app.domain.model

import androidx.compose.ui.graphics.Color
import com.fabbixmb.app.theme.*

enum class Severity(val id: Int, val label: String, val color: Color) {
    NOT_CLASSIFIED(0, "Not classified", SeverityNotClassified),
    INFO(1, "Information", SeverityInfo),
    WARNING(2, "Warning", SeverityWarning),
    AVERAGE(3, "Average", SeverityAverage),
    HIGH(4, "High", SeverityHigh),
    DISASTER(5, "Disaster", SeverityDisaster);

    companion object {
        fun fromId(id: Int): Severity = entries.firstOrNull { it.id == id } ?: NOT_CLASSIFIED
    }
}