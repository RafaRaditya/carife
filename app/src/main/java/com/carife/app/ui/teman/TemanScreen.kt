package com.carife.app.ui.teman

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.carife.app.ui.components.CarifeHairline
import com.carife.app.ui.components.CarifeSectionHeader
import com.carife.app.ui.components.ProfileAvatar
import com.carife.app.ui.components.openShareSheet
import com.carife.app.ui.theme.CarifeOutlineVariant
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifePrimaryFixed
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLow
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import java.time.Duration
import java.time.Instant

private const val ROOM_SECTION_INDEX = 3

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

    var joinedOnly by rememberSaveable { mutableStateOf(false) }
    var showCheckInDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateRoom by rememberSaveable { mutableStateOf(false) }
    var selectedRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCheckInMove by remember { mutableStateOf<CheckInMove?>(null) }

    val listState = rememberLazyListState()
    var scrollTarget by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(scrollTarget) {
        scrollTarget?.let { target ->
            listState.animateScrollToItem(target)
            scrollTarget = null
        }
    }

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
    val visibleSpots = if (joinedOnly) {
        friendSpots.filter { it.person.id in appState.joinedSpotFriendIds }
    } else {
        friendSpots
    }
    val visibleRooms = if (joinedOnly) {
        roomViews.filter { it.isHost || it.room.id in appState.joinedRoomIds }
    } else {
        roomViews
    }
    val joinedRoomCount = roomViews.count { it.isHost || it.room.id in appState.joinedRoomIds }
    val joinedCount = appState.joinedSpotFriendIds.size + joinedRoomCount
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

    fun checkInToCafe(cafe: Cafe?) {
        cafe ?: return
        viewModel.checkIn(cafe, null, null)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TemanHeader(
                    profile = appState.profile,
                    friendCount = appState.friendIds.size,
                    roomCount = roomViews.size,
                    joinedCount = joinedCount,
                    onOpenProfile = onOpenProfile,
                    onOpenTeman = onOpenFriends,
                    onOpenRoom = {
                        joinedOnly = false
                        scrollTarget = ROOM_SECTION_INDEX
                    },
                    onOpenJoined = {
                        joinedOnly = true
                        scrollTarget = ROOM_SECTION_INDEX
                    },
                )
            }

            item {
                val checkIn = appState.checkIn
                if (checkIn == null) {
                    CheckInCta(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onClick = { showCheckInDialog = true },
                    )
                } else {
                    CurrentCheckInCard(
                        checkIn = checkIn,
                        cafe = currentCheckInCafe,
                        profile = appState.profile,
                        now = now,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        onClick = { showCheckInDialog = true },
                        onShare = {
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
                        onCheckOut = viewModel::checkOut,
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
                        onClick = { joinedOnly = false },
                    )
                    CarifeChip(
                        label = "Tergabung",
                        selected = joinedOnly,
                        onClick = { joinedOnly = true },
                    )
                }
            }

            item {
                CarifeSectionHeader(
                    title = "Room nongkrong",
                    supporting = if (joinedOnly) {
                        "${visibleRooms.size} room yang kamu ikuti"
                    } else {
                        "${visibleRooms.size} room"
                    },
                    actionLabel = "Buat room",
                    onAction = { showCreateRoom = true },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            if (visibleRooms.isEmpty()) {
                item {
                    RoomEmptyState(
                        joinedOnly = joinedOnly,
                        onCreateRoom = { showCreateRoom = true },
                    )
                }
            } else {
                items(visibleRooms, key = { it.room.id }) { view ->
                    val roomCafe = view.cafe
                    val checkedInHere = roomCafe?.id != null && appState.checkIn?.cafeId == roomCafe.id
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        RoomCard(
                            view = view,
                            joined = view.isHost || view.room.id in appState.joinedRoomIds,
                            checkedIn = checkedInHere,
                            onCheckIn = { checkInToCafe(roomCafe) },
                            onLeave = { viewModel.toggleJoinRoom(view.room) },
                            onOpen = { selectedRoomId = view.room.id },
                        )
                    }
                }
            }

            item {
                CarifeHairline(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }

            item {
                CarifeSectionHeader(
                    title = "Aktivitas teman",
                    supporting = if (joinedOnly) {
                        "${visibleSpots.size} aktivitas yang kamu ikuti"
                    } else {
                        "${visibleSpots.size} aktivitas"
                    },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            if (visibleSpots.isEmpty()) {
                item {
                    ActivityEmptyState(
                        hasFriends = appState.friendIds.isNotEmpty(),
                        joinedOnly = joinedOnly,
                        onOpenFriends = onOpenFriends,
                    )
                }
            } else {
                items(visibleSpots, key = { it.person.id }) { resolved ->
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        FriendSpotCard(
                            resolved = resolved,
                            joined = resolved.person.id in appState.joinedSpotFriendIds,
                            onToggleJoin = { handleJoin(resolved) },
                            onOpenCafe = onOpenCafe,
                        )
                    }
                }
            }
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
                TextButton(onClick = { pendingCheckInMove = null }) { Text("Nanti saja") }
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
        val roomCafe = view.cafe
        val checkedInHere = roomCafe?.id != null && appState.checkIn?.cafeId == roomCafe.id
        RoomDetailSheet(
            view = view,
            joined = view.isHost || view.room.id in appState.joinedRoomIds,
            checkedInHere = checkedInHere,
            onCheckInAtCafe = {
                checkInToCafe(roomCafe)
                selectedRoomId = null
            },
            viewerProfile = appState.profile,
            friends = friendPeople,
            onDismiss = { selectedRoomId = null },
            onToggleJoin = {
                val wasJoined = !view.isHost && view.room.id in appState.joinedRoomIds
                viewModel.toggleJoinRoom(view.room)
                if (wasJoined) selectedRoomId = null
            },
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
    onOpenTeman: () -> Unit,
    onOpenRoom: () -> Unit,
    onOpenJoined: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Teman",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Aktivitas dan room teman untuk demo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                ProfileAvatar(profile = profile, size = 40.dp)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = CarifeSurfaceContainerLowest,
            border = BorderStroke(1.dp, CarifeOutlineVariant),
        ) {
            Row(Modifier.fillMaxWidth()) {
                SocialStat(
                    value = friendCount.toString(),
                    label = "Teman",
                    onClick = onOpenTeman,
                    modifier = Modifier.weight(1f),
                )
                SocialDivider()
                SocialStat(
                    value = roomCount.toString(),
                    label = "Room",
                    onClick = onOpenRoom,
                    modifier = Modifier.weight(1f),
                )
                SocialDivider()
                SocialStat(
                    value = joinedCount.toString(),
                    label = "Ikut",
                    onClick = onOpenJoined,
                    modifier = Modifier.weight(1f),
                )
            }
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
    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
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

@Composable
private fun SocialDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(CarifeOutlineVariant),
    )
}

@Composable
private fun RoomEmptyState(
    joinedOnly: Boolean,
    onCreateRoom: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = if (joinedOnly) {
                "Belum ada room yang kamu ikuti."
            } else {
                "Belum ada room. Buat room pertamamu untuk mengajak teman nongkrong."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!joinedOnly) {
            Button(
                onClick = onCreateRoom,
                shape = RoundedCornerShape(12.dp),
            ) { Text("Buat room") }
        }
    }
}

@Composable
private fun ActivityEmptyState(
    hasFriends: Boolean,
    joinedOnly: Boolean,
    onOpenFriends: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = when {
                !hasFriends -> "Belum ada aktivitas. Tambah teman untuk melihat tempat nongkrong mereka."
                joinedOnly -> "Belum ada aktivitas yang kamu ikuti."
                else -> "Temanmu belum check-in hari ini."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!hasFriends) {
            TextButton(
                onClick = onOpenFriends,
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
            ) { Text("Tambah teman") }
        }
    }
}

@Composable
private fun CheckInCta(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CarifeSurfaceContainerLowest,
        border = BorderStroke(1.dp, CarifeOutlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarifePrimaryFixed),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = CarifePrimary,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Check-in di kafe",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Beri tahu teman kamu sedang nugas di mana.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                Text("Check-in sekarang")
            }
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
        shape = RoundedCornerShape(16.dp),
        color = CarifeSecondaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, CarifeSecondary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ProfileAvatar(profile = profile, size = 40.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
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
            checkIn.note?.let { note -> NoteBubble(note) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onShare,
                    enabled = cafe != null,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CarifeOutlineVariant),
                ) { Text("Bagikan") }
                TextButton(
                    onClick = onCheckOut,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
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
        title = { Text(if (initial == null) "Check-in di kafe" else "Ganti check-in") },
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

                TextButton(
                    onClick = { showDetails = !showDetails },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                ) {
                    Text(if (showDetails) "Sembunyikan detail" else "Tambah detail (lantai/catatan)")
                }

                if (showDetails) {
                    OutlinedTextField(
                        value = floor,
                        onValueChange = { floor = it },
                        label = { Text("Lantai (opsional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Catatan (opsional)") },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
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
