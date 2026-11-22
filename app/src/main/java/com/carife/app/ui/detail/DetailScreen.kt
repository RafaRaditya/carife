package com.carife.app.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Cafe
import com.carife.app.domain.DistanceUtil
import com.carife.app.domain.OpeningHours
import com.carife.app.domain.ShareTextBuilder
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CafePhotoPlaceholder
import com.carife.app.ui.components.CarifeEmptyState
import com.carife.app.ui.components.SharePreview
import com.carife.app.ui.components.formatRupiahFull
import com.carife.app.ui.components.openCafeDirections
import com.carife.app.ui.components.openShareSheet
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import com.carife.app.ui.theme.CarifeTertiary

private const val NO_DATA = "Belum ada data"

private enum class ShareAction { CAFE, INVITE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    viewModel: CarifeViewModel,
    cafeId: String,
    onBack: () -> Unit,
    onOpenCompare: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val compareIds by viewModel.compareIds.collectAsStateWithLifecycle()
    val now by viewModel.openingHoursTime.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val cafe = catalog.cafes.firstOrNull { it.id == cafeId }

    if (cafe == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                }
                Text("Detail Kafe", style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (catalog.city == null || catalog.isRefreshing) {
                    CircularProgressIndicator()
                } else {
                    CarifeEmptyState(
                        title = "Kafe tidak ditemukan",
                        message = "Data kafe ini tidak ada di katalog kota yang dipilih.",
                        primaryLabel = "Kembali",
                        onPrimary = onBack,
                    )
                }
            }
        }
        return
    }

    val isFavorite = appState.favorites.contains(cafe.id)
    val isComparing = compareIds.contains(cafe.id)
    val openNow = OpeningHours.isOpenNow(cafe, now)
    var shareAction by rememberSaveable(cafe.id) { mutableStateOf<ShareAction?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Box {
                CafePhotoPlaceholder(
                    seed = cafe.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    cornerRadius = 0.dp,
                    iconSize = 48.dp,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = CarifeSurfaceContainerLowest.copy(alpha = 0.94f),
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = CarifeSurfaceContainerLowest.copy(alpha = 0.94f),
                        ) {
                            IconButton(onClick = { shareAction = ShareAction.CAFE }) {
                                Icon(Icons.Filled.Share, contentDescription = "Bagikan kafe ini")
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = CarifeSurfaceContainerLowest.copy(alpha = 0.94f),
                        ) {
                            IconButton(onClick = { viewModel.toggleFavorite(cafe.id) }) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isFavorite) "Hapus dari favorit" else "Simpan ke favorit",
                                    tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Foto belum tersedia",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(CarifeSurfaceContainerLowest.copy(alpha = 0.9f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    cafe.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = listOfNotNull(cafe.area, cafe.address).joinToString(" · ").ifBlank { NO_DATA },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    cafe.distanceMeters?.let { distance ->
                        val reference = if (appState.userLocation != null) "lokasimu" else "pusat kota"
                        InfoPill("${DistanceUtil.formatDistance(distance)} dari $reference")
                    }
                    openNow?.let { isOpen ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isOpen) CarifeSecondaryContainer else CarifeSurfaceContainer,
                        ) {
                            Text(
                                text = if (isOpen) "Buka sekarang" else "Tutup",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOpen) CarifeSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
                    }
                }
                if (cafe.priceMin != null && cafe.priceMax != null) {
                    InfoPill("${formatRupiahFull(cafe.priceMin)} – ${formatRupiahFull(cafe.priceMax)}")
                }
                if (cafe.rating != null) {
                    Text(
                        text = "★ %.1f".format(cafe.rating) +
                            (cafe.reviewCount?.let { " · $it ulasan" } ?: ""),
                        style = MaterialTheme.typography.labelMedium,
                        color = CarifeTertiary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (cafe.densityLevel != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = CarifeSurfaceContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "Simulasi kepadatan · ${cafe.densityLevel.label}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = cafe.densityPercent?.let { "$it% perkiraan keterisian" }
                                ?: "Perkiraan keterisian belum tersedia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { openCafeDirections(context, cafe, viewModel::emitMessage) },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) { Text("Rute", style = MaterialTheme.typography.labelMedium) }

                OutlinedButton(
                    onClick = {
                        viewModel.toggleCompare(cafe.id)
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Text(
                        if (isComparing) "Dipilih" else "Bandingkan",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                    )
                }

                OutlinedButton(
                    onClick = {
                        shareAction = ShareAction.INVITE
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Text("Ajak Teman", style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                }
            }
        }

        item {
            DetailSection("Informasi Kafe") {
                DetailRow("Alamat", cafe.address ?: NO_DATA)
                DetailRow("Area", cafe.area ?: NO_DATA)
                DetailRow("Jam operasional", cafe.openingHoursRaw ?: NO_DATA)
                DetailRow(
                    "Wi-Fi",
                    cafe.wifiMbps?.let { "$it Mbps" }
                        ?: if (cafe.hasWifi) "Ada Wi-Fi" else NO_DATA,
                )
                DetailRow(
                    "Colokan",
                    cafe.outletCount?.let { "$it colokan" } ?: cafe.outlets ?: NO_DATA,
                )
                DetailRow("Suasana", cafe.noiseLevel?.label ?: NO_DATA)
                DetailRow("Kepadatan", cafe.densityLevel?.label ?: NO_DATA)
                DetailRow("Zonasi rokok", cafe.smokingZone?.label ?: NO_DATA)
                DetailRow("Rating", cafe.rating?.let { "%.1f (%d ulasan)".format(it, cafe.reviewCount ?: 0) } ?: NO_DATA)
                DetailRow(
                    "Rentang harga",
                    if (cafe.priceMin != null && cafe.priceMax != null) {
                        "${formatRupiahFull(cafe.priceMin)} – ${formatRupiahFull(cafe.priceMax)}"
                    } else {
                        NO_DATA
                    },
                )
                DetailRow("Website", cafe.website ?: NO_DATA)
            }
        }

        if (cafe.amenities.isNotEmpty()) {
            item {
                DetailSection("Fasilitas") {
                    Text(
                        cafe.amenities.joinToString(" · ") { it.label },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (cafe.menu.isNotEmpty()) {
            item {
                DetailSection("Semua Menu") {
                    cafe.menu.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatRupiahFull(item.price),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (cafe.reviews.isNotEmpty()) {
            item {
                DetailSection("Ulasan") {
                    cafe.reviews.forEach { review ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    review.author,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "★ ${review.rating} · ${review.daysAgo} hari lalu",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "\"${review.text}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (cafe.id.startsWith("osm:")) {
                    Text(
                        text = "Data lokasi dari OpenStreetMap.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onOpenCompare) { Text("Lihat perbandingan kafe") }
            }
        }
    }

    shareAction?.let { action ->
        CafeShareDialog(
            cafe = cafe,
            action = action,
            onDismiss = { shareAction = null },
            onShare = { message ->
                if (openShareSheet(context, message, viewModel::emitMessage)) shareAction = null
            },
        )
    }
}
@Composable
private fun CafeShareDialog(
    cafe: Cafe,
    action: ShareAction,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
) {
    var note by rememberSaveable { mutableStateOf("") }
    var floor by rememberSaveable { mutableStateOf("") }
    val isInvite = action == ShareAction.INVITE
    val message = if (isInvite) {
        ShareTextBuilder.inviteMessage(cafe, note.trim(), floor.trim())
    } else {
        ShareTextBuilder.shareMessage(cafe)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isInvite) "Ajak Teman Nugas" else "Bagikan Kafe") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isInvite) {
                    OutlinedTextField(
                        value = floor,
                        onValueChange = { floor = it },
                        label = { Text("Lantai / meja (opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan (opsional)") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SharePreview(message)
            }
        },
        confirmButton = {
            TextButton(onClick = { onShare(message) }) {
                Text("Bagikan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}

@Composable
private fun InfoPill(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = CarifeSurfaceContainer) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = CarifeSurfaceContainerLowest,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.4f),
        )
    }
}
