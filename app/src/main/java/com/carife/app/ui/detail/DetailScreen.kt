package com.carife.app.ui.detail

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.DensityLevel
import com.carife.app.data.demo.DemoCafePhotos
import com.carife.app.domain.DistanceUtil
import com.carife.app.domain.OpeningHours
import com.carife.app.domain.ShareTextBuilder
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CafePhotoPlaceholder
import com.carife.app.ui.components.CarifeEmptyState
import com.carife.app.ui.components.CarifeHairline
import com.carife.app.ui.components.CarifeSectionHeader
import com.carife.app.ui.components.CarifeTag
import com.carife.app.ui.components.SharePreview
import com.carife.app.ui.components.formatRupiahFull
import com.carife.app.ui.components.openCafeDirections
import com.carife.app.ui.components.openShareSheet
import com.carife.app.ui.teman.CreateRoomDialog
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifeOutlineVariant
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeStar
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import com.carife.app.ui.theme.CarifeTertiary
import kotlin.math.roundToInt

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
                Text("Detail kafe", style = MaterialTheme.typography.titleMedium)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (catalog.city == null || catalog.isRefreshing) {
                    CircularProgressIndicator(color = CarifePrimary)
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
    var showCreateRoom by rememberSaveable(cafe.id) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            CafeHero(
                cafe = cafe,
                isFavorite = isFavorite,
                onBack = onBack,
                onToggleFavorite = { viewModel.toggleFavorite(cafe.id) },
                onShare = { shareAction = ShareAction.CAFE },
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = cafe.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = listOfNotNull(cafe.area, cafe.address).joinToString(" · ").ifBlank { NO_DATA },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    cafe.rating?.let { rating ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = CarifeStar,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "%.1f".format(rating) +
                                    (cafe.reviewCount?.let { " · $it ulasan" } ?: ""),
                                style = MaterialTheme.typography.labelMedium,
                                color = CarifeStar,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    openNow?.let { isOpen ->
                        Text(
                            text = if (isOpen) "Buka sekarang" else "Tutup",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isOpen) CarifeSecondary else CarifeTertiary,
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { showCreateRoom = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Buat room di kafe ini",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { openCafeDirections(context, cafe, viewModel::emitMessage) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, CarifeOutlineVariant),
                    ) {
                        Text("Rute", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { viewModel.toggleCompare(cafe.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, CarifeOutlineVariant),
                    ) {
                        Text(
                            text = if (isComparing) "Dipilih" else "Bandingkan",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(
                        onClick = { shareAction = ShareAction.INVITE },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        border = BorderStroke(1.dp, CarifeOutlineVariant),
                    ) {
                        Text(
                            text = "Ajak Teman",
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        item {
            FactSheet(cafe = cafe, openNow = openNow, modifier = Modifier.padding(horizontal = 20.dp))
        }

        if (cafe.densityLevel != null) {
            item {
                DensityCard(
                    percent = cafe.densityPercent,
                    level = cafe.densityLevel,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CarifeSectionHeader(title = "Fasilitas")
                if (cafe.amenities.isEmpty()) {
                    Text(
                        text = "Belum ada data fasilitas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        cafe.amenities.forEach { amenity ->
                            CarifeTag(amenity.label)
                        }
                    }
                }
            }
        }

        if (cafe.menu.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CarifeSectionHeader(title = "Menu", supporting = "${cafe.menu.size} item contoh")
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CarifeSurfaceContainerLowest,
                        border = BorderStroke(1.dp, CarifeOutlineVariant),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            cafe.menu.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = formatRupiahFull(item.price),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (index != cafe.menu.lastIndex) {
                                    CarifeHairline()
                                }
                            }
                        }
                    }
                }
            }
        }

        if (cafe.reviews.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CarifeSectionHeader(
                        title = "Ulasan",
                        supporting = "${cafe.reviews.size} ulasan contoh",
                    )
                    cafe.reviews.forEachIndexed { index, review ->
                        Column(
                            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = review.author,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = null,
                                        tint = CarifeStar,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    Text(
                                        text = "${review.rating} · ${review.daysAgo} hari lalu",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                text = "\"${review.text}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (index != cafe.reviews.lastIndex) {
                            CarifeHairline()
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CarifeSectionHeader(title = "Informasi lain")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CarifeSurfaceContainerLowest,
                    border = BorderStroke(1.dp, CarifeOutlineVariant),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                        DetailRow("Alamat", cafe.address ?: NO_DATA)
                        CarifeHairline()
                        DetailRow("Area", cafe.area ?: NO_DATA)
                        CarifeHairline()
                        DetailRow("Jam operasional", cafe.openingHoursRaw ?: NO_DATA)
                        CarifeHairline()
                        DetailRow("Suasana", cafe.noiseLevel?.label ?: NO_DATA)
                        CarifeHairline()
                        DetailRow(
                            "Rentang harga",
                            if (cafe.priceMin != null && cafe.priceMax != null) {
                                "${formatRupiahFull(cafe.priceMin)} – ${formatRupiahFull(cafe.priceMax)}"
                            } else {
                                NO_DATA
                            },
                        )
                        CarifeHairline()
                        DetailRow("Website", cafe.website ?: NO_DATA)
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
                TextButton(
                    onClick = onOpenCompare,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                ) {
                    Text("Lihat perbandingan kafe", color = CarifePrimary)
                }
            }
        }
    }

    if (showCreateRoom) {
        CreateRoomDialog(
            cafes = listOf(cafe),
            fixedCafe = cafe,
            onDismiss = { showCreateRoom = false },
            onCreate = { targetCafe, schedule ->
                viewModel.createRoom(
                    cafe = targetCafe,
                    title = "Nongkrong di ${targetCafe.name}",
                    schedule = schedule,
                    note = "",
                    invitedFriendIds = emptyList(),
                    capacity = 5,
                )
                showCreateRoom = false
            },
        )
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
private fun CafeHero(
    cafe: Cafe,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    Box {
        CafePhotoPlaceholder(
            seed = cafe.id,
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp),
            cornerRadius = 0.dp,
            iconSize = 44.dp,
            showLabel = true,
            photoAsset = DemoCafePhotos.assetFor(cafe),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HeroIconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroIconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Bagikan kafe ini",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                HeroIconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Hapus dari favorit" else "Simpan ke favorit",
                        tint = if (isFavorite) CarifePrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CarifeSurfaceContainerLowest.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, CarifeOutlineVariant),
    ) {
        IconButton(onClick = onClick) { content() }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FactSheet(cafe: Cafe, openNow: Boolean?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CarifeSurfaceContainerLowest,
        border = BorderStroke(1.dp, CarifeOutlineVariant),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FactCell(
                    label = "Wi-Fi",
                    value = cafe.wifiMbps?.let { "$it Mbps" }
                        ?: if (cafe.hasWifi) "Ada Wi-Fi" else NO_DATA,
                    modifier = Modifier.weight(1f),
                )
                FactDivider()
                FactCell(
                    label = "Colokan",
                    value = cafe.outletCount?.let { "$it colokan" } ?: cafe.outlets ?: NO_DATA,
                    modifier = Modifier.weight(1f),
                )
            }
            CarifeHairline()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FactCell(
                    label = "Jarak",
                    value = cafe.distanceMeters?.let(DistanceUtil::formatDistance) ?: NO_DATA,
                    modifier = Modifier.weight(1f),
                )
                FactDivider()
                FactCell(
                    label = "Zonasi",
                    value = cafe.smokingZone?.label ?: NO_DATA,
                    modifier = Modifier.weight(1f),
                )
            }
            CarifeHairline()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FactCell(
                    label = "Harga",
                    value = if (cafe.priceMin != null && cafe.priceMax != null) {
                        "${formatRupiahFull(cafe.priceMin)}–${formatRupiahFull(cafe.priceMax)}"
                    } else {
                        NO_DATA
                    },
                    modifier = Modifier.weight(1f),
                )
                FactDivider()
                FactCell(
                    label = "Jam",
                    value = when (openNow) {
                        true -> "Buka sekarang"
                        false -> "Tutup"
                        null -> NO_DATA
                    },
                    accent = when (openNow) {
                        true -> CarifeSecondary
                        false -> CarifeTertiary
                        null -> null
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FactCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = accent ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FactDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(40.dp)
            .background(CarifeOutlineVariant),
    )
}

@Composable
private fun DensityCard(
    percent: Int?,
    level: DensityLevel,
    modifier: Modifier = Modifier,
) {
    val value = percent ?: when (level) {
        DensityLevel.SEPI -> 30
        DensityLevel.SEDANG -> 60
        DensityLevel.PENUH -> 90
    }
    val filled = (value / 10f).roundToInt().coerceIn(0, 10)
    val tone = when (level) {
        DensityLevel.SEPI -> CarifeSecondary
        DensityLevel.SEDANG -> CarifeTertiary
        DensityLevel.PENUH -> MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CarifeSurfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Simulasi kepadatan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = level.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = "$value%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = tone,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(10) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (index < filled) tone
                                else CarifeOutlineVariant.copy(alpha = 0.6f)
                            ),
                    )
                }
            }
            Text(
                text = "Perkiraan keterisian dari data contoh, bukan kepadatan langsung.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.35f),
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
        title = { Text(if (isInvite) "Ajak teman nugas" else "Bagikan kafe") },
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan (opsional)") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SharePreview(message)
            }
        },
        confirmButton = {
            TextButton(onClick = { onShare(message) }) { Text("Bagikan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        },
    )
}
