package com.carife.app.ui.profile

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.UserPreferences
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import com.carife.app.data.DemoFriendsData
import com.carife.app.data.FriendSpotResolved
import com.carife.app.domain.ShareTextBuilder
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CarifeChip
import com.carife.app.ui.detail.share
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: CarifeViewModel,
    onOpenCafe: (String) -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var draft by remember(appState.hasSavedPreferences, appState.preferences) {
        mutableStateOf(appState.preferences)
    }
    var showShareDialog by remember { mutableStateOf(false) }

    val friendSpots = remember(catalog.cafes) {
        DemoFriendsData.resolve(DemoFriendsData.spots, catalog.cafes)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ProfileHeader() }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard("14", "Spot Nugas", Modifier.weight(1f))
                StatCard("89%", "Fokus Match", Modifier.weight(1f))
                StatCard("18", "Teman Nugas", Modifier.weight(1f))
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Teman Nongkrong",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Konten demo: teman yang membagikan lokasi kafe hari ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (friendSpots.isEmpty()) {
            item {
                Text(
                    "Belum ada teman singgah di kota ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            items(friendSpots.size) { index ->
                val resolved = friendSpots[index]
                FriendSpotCard(
                    resolved = resolved,
                    onOpenCafe = onOpenCafe,
                )
            }
        }

        item {
            Button(
                onClick = { showShareDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Text("Bagikan Tempat Nongkrong Saya Sekarang")
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Atur Kriteria Nugas Kamu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Preferensi memengaruhi skor Carife Match di Home, bukan menyaring hasil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                PreferenceSection("Budget") {
                    BudgetBucket.entries.filter { it != BudgetBucket.ANY }.forEach { bucket ->
                        CarifeChip(
                            label = bucket.label,
                            selected = draft.budget == bucket,
                            onClick = { draft = draft.copy(budget = if (draft.budget == bucket) BudgetBucket.ANY else bucket) },
                        )
                    }
                }

                PreferenceSection("Kebutuhan Wi-Fi") {
                    listOf(WifiRequirement.MIN_30, WifiRequirement.STANDARD, WifiRequirement.ANY).forEach { requirement ->
                        CarifeChip(
                            label = requirement.label,
                            selected = draft.wifi == requirement,
                            onClick = { draft = draft.copy(wifi = requirement) },
                        )
                    }
                }

                PreferenceSection("Ruangan & Rokok") {
                    listOf(ZonePreference.NON_SMOKING_AC, ZonePreference.SEPARATE, ZonePreference.ANY).forEach { zone ->
                        CarifeChip(
                            label = zone.label,
                            selected = draft.zone == zone,
                            onClick = { draft = draft.copy(zone = zone) },
                        )
                    }
                }

                PreferenceSection("Fasilitas Wajib") {
                    Amenity.entries.forEach { amenity ->
                        CarifeChip(
                            label = amenity.label,
                            selected = draft.amenities.contains(amenity),
                            onClick = {
                                draft = draft.copy(
                                    amenities = if (draft.amenities.contains(amenity)) {
                                        draft.amenities - amenity
                                    } else {
                                        draft.amenities + amenity
                                    }
                                )
                            },
                        )
                    }
                }

                PreferenceSection("Radius") {
                    RadiusPreference.entries.filter { it != RadiusPreference.ANY }.forEach { radius ->
                        CarifeChip(
                            label = radius.label,
                            selected = draft.radius == radius,
                            onClick = { draft = draft.copy(radius = if (draft.radius == radius) RadiusPreference.ANY else radius) },
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.savePreferences(draft)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Simpan Preferensi & Update Skor Rekomendasi")
                }

                Text(
                    "Data lokasi dan kontak tidak diakses. Pilihan tempat nongkrong dipilih manual dari katalog.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showShareDialog) {
        ShareSpotDialog(
            cafes = catalog.cafes,
            onDismiss = { showShareDialog = false },
            onShare = { cafe, note, floor ->
                share(context, ShareTextBuilder.mySpotMessage(cafe, note, floor))
                showShareDialog = false
            },
        )
    }
}

@Composable
private fun ProfileHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = CarifeSurfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(CarifeSurfaceContainer),
                    contentAlignment = Alignment.Center,
                ) { Text("DP", fontWeight = FontWeight.Bold, color = CarifePrimary) }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Dimas Pratama",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Bekasi · Productive Workspace Seeker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(shape = RoundedCornerShape(999.dp), color = CarifeSecondaryContainer) {
                    Text(
                        "Data demo",
                        style = MaterialTheme.typography.labelSmall,
                        color = CarifeSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Surface(shape = RoundedCornerShape(999.dp), color = CarifeSurfaceContainer) {
                Text(
                    "Solo Deep Worker",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CarifeSurfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FriendSpotCard(resolved: FriendSpotResolved, onOpenCafe: (String) -> Unit) {
    val spot = resolved.spot
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = CarifeSurfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(spot.avatarColor)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(spot.initials, color = CarifeOnPrimary, style = MaterialTheme.typography.labelMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text(spot.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        text = resolved.cafe?.name ?: "Belum ada kafe demo di kota ini",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    spot.timeAgo,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text("\"${spot.note}\"", style = MaterialTheme.typography.bodySmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    spot.tags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(999.dp), color = CarifeSurfaceContainer) {
                            Text(
                                tag,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
                resolved.cafe?.let { cafe ->
                    TextButton(onClick = { onOpenCafe(cafe.id) }) {
                        Text(if (spot.groupProject) "Buka Kafe" else "Gabung")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun ShareSpotDialog(
    cafes: List<Cafe>,
    onDismiss: () -> Unit,
    onShare: (Cafe, String?, String?) -> Unit,
) {
    var selectedCafe by remember(cafes) { mutableStateOf(cafes.firstOrNull()) }
    var note by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bagikan Tempat Nongkrong") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Pilih kafe dari katalog. Lokasi tidak diambil otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(999.dp))
                        .background(CarifeSurfaceContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    BasicTextField(
                        value = selectedCafe?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                CafePicker(cafes, selectedCafe) { selectedCafe = it }
                FieldInput("Lantai (opsional)", floor) { floor = it }
                FieldInput("Catatan (opsional)", note) { note = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedCafe?.let { onShare(it, note.ifBlank { null }, floor.ifBlank { null }) } },
                enabled = selectedCafe != null,
            ) { Text("Bagikan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CafePicker(cafes: List<Cafe>, selected: Cafe?, onSelect: (Cafe) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        cafes.take(8).forEach { cafe ->
            Surface(
                onClick = { onSelect(cafe) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected?.id == cafe.id) CarifePrimary else CarifeSurfaceContainer,
            ) {
                Text(
                    text = cafe.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected?.id == cafe.id) CarifeOnPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FieldInput(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CarifeSurfaceContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    "Tulis di sini",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
