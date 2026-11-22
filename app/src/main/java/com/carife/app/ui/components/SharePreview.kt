package com.carife.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun SharePreview(message: String) {
    Text("Pratinjau pesan", style = MaterialTheme.typography.labelMedium)
    Text(message, style = MaterialTheme.typography.bodySmall)
}
