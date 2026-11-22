package com.carife.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.Cafe
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSurfaceContainer

@Composable
internal fun CafePicker(
    cafes: List<Cafe>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    val ordered = remember(cafes) { cafes.sortedBy { it.name.lowercase() } }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(ordered, key = { it.id }) { cafe ->
            val selected = selectedId == cafe.id
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) CarifePrimary else CarifeSurfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .selectable(selected, role = Role.RadioButton, onClick = { onSelect(cafe.id) })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = cafe.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) CarifeOnPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
