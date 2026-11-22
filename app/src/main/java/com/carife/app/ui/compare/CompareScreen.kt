package com.carife.app.ui.compare

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.Cafe
import com.carife.app.domain.CompareAdvisor
import com.carife.app.domain.DistanceUtil
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CafePhotoPlaceholder
import com.carife.app.ui.components.formatRupiahFull
import com.carife.app.ui.components.openCafeDirections
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest

private const val NO_DATA = "Belum ada data"

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Perbandingan Kafe",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (selected.isEmpty()) {
                        "Belum ada kafe dipilih. Pilih 2 sampai 3 kafe untuk dibandingkan."
                    } else {
                        "Kamu baru memilih 1 kafe. Tambah satu lagi untuk membandingkan."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onPickCafes) { Text("Pilih Kafe") }
                if (selected.isNotEmpty()) {
                    TextButton(onClick = viewModel::clearCompare) { Text("Kosongkan pilihan") }
                }
                Text(
                    "Maksimal 3 kafe dalam satu perbandingan.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val advice = CompareAdvisor.advise(selected, appState.preferences)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Perbandingan Kafe",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Membandingkan ${selected.size} kafe",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextButton(onClick = onPickCafes) { Text("Ubah Pilihan") }
                    TextButton(onClick = viewModel::clearCompare) { Text("Kosongkan") }
                }
            }
        }

        item {
            CompareMatrix(selected, advice, onOpenCafe, onRemoveCafe = viewModel::toggleCompare)
        }

        if (advice.summary != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = CarifeSurfaceContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "Saran Carife",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selected.forEach { cafe ->
                    Button(
                        onClick = { openCafeDirections(context, cafe, viewModel::emitMessage) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Rute ke ${cafe.name}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(112.dp))
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

        CompareRow("ESTIMASI BUDGET", cafes) { cafe ->
            if (cafe.priceMin != null && cafe.priceMax != null) {
                "${formatRupiahFull(cafe.priceMin)} – ${formatRupiahFull(cafe.priceMax)}"
            } else {
                NO_DATA
            }
        }

        CompareRow("JARAK", cafes) { cafe ->
            cafe.distanceMeters?.let { DistanceUtil.formatDistance(it) } ?: NO_DATA
        }

        CompareRow("KONEKSI WI-FI", cafes) { cafe ->
            cafe.wifiMbps?.let { "$it Mbps" }
                ?: if (cafe.amenities.contains(Amenity.WIFI)) "Ada Wi-Fi" else NO_DATA
        }

        CompareRow("COLOKAN", cafes) { cafe ->
            cafe.outletCount?.let { "$it colokan" } ?: cafe.outlets ?: NO_DATA
        }

        CompareRow("KEPADATAN", cafes) { cafe ->
            cafe.densityLevel?.let { density ->
                density.label + (cafe.densityPercent?.let { " · $it%" } ?: "")
            } ?: NO_DATA
        }

        CompareRow("SUASANA", cafes) { cafe ->
            cafe.noiseLevel?.label ?: NO_DATA
        }

        CompareRow("ZONASI & ROKOK", cafes) { cafe ->
            cafe.smokingZone?.label ?: NO_DATA
        }

        CompareRow("JAM OPERASIONAL", cafes) { cafe ->
            cafe.openingHoursRaw ?: NO_DATA
        }

        CompareRow("FASILITAS", cafes) { cafe ->
            cafe.amenities.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.label } ?: NO_DATA
        }

        CompareRow("MENU", cafes) { cafe ->
            cafe.menu.take(3).takeIf { it.isNotEmpty() }
                ?.joinToString(", ") { it.name }
                ?: NO_DATA
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
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CarifeSurfaceContainerLowest)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isBestForStudy || isCheapest) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isBestForStudy) CarifeSecondaryContainer else CarifeSurfaceContainer,
            ) {
                Text(
                    text = when {
                        isBestForStudy && isCheapest -> "Cocok Nugas & Hemat"
                        isBestForStudy -> "Cocok Buat Nugas"
                        else -> "Paling Hemat"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isBestForStudy) CarifeSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        CafePhotoPlaceholder(
            seed = cafe.id,
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            cornerRadius = 14.dp,
        )

        Text(
            cafe.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Row {
            TextButton(onClick = onOpen, modifier = Modifier.weight(1f)) { Text("Detail") }
            TextButton(
                onClick = onRemove,
                modifier = Modifier.weight(1f).semantics {
                    contentDescription = "Hapus ${cafe.name} dari perbandingan"
                },
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) { Text("Hapus") }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(112.dp)
                .padding(top = 10.dp),
        )
        cafes.forEach { cafe ->
            Surface(
                modifier = Modifier.width(160.dp),
                shape = RoundedCornerShape(14.dp),
                color = CarifeSurfaceContainerLowest,
            ) {
                Text(
                    text = value(cafe),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }
    }
}
