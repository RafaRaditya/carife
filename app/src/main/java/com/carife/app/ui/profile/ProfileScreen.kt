package com.carife.app.ui.profile

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.UserProfile
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import com.carife.app.data.DemoFriendsData
import com.carife.app.domain.MatchScorer
import com.carife.app.domain.ShareTextBuilder
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CafePicker
import com.carife.app.ui.components.CarifeChip
import com.carife.app.ui.components.ProfileAvatar
import com.carife.app.ui.components.SharePreview
import com.carife.app.ui.components.openShareSheet
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: CarifeViewModel,
    onOpenCafe: (String) -> Unit,
    onOpenAccountSettings: () -> Unit,
    onPreferencesSaved: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = appState.profile

    var draft by remember(appState.hasSavedPreferences, appState.preferences) {
        mutableStateOf(appState.preferences)
    }
    var showManageFriends by rememberSaveable { mutableStateOf(false) }
    var showCreateRoom by rememberSaveable { mutableStateOf(false) }
    var showShareDialog by rememberSaveable { mutableStateOf(false) }
    var savingPreferences by remember { mutableStateOf(false) }

    val friendPeople = remember(appState.friendIds) {
        DemoFriendsData.friends(appState.friendIds)
    }
    val friendSpots = remember(appState.friendIds, catalog.cafes) {
        DemoFriendsData.resolve(appState.friendIds, catalog.cafes)
    }
    val resolvedByFriend = remember(friendSpots) {
        friendSpots.associateBy { it.person.id }
    }
    val bestMatchScore = remember(catalog.cafes, appState.hasSavedPreferences, appState.preferences) {
        if (appState.hasSavedPreferences) {
            MatchScorer.bestMatch(catalog.cafes, appState.preferences)?.second
        } else {
            null
        }
    }
    val friendById = remember { DemoFriendsData.people.associateBy { it.id } }
    val roomViews = remember(
        appState.rooms,
        appState.friendIds,
        catalog.cafes,
        profile,
    ) {
        val userRooms = appState.rooms
            .sortedByDescending { it.createdAtEpochMillis }
            .map { room ->
                RoomView(
                    room = room,
                    cafe = DemoFriendsData.resolveRoomCafe(room, catalog.cafes),
                    hostName = "Kamu",
                    hostInitials = profile.initials,
                    hostColor = profile.avatarColor,
                    hostProfile = profile,
                    isHost = true,
                    invitedPeople = room.invitedFriendIds.mapNotNull(friendById::get),
                )
            }
        val demoRooms = DemoFriendsData.demoRooms
            .filter { room -> room.hostFriendId in appState.friendIds }
            .map { room ->
                val host = room.hostFriendId?.let(friendById::get)
                RoomView(
                    room = room,
                    cafe = DemoFriendsData.resolveRoomCafe(room, catalog.cafes),
                    hostName = host?.name ?: "Teman",
                    hostInitials = host?.initials ?: "?",
                    hostColor = host?.avatarColor ?: 0xFF45664B,
                    hostProfile = null,
                    isHost = false,
                    invitedPeople = room.invitedFriendIds.mapNotNull(friendById::get),
                )
            }
        userRooms + demoRooms
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            ProfileHeader(
                profile = profile,
                cityName = appState.selectedCity.name,
                onOpenAccountSettings = onOpenAccountSettings,
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(appState.favorites.size.toString(), "Spot Nugas", Modifier.weight(1f))
                StatCard(bestMatchScore?.let { "$it%" } ?: "—", "Fokus Match", Modifier.weight(1f))
                StatCard(appState.friendIds.size.toString(), "Teman Nugas", Modifier.weight(1f))
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GroupWork,
                        contentDescription = null,
                        tint = CarifeSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        "Teman Nongkrong",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    LiveDot()
                }
                Text(
                    "Konten demo: teman yang membagikan lokasi kafe hari ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PrivacyBadge()
            }
        }

        item {
            FriendListSection(
                people = friendPeople,
                resolvedByFriend = resolvedByFriend,
                joinedFriendIds = appState.joinedSpotFriendIds,
                onManageFriends = { showManageFriends = true },
                onOpenCafe = onOpenCafe,
            )
        }

        item {
            Text(
                text = "Aktivitas terbaru",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (friendSpots.isEmpty()) {
            item {
                Text(
                    text = "Belum ada aktivitas. Tambah teman lewat Kelola Teman.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        } else {
            items(friendSpots, key = { it.person.id }) { resolved ->
                FriendSpotCard(
                    resolved = resolved,
                    joined = resolved.person.id in appState.joinedSpotFriendIds,
                    onToggleJoin = { viewModel.toggleJoinSpot(resolved.person.id) },
                    onOpenCafe = onOpenCafe,
                )
            }
        }

        item {
            RoomsSection(
                rooms = roomViews,
                joinedRoomIds = appState.joinedRoomIds,
                onCreateRoom = { showCreateRoom = true },
                onJoinRoom = viewModel::toggleJoinRoom,
                onShareRoom = { view ->
                    openShareSheet(
                        context,
                        ShareTextBuilder.roomInviteMessage(view.room, view.cafe),
                        viewModel::emitMessage,
                    )
                },
                onDeleteRoom = viewModel::deleteRoom,
            )
        }

        item {
            Button(
                onClick = { showShareDialog = true },
                enabled = catalog.cafes.isNotEmpty(),
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
                    "Preferensi memengaruhi skor Carife Match dan urutan Kecocokan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                PreferenceSection("Budget") {
                    BudgetBucket.entries.filter { it != BudgetBucket.ANY }.forEach { bucket ->
                        CarifeChip(
                            label = bucket.label,
                            selected = draft.budget == bucket,
                            onClick = {
                                draft = draft.copy(
                                    budget = if (draft.budget == bucket) BudgetBucket.ANY else bucket
                                )
                            },
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
                            onClick = {
                                draft = draft.copy(
                                    radius = if (draft.radius == radius) RadiusPreference.ANY else radius
                                )
                            },
                        )
                    }
                }

                Button(
                    onClick = {
                        savingPreferences = true
                        scope.launch {
                            try {
                                if (viewModel.savePreferences(draft)) onPreferencesSaved()
                            } finally {
                                savingPreferences = false
                            }
                        }
                    },
                    enabled = !savingPreferences,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Simpan Preferensi & Update Skor Rekomendasi")
                }

                Text(
                    "Pilih tempat yang ingin dibagikan dari katalog. Izin lokasi digunakan untuk menghitung jarak.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showManageFriends) {
        ManageFriendsDialog(
            allPeople = DemoFriendsData.people,
            addedIds = appState.friendIds,
            onToggleFriend = { friendId ->
                if (friendId in appState.friendIds) {
                    viewModel.removeFriend(friendId)
                } else {
                    viewModel.addFriend(friendId)
                }
            },
            onDismiss = { showManageFriends = false },
        )
    }

    if (showCreateRoom) {
        CreateRoomDialog(
            cafes = catalog.cafes,
            friends = friendPeople,
            onDismiss = { showCreateRoom = false },
            onCreate = { cafe, title, schedule, note, invitedIds, capacity ->
                viewModel.createRoom(cafe, title, schedule, note, invitedIds, capacity)
                showCreateRoom = false
            },
        )
    }

    if (showShareDialog) {
        ShareSpotDialog(
            cafes = catalog.cafes,
            onDismiss = { showShareDialog = false },
            onShare = { message ->
                if (openShareSheet(context, message, viewModel::emitMessage)) showShareDialog = false
            },
        )
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    cityName: String,
    onOpenAccountSettings: () -> Unit,
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileAvatar(profile = profile, size = 58.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        profile.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        listOfNotNull(cityName, profile.tagline.takeIf { it.isNotBlank() })
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onOpenAccountSettings,
                    modifier = Modifier.semantics {
                        contentDescription = "Buka pengaturan akun"
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = CarifePrimary,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                profile.status.takeIf { it.isNotBlank() }?.let { status ->
                    Surface(shape = RoundedCornerShape(999.dp), color = CarifeSurfaceContainer) {
                        Text(
                            text = status,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(999.dp), color = CarifeSecondaryContainer) {
                    Text(
                        "Data lokal",
                        style = MaterialTheme.typography.labelSmall,
                        color = CarifeSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
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
private fun PrivacyBadge() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarifeSecondaryContainer.copy(alpha = 0.4f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Shield,
            contentDescription = null,
            tint = CarifeSecondary,
            modifier = Modifier.size(18.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Berbagi Lokasi Berbasis Kafe",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = CarifeSecondary,
            )
            Text(
                text = "Hanya nama & lantai kafe yang terlihat. Koordinat GPS presisi tetap privat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveDot() {
    val transition = rememberInfiniteTransition(label = "liveIndicator")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900), RepeatMode.Reverse),
        label = "liveIndicatorAlpha",
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .semantics { contentDescription = "Indikator demo aktivitas teman" }
            .clip(CircleShape)
            .background(CarifeSecondary.copy(alpha = alpha)),
    )
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
    onShare: (String) -> Unit,
) {
    var selectedCafeId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedCafe = cafes.firstOrNull { it.id == selectedCafeId }
    var note by rememberSaveable { mutableStateOf("") }
    var floor by rememberSaveable { mutableStateOf("") }
    val message = selectedCafe?.let { ShareTextBuilder.mySpotMessage(it, note.trim(), floor.trim()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bagikan Tempat Nongkrong") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Pilih kafe yang ingin dibagikan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (cafes.isNotEmpty()) {
                    CafePicker(cafes, selectedCafe?.id) { selectedCafeId = it }
                } else {
                    Text("Belum ada kafe di kota ini", style = MaterialTheme.typography.titleSmall)
                }
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it },
                    label = { Text("Lantai (opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (opsional)") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                message?.let { SharePreview(it) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { message?.let(onShare) },
                enabled = message != null,
            ) { Text("Bagikan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
