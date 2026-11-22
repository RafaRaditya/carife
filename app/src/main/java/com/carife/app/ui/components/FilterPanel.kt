package com.carife.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "Kriteria Spesifik Nugas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onReset) { Text("Reset Semua") }
        }

        FilterSection("BUDGET MINIMUM / MAKANAN") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BudgetBucket.entries.filter { it != BudgetBucket.ANY }.forEach { bucket ->
                    CarifeChip(
                        label = bucket.label,
                        selected = filters.budget == bucket,
                        onClick = { onChange(filters.copy(budget = if (filters.budget == bucket) null else bucket)) },
                    )
                }
            }
        }

        FilterSection("KONEKSI INTERNET") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        FilterSection("ZONASI ASAP ROKOK") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ZonePreference.entries.filter { it != ZonePreference.ANY }.forEach { zone ->
                    CarifeChip(
                        label = zone.label,
                        selected = filters.zone == zone,
                        onClick = { onChange(filters.copy(zone = if (filters.zone == zone) null else zone)) },
                    )
                }
            }
        }

        FilterSection("SUASANA & COLOKAN") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NoiseLevel.entries.forEach { noise ->
                    CarifeChip(
                        label = noise.label,
                        selected = filters.noise == noise,
                        onClick = { onChange(filters.copy(noise = if (filters.noise == noise) null else noise)) },
                    )
                }
                CarifeChip(
                    label = "Banyak Colokan (8+)",
                    selected = filters.minOutlets != null,
                    onClick = { onChange(filters.copy(minOutlets = if (filters.minOutlets != null) null else 8)) },
                )
                CarifeChip(
                    label = "< Rp30rb",
                    selected = filters.priceCap != null,
                    onClick = { onChange(filters.copy(priceCap = if (filters.priceCap != null) null else 30_000)) },
                )
            }
        }

        FilterSection("FASILITAS") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        FilterSection("JARAK") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RadiusPreference.entries.filter { it != RadiusPreference.ANY }.forEach { radius ->
                    CarifeChip(
                        label = radius.label,
                        selected = filters.distance == radius,
                        onClick = { onChange(filters.copy(distance = if (filters.distance == radius) null else radius)) },
                    )
                }
            }
        }

        FilterSection("LAINNYA") {
            CarifeChip(
                label = "Buka Sekarang",
                selected = filters.openNowOnly,
                onClick = { onChange(filters.copy(openNowOnly = !filters.openNowOnly)) },
            )
        }

        Text(
            text = "$resultCount kafe cocok dengan kriteria ini",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Tampilkan $resultCount Kafe")
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}
