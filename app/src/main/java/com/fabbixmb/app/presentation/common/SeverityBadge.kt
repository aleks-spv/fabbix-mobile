package com.fabbixmb.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fabbixmb.app.domain.model.Severity

@Composable
fun SeverityBadge(severity: Severity, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(severity.color)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = severity.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}