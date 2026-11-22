package com.carife.app.ui.teman

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.StudyRoom
import com.carife.app.core.model.UserProfile
import com.carife.app.data.DemoFriendsData
import com.carife.app.data.FriendPerson
import com.carife.app.ui.components.CafePicker
import com.carife.app.ui.components.CarifeChip
import com.carife.app.ui.components.ProfileAvatar
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifeOnTertiaryFixed
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import com.carife.app.ui.theme.CarifeTertiaryFixed

internal data class RoomView(
    val room: StudyRoom,
    val cafe: Cafe?,
    val hostName: String,
    val hostInitials: String,
    val hostColor: Long,
    val hostProfile: UserProfile?,
    val isHost: Boolean,
    val invitedPeople: List<FriendPerson>,
)

internal fun buildRoomViews(
    rooms: List<StudyRoom>,
    friendIds: Set<String>,
    cafes: List<Cafe>,
    profile: UserProfile,
): List<RoomView> {
    val friendById = DemoFriendsData.people.associateBy { it.id }
    val userRooms = rooms
        .sortedByDescending { it.createdAtEpochMillis }
        .map { room ->
            RoomView(
                room = room,
                cafe = DemoFriendsData.resolveRoomCafe(room, cafes),
                hostName = "Kamu",
                hostInitials = profile.initials,
                hostColor = profile.avatarColor,
                hostProfile = profile,
                isHost = true,
                invitedPeople = room.invitedFriendIds.mapNotNull(friendById::get),
            )
        }
    val demoRooms = DemoFriendsData.demoRooms
        .filter { room -> room.hostFriendId in friendIds }
        .map { room ->
            val host = room.hostFriendId?.let(friendById::get)
            RoomView(
                room = room,
                cafe = DemoFriendsData.resolveRoomCafe(room, cafes),
                hostName = host?.name ?: "Teman",
                hostInitials = host?.initials ?: "?",
                hostColor = host?.avatarColor ?: 0xFF45664B,
                hostProfile = null,
                isHost = false,
                invitedPeople = room.invitedFriendIds.mapNotNull(friendById::get),
            )
        }
    return userRooms + demoRooms
}

@Composable
internal fun RoomTab(
    modifier: Modifier,
    rooms: List<RoomView>,
    joinedRoomIds: Set<String>,
    joinedOnly: Boolean,
    onJoinedOnlyChange: (Boolean) -> Unit,
    onCreateRoom: () -> Unit,
    onOpenRoom: (RoomView) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Room Nongkrong",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onCreateRoom) { Text("Buat Room") }
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

        if (rooms.isEmpty()) {
            item {
                Text(
                    text = if (joinedOnly) {
                        "Belum ada room yang kamu ikuti."
                    } else {
                        "Belum ada room. Buat room pertamamu untuk mengajak teman nongkrong."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                if (!joinedOnly) {
                    Button(
                        onClick = onCreateRoom,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    ) { Text("Buat Room") }
                }
            }
        } else {
            items(rooms, key = { it.room.id }) { view ->
                Box(Modifier.padding(horizontal = 20.dp)) {
                    RoomCard(
                        view = view,
                        joined = view.isHost || view.room.id in joinedRoomIds,
                        onOpen = { onOpenRoom(view) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomCard(
    view: RoomView,
    joined: Boolean,
    onOpen: () -> Unit,
) {
    val room = view.room
    val participants = 1 + view.invitedPeople.size + if (!view.isHost && joined) 1 else 0
    val accent = if (joined) CarifeSecondary else CarifePrimary

    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (joined) {
            CarifeSecondaryContainer.copy(alpha = 0.28f)
        } else {
            CarifeSurfaceContainerLowest
        },
        border = if (joined) BorderStroke(1.dp, CarifeSecondary) else null,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = room.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Host: ${view.hostName} · ${room.schedule}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RoomStatusBadge(view = view, joined = joined)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = view.cafe?.name ?: room.cafeName ?: "Kafe tidak ditemukan di kota ini",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ParticipantAvatar(
                    initials = view.hostInitials,
                    color = view.hostColor,
                )
                view.invitedPeople.take(3).forEach { person ->
                    ParticipantAvatar(
                        initials = person.initials,
                        color = person.avatarColor,
                    )
                }
                if (view.invitedPeople.size > 3) {
                    Text(
                        text = "+${view.invitedPeople.size - 3}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "$participants/${room.capacity} peserta",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Outlined.MeetingRoom,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun RoomStatusBadge(view: RoomView, joined: Boolean) {
    val (label, background, contentColor) = when {
        view.isHost -> Triple("Room saya", CarifeTertiaryFixed, CarifeOnTertiaryFixed)
        joined -> Triple("Tergabung", CarifeSecondaryContainer, CarifeSecondary)
        else -> Triple("Room teman", CarifeSurfaceContainer, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ParticipantAvatar(initials: String, color: Long) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color(color)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelSmall,
            color = CarifeOnPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun RoomDetailSheet(
    view: RoomView,
    joined: Boolean,
    viewerProfile: UserProfile,
    friends: List<FriendPerson>,
    onDismiss: () -> Unit,
    onToggleJoin: () -> Unit,
    onShare: () -> Unit,
    onUpdateInvites: (List<String>) -> Unit,
    onUpdateDetails: (String, String) -> Unit,
    onUpdateCapacity: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var showInvite by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showQuota by remember { mutableStateOf(false) }

    val room = view.room
    val participants = 1 + view.invitedPeople.size + if (!view.isHost && joined) 1 else 0
    val isFull = !joined && participants >= room.capacity

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = room.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Host: ${view.hostName} · ${room.schedule}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = CarifePrimary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = view.cafe?.name ?: room.cafeName ?: "Kafe tidak ditemukan di kota ini",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CarifePrimary,
                )
            }

            room.note.takeIf { it.isNotBlank() }?.let { NoteBubble(it) }

            Text(
                text = "Peserta ($participants/${room.capacity})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (view.hostProfile != null) {
                    ProfileAvatar(profile = view.hostProfile, size = 28.dp)
                } else {
                    ParticipantAvatar(view.hostInitials, view.hostColor)
                }
                view.invitedPeople.forEach { person ->
                    ParticipantAvatar(person.initials, person.avatarColor)
                }
                if (!view.isHost && joined) {
                    ProfileAvatar(profile = viewerProfile, size = 28.dp)
                }
            }

            if (view.isHost) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Bagikan")
                    }
                    OutlinedButton(onClick = { showInvite = true }, modifier = Modifier.weight(1f)) {
                        Text("Undang Teman")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showEdit = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Edit Judul/Waktu")
                    }
                    TextButton(onClick = { showQuota = true }) { Text("Ubah Kuota") }
                }
                TextButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Text("Tutup Room", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Button(
                    onClick = onToggleJoin,
                    enabled = joined || !isFull,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            joined -> "Tergabung"
                            isFull -> "Room penuh"
                            else -> "Gabung"
                        }
                    )
                }
                if (joined) {
                    Text(
                        text = "Kamu tergabung di room ini.",
                        style = MaterialTheme.typography.labelSmall,
                        color = CarifeSecondary,
                    )
                }
                OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Bagikan Undangan")
                }
            }
        }
    }

    if (showInvite) {
        InviteFriendsDialog(
            friends = friends,
            invitedIds = view.room.invitedFriendIds.toSet(),
            maxInvites = (room.capacity - 1).coerceAtLeast(0),
            onDismiss = { showInvite = false },
            onSave = { ids ->
                onUpdateInvites(ids)
                showInvite = false
            },
        )
    }
    if (showEdit) {
        EditRoomDialog(
            initialTitle = room.title,
            initialSchedule = room.schedule,
            onDismiss = { showEdit = false },
            onSave = { title, schedule ->
                onUpdateDetails(title, schedule)
                showEdit = false
            },
        )
    }
    if (showQuota) {
        QuotaDialog(
            current = room.capacity,
            minimum = 1 + view.invitedPeople.size,
            onDismiss = { showQuota = false },
            onSave = { capacity ->
                onUpdateCapacity(capacity)
                showQuota = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InviteFriendsDialog(
    friends: List<FriendPerson>,
    invitedIds: Set<String>,
    maxInvites: Int,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var selected by remember { mutableStateOf(invitedIds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Undang Teman") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Pilih teman untuk diundang. Maksimal $maxInvites teman.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (friends.isEmpty()) {
                    Text(
                        text = "Belum ada teman. Tambahkan teman dulu lewat Teman Saya.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        friends.forEach { person ->
                            CarifeChip(
                                label = person.name.substringBefore(" "),
                                selected = person.id in selected,
                                onClick = {
                                    selected = when {
                                        person.id in selected -> selected - person.id
                                        selected.size < maxInvites -> selected + person.id
                                        else -> selected
                                    }
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected.toList()) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun EditRoomDialog(
    initialTitle: String,
    initialSchedule: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    var title by remember(initialTitle) { mutableStateOf(initialTitle) }
    var schedule by remember(initialSchedule) { mutableStateOf(initialSchedule) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Room") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("Waktu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, schedule) },
                enabled = title.isNotBlank() && schedule.isNotBlank(),
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuotaDialog(
    current: Int,
    minimum: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var capacity by remember(current) { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah Kuota") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Kuota tidak bisa lebih kecil dari jumlah peserta saat ini ($minimum).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(3, 4, 5, 6, 8, 10).forEach { value ->
                        CarifeChip(
                            label = "$value",
                            selected = capacity == value,
                            enabled = value >= minimum,
                            onClick = { capacity = value },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(capacity) }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CreateRoomDialog(
    cafes: List<Cafe>,
    onDismiss: () -> Unit,
    onCreate: (Cafe, String) -> Unit,
) {
    var selectedCafeId by remember { mutableStateOf<String?>(null) }
    var schedule by remember { mutableStateOf("Hari ini · 16.00") }

    val selectedCafe = cafes.firstOrNull { it.id == selectedCafeId }
    val presets = listOf("Hari ini · 16.00", "Hari ini · 19.00", "Besok · 10.00")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Room") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Pilih kafe dan waktu. Kamu bisa mengundang teman setelah room dibuat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (cafes.isNotEmpty()) {
                    CafePicker(cafes, selectedCafe?.id) { selectedCafeId = it }
                } else {
                    Text("Belum ada kafe di kota ini", style = MaterialTheme.typography.titleSmall)
                }

                Text(
                    text = "Waktu",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    presets.forEach { preset ->
                        CarifeChip(
                            label = preset,
                            selected = schedule == preset,
                            onClick = { schedule = preset },
                        )
                    }
                }
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("Atau tulis sendiri") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedCafe?.let { onCreate(it, schedule) } },
                enabled = selectedCafe != null && schedule.isNotBlank(),
            ) { Text("Buat Room") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
