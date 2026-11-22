package com.carife.app.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Cafe
import com.carife.app.domain.DistanceUtil
import com.carife.app.domain.OpeningHours
import com.carife.app.domain.ShareTextBuilder
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CafePhotoPlaceholder
import com.carife.app.ui.components.CarifeEmptyState
import com.carife.app.ui.components.DemoBadge
import com.carife.app.ui.components.formatRupiahFull
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import com.carife.app.ui.theme.CarifeTertiary
import java.time.Instant

private const val NO_DATA = "Belum ada data"

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
                Text("Cafe Details", style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CarifeEmptyState(
                    title = "Kafe tidak ditemukan",
                    message = if (catalog.isRefreshing) {
                        "Sedang memuat data kafe…"
                    } else {
                        "Data kafe ini tidak ada di katalog kota yang dipilih."
                    },
                    primaryLabel = "Kembali",
                    onPrimary = onBack,
                )
            }
        }
        return
    }

    val isFavorite = appState.favorites.contains(cafe.id)
    val isComparing = compareIds.contains(cafe.id)
    val openNow = OpeningHours.isOpenNow(cafe.openingHoursRaw, Instant.now())
    var showInviteDialog by remember(cafe.id) { mutableStateOf(false) }

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
                            IconButton(onClick = {
                                share(context, ShareTextBuilder.shareMessage(cafe))
                            }) {
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
                        text = cafe.photoCount?.let { "$it Foto" } ?: "Foto belum tersedia dari OpenStreetMap",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(CarifeSurfaceContainerLowest.copy(alpha = 0.9f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                    if (cafe.isDemoData) DemoBadge()
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cafe.distanceMeters?.let { distance ->
                        InfoPill("${DistanceUtil.formatDistance(distance)} dari lokasimu")
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
                            text = cafe.densityPercent?.let { "$it% lebih ramai dari biasanya" }
                                ?: "Angka ini contoh, bukan pengukuran langsung.",
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
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ShareTextBuilder.directionsUrl(cafe)))
                        runCatching { context.startActivity(intent) }
                            .onFailure {
                                val text = ShareTextBuilder.directionsUrl(cafe)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Alamat ${cafe.name}", text))
                                viewModel.emitMessage("Tidak ada aplikasi peta. Alamat disalin.")
                            }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) { Text("Rute", style = MaterialTheme.typography.labelMedium, maxLines = 1) }

                OutlinedButton(
                    onClick = {
                        viewModel.toggleCompare(cafe.id)
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Text(
                        if (isComparing) "Dipilih" else "Banding",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }

                OutlinedButton(
                    onClick = {
                        showInviteDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Text("Ajak", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }

        item {
            DetailSection("Informasi Kafe") {
                DetailRow("Alamat", cafe.address ?: NO_DATA)
                DetailRow("Area", cafe.area ?: NO_DATA)
                DetailRow("Jam operasional", cafe.openingHoursRaw ?: NO_DATA)
                DetailRow("Wi-Fi", cafe.wifiMbps?.let { "$it Mbps" } ?: NO_DATA)
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
                DetailSection("Ulasan Contoh") {
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
                Text(
                    text = "Nama, alamat, dan jam berasal dari OpenStreetMap. " +
                        "Harga, colokan, rating, kepadatan, menu, dan ulasan adalah data contoh.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenCompare) { Text("Lihat perbandingan kafe") }
            }
        }
    }

    if (showInviteDialog) {
        InviteDialog(
            cafe = cafe,
            onDismiss = { showInviteDialog = false },
            onShare = { note, floor ->
                share(context, ShareTextBuilder.inviteMessage(cafe, note, floor))
                showInviteDialog = false
            },
        )
    }
}

@Composable
private fun InviteDialog(
    cafe: Cafe,
    onDismiss: () -> Unit,
    onShare: (note: String?, floor: String?) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajak Teman Nugas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(cafe.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Pesan ini dibuka di menu berbagi Android setelah kamu setuju.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Pratinjau:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    ShareTextBuilder.inviteMessage(
                        cafe,
                        note.ifBlank { null },
                        floor.ifBlank { null },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onShare(note.ifBlank { null }, floor.ifBlank { null }) }) {
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

internal fun share(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Bagikan lewat"))
}
