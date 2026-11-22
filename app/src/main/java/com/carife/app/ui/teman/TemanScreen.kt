package com.carife.app.ui.teman

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.CheckIn
import com.carife.app.core.model.UserProfile
import com.carife.app.data.DemoFriendsData
import com.carife.app.data.FriendSpotResolved
import com.carife.app.domain.JoinCheckInDecision
import com.carife.app.domain.ShareTextBuilder
import com.carife.app.domain.SocialRules
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CafePicker
import com.carife.app.ui.components.CarifeChip
import com.carife.app.ui.components.ProfileAvatar
import com.carife.app.ui.components.openShareSheet
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import java.time.Duration
import java.time.Instant

private data class CheckInMove(
    val cafe: Cafe,
    val friendName: String,
)

@Composable
fun TemanScreen(
    viewModel: CarifeViewModel,
    onOpenCafe: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenFriends: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val now by viewModel.openingHoursTime.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var activityJoinedOnly by rememberSaveable { mutableStateOf(false) }
    var roomJoinedOnly by rememberSaveable { mutableStateOf(false) }
    var showCheckInDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateRoom by rememberSaveable { mutableStateOf(false) }
    var selectedRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCheckInMove by remember { mutableStateOf<CheckInMove?>(null) }

    val friendPeople = remember(appState.friendIds) {
        DemoFriendsData.friends(appState.friendIds)
    }
    val friendSpots = remember(appState.friendIds, catalog.cafes) {
        DemoFriendsData.resolve(appState.friendIds, catalog.cafes)
    }
    val roomViews = remember(appState.rooms, appState.friendIds, catalog.cafes, appState.profile) {
        buildRoomViews(
            rooms = appState.rooms,
            friendIds = appState.friendIds,
            cafes = catalog.cafes,
            profile = appState.profile,
        )
    }
    val currentCheckInCafe = appState.checkIn?.cafeId?.let { id ->
        catalog.cafes.firstOrNull { it.id == id }
    }
    val visibleSpots = if (activityJoinedOnly) {
        friendSpots.filter { it.person.id in appState.joinedSpotFriendIds }
    } else {
        friendSpots
    }
    val visibleRooms = if (roomJoinedOnly) {
        roomViews.filter { it.isHost || it.room.id in appState.joinedRoomIds }
    } else {
        roomViews
    }
    val selectedRoom = roomViews.firstOrNull { it.room.id == selectedRoomId }

    fun handleJoin(resolved: FriendSpotResolved) {
        val friendId = resolved.person.id
        if (friendId in appState.joinedSpotFriendIds) {
            viewModel.toggleJoinSpot(friendId)
            return
        }

        val cafe = resolved.cafe ?: return
        viewModel.toggleJoinSpot(friendId)

        when (SocialRules.joinCheckInDecision(appState.checkIn, cafe.id)) {
            JoinCheckInDecision.CHECK_IN -> viewModel.checkIn(cafe, null, null)
            JoinCheckInDecision.KEEP_CURRENT -> Unit
            JoinCheckInDecision.ASK_TO_MOVE ->
                pendingCheckInMove = CheckInMove(cafe, resolved.person.name)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        TemanHeader(
            profile = appState.profile,
            friendCount = appState.friendIds.size,
            roomCount = roomViews.size,
            joinedCount = appState.joinedSpotFriendIds.size,
            onOpenProfile = onOpenProfile,
            onOpenFriends = onOpenFriends,
            onOpenTeman = onOpenFriends,
            onOpenRoom = {
                selectedTab = 1
                roomJoinedOnly = false
            },
            onOpenJoined = {
                selectedTab = 0
                activityJoinedOnly = true
            },
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Aktivitas") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Room") },
            )
        }

        if (selectedTab == 0) {
            ActivityTab(
                modifier = Modifier.weight(1f),
                profile = appState.profile,
                checkIn = appState.checkIn,
                currentCafe = currentCheckInCafe,
                now = now,
                joinedOnly = activityJoinedOnly,
                onJoinedOnlyChange = { activityJoinedOnly = it },
                spots = visibleSpots,
                hasFriends = appState.friendIds.isNotEmpty(),
                joinedIds = appState.joinedSpotFriendIds,
                onCheckInClick = { showCheckInDialog = true },
                onCheckOut = viewModel::checkOut,
                onShareCheckIn = {
                    val checkIn = appState.checkIn
                    val cafe = currentCheckInCafe
                    if (checkIn != null && cafe != null) {
                        openShareSheet(
                            context,
                            ShareTextBuilder.mySpotMessage(cafe, checkIn.note, checkIn.floor),
                            viewModel::emitMessage,
                        )
                    }
                },
                onToggleJoin = ::handleJoin,
                onOpenCafe = onOpenCafe,
                onOpenFriends = onOpenFriends,
            )
        } else {
            RoomTab(
                modifier = Modifier.weight(1f),
                rooms = visibleRooms,
                joinedRoomIds = appState.joinedRoomIds,
                joinedOnly = roomJoinedOnly,
                onJoinedOnlyChange = { roomJoinedOnly = it },
                onCreateRoom = { showCreateRoom = true },
                onOpenRoom = { view -> selectedRoomId = view.room.id },
            )
        }
    }

    if (showCheckInDialog) {
        CheckInDialog(
            cafes = catalog.cafes,
            initial = appState.checkIn,
            onDismiss = { showCheckInDialog = false },
            onSave = { cafe, floor, note ->
                viewModel.checkIn(cafe, floor, note)
                showCheckInDialog = false
            },
        )
    }

    pendingCheckInMove?.let { move ->
        AlertDialog(
            onDismissRequest = { pendingCheckInMove = null },
            title = { Text("Gabung berhasil") },
            text = {
                Text(
                    "Kamu sedang check-in di ${appState.checkIn?.cafeName ?: "kafe lain"}. " +
                        "Pindahkan check-in ke ${move.cafe.name} bareng ${move.friendName}?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.checkIn(move.cafe, null, null)
                        pendingCheckInMove = null
                    },
                ) { Text("Pindahkan") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCheckInMove = null }) { Text("Nanti Saja") }
            },
        )
    }

    if (showCreateRoom) {
        CreateRoomDialog(
            cafes = catalog.cafes,
            onDismiss = { showCreateRoom = false },
            onCreate = { cafe, schedule ->
                viewModel.createRoom(
                    cafe = cafe,
                    title = "Nongkrong di ${cafe.name}",
                    schedule = schedule,
                    note = "",
                    invitedFriendIds = emptyList(),
                    capacity = 5,
                )
                showCreateRoom = false
            },
        )
    }

    selectedRoom?.let { view ->
        RoomDetailSheet(
            view = view,
            joined = view.isHost || view.room.id in appState.joinedRoomIds,
            viewerProfile = appState.profile,
            friends = friendPeople,
            onDismiss = { selectedRoomId = null },
            onToggleJoin = { viewModel.toggleJoinRoom(view.room) },
            onShare = {
                openShareSheet(
                    context,
                    ShareTextBuilder.roomInviteMessage(view.room, view.cafe),
                    viewModel::emitMessage,
                )
            },
            onUpdateInvites = { ids -> viewModel.updateRoomInvites(view.room.id, ids) },
            onUpdateDetails = { title, schedule ->
                viewModel.updateRoomDetails(view.room.id, title, schedule)
            },
            onUpdateCapacity = { capacity ->
                viewModel.updateRoomCapacity(view.room.id, capacity)
            },
            onDelete = {
                viewModel.deleteRoom(view.room.id)
                selectedRoomId = null
            },
        )
    }
}

@Composable
private fun TemanHeader(
    profile: UserProfile,
    friendCount: Int,
    roomCount: Int,
    joinedCount: Int,
    onOpenProfile: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenTeman: () -> Unit,
    onOpenRoom: () -> Unit,
    onOpenJoined: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Teman Nongkrong",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Aktivitas nongkrong temanmu hari ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenFriends) {
                Icon(
                    imageVector = Icons.Outlined.Group,
                    contentDescription = "Buka Teman Saya",
                    tint = CarifePrimary,
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Buka profil",
                        onClick = onOpenProfile,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ProfileAvatar(profile = profile, size = 36.dp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SocialStat(
                value = friendCount.toString(),
                label = "Teman",
                onClick = onOpenTeman,
                modifier = Modifier.weight(1f),
            )
            SocialStat(
                value = roomCount.toString(),
                label = "Room",
                onClick = onOpenRoom,
                modifier = Modifier.weight(1f),
            )
            SocialStat(
                value = joinedCount.toString(),
                label = "Ikut",
                onClick = onOpenJoined,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SocialStat(
    value: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = CarifeSurfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CarifePrimary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActivityTab(
    modifier: Modifier,
    profile: UserProfile,
    checkIn: CheckIn?,
    currentCafe: Cafe?,
    now: Instant,
    joinedOnly: Boolean,
    onJoinedOnlyChange: (Boolean) -> Unit,
    spots: List<FriendSpotResolved>,
    hasFriends: Boolean,
    joinedIds: Set<String>,
    onCheckInClick: () -> Unit,
    onCheckOut: () -> Unit,
    onShareCheckIn: () -> Unit,
    onToggleJoin: (FriendSpotResolved) -> Unit,
    onOpenCafe: (String) -> Unit,
    onOpenFriends: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            if (checkIn == null) {
                CheckInCta(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onClick = onCheckInClick,
                )
            } else {
                CurrentCheckInCard(
                    checkIn = checkIn,
                    cafe = currentCafe,
                    profile = profile,
                    now = now,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onClick = onCheckInClick,
                    onShare = onShareCheckIn,
                    onCheckOut = onCheckOut,
                )
            }
        }

        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CarifeChip(
                    label = "Semua",
                    selected = !joinedOnly,
                    onClick = { onJoinedOnlyChange(false) },
                )
                CarifeChip(
                    label = "Tergabung",
                    selected = joinedOnly,
                    onClick = { onJoinedOnlyChange(true) },
                )
            }
        }

        if (spots.isEmpty()) {
            item {
                Text(
                    text = when {
                        !hasFriends -> "Belum ada aktivitas. Tambah teman untuk melihat tempat nongkrong mereka."
                        joinedOnly -> "Belum ada yang kamu ikuti."
                        else -> "Temanmu belum check-in hari ini."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                if (!hasFriends) {
                    TextButton(
                        onClick = onOpenFriends,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) { Text("Tambah Teman") }
                }
            }
        } else {
            items(spots, key = { it.person.id }) { resolved ->
                Box(Modifier.padding(horizontal = 20.dp)) {
                    FriendSpotCard(
                        resolved = resolved,
                        joined = resolved.person.id in joinedIds,
                        onToggleJoin = { onToggleJoin(resolved) },
                        onOpenCafe = onOpenCafe,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInCta(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CarifePrimary,
        contentColor = CarifeOnPrimary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Check-in di Kafe",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Biar teman tahu kamu sedang nongkrong di mana.",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CurrentCheckInCard(
    checkIn: CheckIn,
    cafe: Cafe?,
    profile: UserProfile,
    now: Instant,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onCheckOut: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CarifeSecondaryContainer.copy(alpha = 0.32f),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarifeSecondary),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileAvatar(profile = profile, size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Kamu sedang di ${cafe?.name ?: checkIn.cafeName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = relativeTime(checkIn.createdAtEpochMillis, now),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = CarifeSecondary,
                )
            }

            checkIn.floor?.let { floor ->
                Text(
                    text = "Lantai $floor",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            checkIn.note?.let { note ->
                NoteBubble(note)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onShare, enabled = cafe != null) {
                    Text("Bagikan")
                }
                TextButton(onClick = onCheckOut) {
                    Text("Check-out", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun relativeTime(createdAtEpochMillis: Long, now: Instant): String {
    val minutes = Duration.between(Instant.ofEpochMilli(createdAtEpochMillis), now)
        .toMinutes()
        .coerceAtLeast(0)
        .toInt()
    return DemoFriendsData.relativeTimeLabel(minutes)
}

@Composable
private fun CheckInDialog(
    cafes: List<Cafe>,
    initial: CheckIn?,
    onDismiss: () -> Unit,
    onSave: (Cafe, String?, String?) -> Unit,
) {
    var selectedCafeId by remember(initial) { mutableStateOf(initial?.cafeId) }
    var floor by remember(initial) { mutableStateOf(initial?.floor.orEmpty()) }
    var note by remember(initial) { mutableStateOf(initial?.note.orEmpty()) }
    var showDetails by remember(initial) {
        mutableStateOf(!initial?.floor.isNullOrBlank() || !initial?.note.isNullOrBlank())
    }

    val selectedCafe = cafes.firstOrNull { it.id == selectedCafeId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Check-in di Kafe" else "Ganti Check-in") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (cafes.isNotEmpty()) {
                    CafePicker(cafes, selectedCafe?.id) { selectedCafeId = it }
                } else {
                    Text("Belum ada kafe di kota ini", style = MaterialTheme.typography.titleSmall)
                }

                TextButton(onClick = { showDetails = !showDetails }) {
                    Text(if (showDetails) "Sembunyikan detail" else "Tambah detail (lantai/catatan)")
                }

                if (showDetails) {
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
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = CarifeSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Yang terlihat: nama kafe & lantai. GPS presisi tetap privat.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCafe?.let { onSave(it, floor.ifBlank { null }, note.ifBlank { null }) }
                },
                enabled = selectedCafe != null,
            ) { Text(if (initial == null) "Check-in" else "Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
