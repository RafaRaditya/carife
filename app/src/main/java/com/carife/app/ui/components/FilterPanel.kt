package com.carife.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.ExploreFilters
import com.carife.app.core.model.NoiseLevel
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterPanel(
    filters: ExploreFilters,
    candidates: List<Cafe>,
    resultCount: Int,
    onChange: (ExploreFilters) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableAmenities = remember(candidates) {
        Amenity.entries.filter { amenity -> candidates.any { it.hasAmenity(amenity) } }.toSet()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Filter nugas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "$resultCount kafe cocok dengan kriteria ini",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onReset,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("Reset semua", style = MaterialTheme.typography.labelMedium)
            }
        }

        FilterSection("Budget") {
            BudgetBucket.entries.filter { it != BudgetBucket.ANY }.forEach { bucket ->
                CarifeChip(
                    label = bucket.label,
                    selected = filters.budget == bucket,
                    onClick = { onChange(filters.copy(budget = if (filters.budget == bucket) null else bucket)) },
                )
            }
        }

        FilterSection("Koneksi internet") {
            WifiRequirement.entries.filter { it != WifiRequirement.ANY }.forEach { requirement ->
                CarifeChip(
                    label = requirement.label,
                    selected = filters.wifi == requirement,
                    onClick = {
                        onChange(filters.copy(wifi = if (filters.wifi == requirement) null else requirement))
                    },
                )
            }
        }

        FilterSection("Zonasi asap rokok") {
            ZonePreference.entries.filter { it != ZonePreference.ANY }.forEach { zone ->
                CarifeChip(
                    label = zone.label,
                    selected = filters.zone == zone,
                    onClick = { onChange(filters.copy(zone = if (filters.zone == zone) null else zone)) },
                )
            }
        }

        FilterSection("Suasana & colokan") {
            NoiseLevel.entries.forEach { noise ->
                CarifeChip(
                    label = noise.label,
                    selected = filters.noise == noise,
                    onClick = { onChange(filters.copy(noise = if (filters.noise == noise) null else noise)) },
                )
            }
            CarifeChip(
                label = "Banyak colokan (8+)",
                selected = filters.minOutlets != null,
                onClick = { onChange(filters.copy(minOutlets = if (filters.minOutlets != null) null else 8)) },
            )
            CarifeChip(
                label = "< Rp30rb",
                selected = filters.priceCap != null,
                onClick = { onChange(filters.copy(priceCap = if (filters.priceCap != null) null else 30_000)) },
            )
        }

        FilterSection("Fasilitas") {
            Amenity.entries.forEach { amenity ->
                CarifeChip(
                    label = amenity.label,
                    selected = filters.amenities.contains(amenity),
                    enabled = availableAmenities.contains(amenity) || filters.amenities.contains(amenity),
                    onClick = {
                        val updated = if (filters.amenities.contains(amenity)) {
                            filters.amenities - amenity
                        } else {
                            filters.amenities + amenity
                        }
                        onChange(filters.copy(amenities = updated))
                    },
                )
            }
        }

        FilterSection("Jarak") {
            RadiusPreference.entries.filter { it != RadiusPreference.ANY }.forEach { radius ->
                CarifeChip(
                    label = radius.label,
                    selected = filters.distance == radius,
                    onClick = { onChange(filters.copy(distance = if (filters.distance == radius) null else radius)) },
                )
            }
        }

        FilterSection("Status") {
            CarifeChip(
                label = "Buka sekarang",
                selected = filters.openNowOnly,
                onClick = { onChange(filters.copy(openNowOnly = !filters.openNowOnly)) },
            )
        }

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            Text("Tampilkan $resultCount kafe")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}
