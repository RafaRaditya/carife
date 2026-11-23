package com.carife.app.ui.compare

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.Cafe
import com.carife.app.data.demo.DemoCafePhotos
import com.carife.app.domain.CompareAdvisor
import com.carife.app.domain.DistanceUtil
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CafePhotoPlaceholder
import com.carife.app.ui.components.CarifeHairline
import com.carife.app.ui.components.CarifeSectionHeader
import com.carife.app.ui.components.formatRupiahFull
import com.carife.app.ui.components.openCafeDirections
import com.carife.app.ui.theme.CarifeOutlineVariant
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifePrimaryFixed
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import com.carife.app.ui.theme.CarifeTertiary

private const val NO_DATA = "Belum ada data"

private val LabelWidth = 96.dp
private val CellWidth = 150.dp
private val MatrixGap = 8.dp

@Composable
fun CompareScreen(
    viewModel: CarifeViewModel,
    onOpenCafe: (String) -> Unit,
    onPickCafes: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val compareIds by viewModel.compareIds.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val selected = compareIds.mapNotNull { id -> catalog.cafes.firstOrNull { it.id == id } }

    if (selected.size < 2) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Bandingkan kafe",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = if (selected.isEmpty()) {
                    "Pilih 2–3 kafe dari Home atau Peta untuk melihat fasilitasnya berdampingan."
                } else {
                    "Kamu baru memilih 1 kafe. Tambah satu lagi untuk mulai membandingkan."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = onPickCafes,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) { Text("Pilih kafe") }
            if (selected.isNotEmpty()) {
                TextButton(onClick = viewModel::clearCompare) { Text("Kosongkan pilihan") }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                shape = RoundedCornerShape(16.dp),
                color = CarifePrimaryFixed,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Cara memilih",
                        style = MaterialTheme.typography.labelMedium,
                        color = CarifePrimary,
                    )
                    Text(
                        text = "Centang “Bandingkan” pada kartu kafe. Tiga pilihan pertama masuk ke tabel; pilihan keempat ditolak.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        return
    }

    val advice = CompareAdvisor.advise(selected, appState.preferences)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Bandingkan",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${selected.size} kafe dipilih · geser tabel ke samping",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(
                        onClick = onPickCafes,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    ) { Text("Ubah pilihan") }
                    TextButton(
                        onClick = viewModel::clearCompare,
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    ) { Text("Kosongkan") }
                }
            }
        }

        item {
            CompareMatrix(
                cafes = selected,
                advice = advice,
                onOpenCafe = onOpenCafe,
                onRemoveCafe = viewModel::toggleCompare,
            )
        }

        if (advice.summary != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = CarifePrimaryFixed,
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = "Saran Carife",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = CarifePrimary,
                        )
                        Text(advice.summary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CarifeSectionHeader(
                    title = "Rute cepat",
                    supporting = "Buka navigasi ke kafe yang dibandingkan",
                )
                selected.forEach { cafe ->
                    OutlinedButton(
                        onClick = { openCafeDirections(context, cafe, viewModel::emitMessage) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CarifeOutlineVariant),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NearMe,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = CarifePrimary,
                        )
                        Text(
                            text = "Rute ke ${cafe.name}",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareMatrix(
    cafes: List<Cafe>,
    advice: CompareAdvisor.Advice,
    onOpenCafe: (String) -> Unit,
    onRemoveCafe: (String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val tableWidth: Dp = 16.dp +
        LabelWidth +
        CellWidth * cafes.size +
        MatrixGap * cafes.size

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .horizontalScroll(scrollState),
    ) {
        Surface(
            modifier = Modifier.width(tableWidth),
            shape = RoundedCornerShape(18.dp),
            color = CarifeSurfaceContainerLowest,
            border = BorderStroke(1.dp, CarifeOutlineVariant),
        ) {
            Column {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(MatrixGap),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "Kafe",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .width(LabelWidth)
                            .padding(top = 8.dp, start = 2.dp),
                    )
                    cafes.forEach { cafe ->
                        CafeHeaderCell(
                            cafe = cafe,
                            isBestForStudy = advice.bestForStudyId == cafe.id,
                            isCheapest = advice.cheapestId == cafe.id,
                            onOpen = { onOpenCafe(cafe.id) },
                            onRemove = { onRemoveCafe(cafe.id) },
                        )
                    }
                }

                CarifeHairline(Modifier.padding(horizontal = 8.dp))

                CompareRow("Budget", cafes) { cafe ->
                    if (cafe.priceMin != null && cafe.priceMax != null) {
                        "${formatRupiahFull(cafe.priceMin)}–${formatRupiahFull(cafe.priceMax)}"
                    } else {
                        NO_DATA
                    }
                }
                CarifeHairline(Modifier.padding(horizontal = 8.dp))
                CompareRow("Jarak", cafes) { cafe ->
                    cafe.distanceMeters?.let { DistanceUtil.formatDistance(it) } ?: NO_DATA
                }
                CarifeHairline(Modifier.padding(horizontal = 8.dp))
                CompareRow("Wi-Fi", cafes) { cafe ->
                    cafe.wifiMbps?.let { "$it Mbps" }
                        ?: if (cafe.hasWifi) "Ada Wi-Fi" else NO_DATA
                }
                CarifeHairline(Modifier.padding(horizontal = 8.dp))
                CompareRow("Colokan", cafes) { cafe ->
                    cafe.outletCount?.let { "$it colokan" } ?: cafe.outlets ?: NO_DATA
                }
                CarifeHairline(Modifier.padding(horizontal = 8.dp))
                CompareRow("Kepadatan", cafes) { cafe ->
                    cafe.densityLevel?.let { level ->
                        level.label + (cafe.densityPercent?.let { " · $it%" } ?: "")
                    } ?: NO_DATA
                }
                CarifeHairline(Modifier.padding(horizontal = 8.dp))
                CompareRow("Suasana", cafes) { cafe ->
                    cafe.noiseLevel?.label ?: NO_DATA
                }
                CarifeHairline(Modifier.padding(horizontal = 8.dp))
                CompareRow("Zonasi rokok", cafes) { cafe ->
                    cafe.smokingZone?.label ?: NO_DATA
                }
                CarifeHairline(Modifier.padding(horizontal = 8.dp))
                CompareRow("Jam buka", cafes) { cafe ->
                    cafe.openingHoursRaw ?: NO_DATA
                }
                CarifeHairline(Modifier.padding(horizontal = 8.dp))
                CompareRow("Fasilitas", cafes) { cafe ->
                    cafe.amenities.takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") { it.label }
                        ?: NO_DATA
                }
                CarifeHairline(Modifier.padding(horizontal = 8.dp))
                CompareRow("Menu", cafes) { cafe ->
                    cafe.menu.take(3).takeIf { it.isNotEmpty() }
                        ?.joinToString(", ") { it.name }
                        ?: NO_DATA
                }
            }
        }
    }
}

@Composable
private fun CafeHeaderCell(
    cafe: Cafe,
    isBestForStudy: Boolean,
    isCheapest: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier.width(CellWidth),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(
            modifier = Modifier.height(32.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            if (isBestForStudy) {
                Text(
                    text = "Cocok buat nugas",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CarifeSecondary,
                )
            }
            if (isCheapest) {
                Text(
                    text = "Paling hemat",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CarifeTertiary,
                )
            }
        }
        CafePhotoPlaceholder(
            seed = cafe.id,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            cornerRadius = 12.dp,
            photoAsset = DemoCafePhotos.assetFor(cafe),
        )
        Text(
            text = cafe.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row {
            TextButton(
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                Text("Detail", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(
                onClick = onRemove,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Hapus ${cafe.name} dari perbandingan" },
                contentPadding = PaddingValues(horizontal = 0.dp),
            ) {
                Text(
                    "Hapus",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CompareRow(
    label: String,
    cafes: List<Cafe>,
    value: (Cafe) -> String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(MatrixGap),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(LabelWidth)
                .padding(top = 8.dp),
        )
        cafes.forEach { cafe ->
            Text(
                text = value(cafe),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .width(CellWidth)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}
