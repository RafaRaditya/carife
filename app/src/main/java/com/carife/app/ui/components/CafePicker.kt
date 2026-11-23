package com.carife.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.Cafe
import com.carife.app.domain.DistanceUtil
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifePrimaryFixed
import com.carife.app.ui.theme.CarifeSurfaceContainerLow

@Composable
internal fun CafePicker(
    cafes: List<Cafe>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchable: Boolean = false,
    showDetails: Boolean = false,
    height: Dp = 150.dp,
) {
    var query by remember { mutableStateOf("") }
    val ordered = remember(cafes) { cafes.sortedBy { it.name.lowercase() } }
    val filtered = remember(ordered, query) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            ordered
        } else {
            ordered.filter { cafe ->
                cafe.name.contains(needle, ignoreCase = true) ||
                    cafe.area?.contains(needle, ignoreCase = true) == true ||
                    cafe.address?.contains(needle, ignoreCase = true) == true
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (searchable) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Cari kafe atau area") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = CarifePrimary,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Hapus pencarian",
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (filtered.isEmpty()) {
            Text(
                text = "Kafe tidak ditemukan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(filtered, key = { it.id }) { cafe ->
                    CafePickerRow(
                        cafe = cafe,
                        selected = selectedId == cafe.id,
                        showDetails = showDetails,
                        onSelect = { onSelect(cafe.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CafePickerRow(
    cafe: Cafe,
    selected: Boolean,
    showDetails: Boolean,
    onSelect: () -> Unit,
) {
    val description = listOfNotNull(cafe.area, cafe.address)
        .joinToString(" · ")
        .ifBlank { "Alamat belum tersedia" }
    val meta = buildList {
        cafe.distanceMeters?.let { add(DistanceUtil.formatDistance(it)) }
        if (cafe.priceMin != null && cafe.priceMax != null) {
            add("${formatRupiah(cafe.priceMin)}–${formatRupiah(cafe.priceMax)}")
        } else {
            cafe.priceMin?.let { add("${formatRupiah(it)}+") }
        }
        cafe.wifiMbps?.let { add("$it Mbps") }
    }.joinToString(" · ")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) CarifePrimaryFixed else CarifeSurfaceContainerLow,
        border = if (selected) {
            BorderStroke(1.dp, CarifePrimary.copy(alpha = 0.25f))
        } else {
            null
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .selectable(selected, role = Role.RadioButton, onClick = onSelect)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = cafe.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (showDetails) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Kafe terpilih",
                    tint = CarifePrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
