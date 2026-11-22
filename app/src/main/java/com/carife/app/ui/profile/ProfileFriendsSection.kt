package com.carife.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.StudyRoom
import com.carife.app.core.model.UserProfile
import com.carife.app.data.DemoFriendsData
import com.carife.app.data.FriendPerson
import com.carife.app.data.FriendSpotResolved
import com.carife.app.domain.DistanceUtil
import com.carife.app.ui.components.CafePicker
import com.carife.app.ui.components.CarifeChip
import com.carife.app.ui.components.ProfileAvatar
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifeOnTertiaryFixed
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLow
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import com.carife.app.ui.theme.CarifeTertiaryFixed

@Composable
internal fun FriendListSection(
    people: List<FriendPerson>,
    resolvedByFriend: Map<String, FriendSpotResolved>,
    joinedFriendIds: Set<String>,
    onManageFriends: () -> Unit,
    onOpenCafe: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Teman Saya (${people.size})",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onManageFriends) { Text("Tambah Teman") }
        }

        if (people.isEmpty()) {
            Text(
                text = "Belum ada teman. Tambahkan teman demo untuk melihat aktivitas mereka.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        } else {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(people, key = { it.id }) { person ->
                    val resolved = resolvedByFriend[person.id]
                    FriendMiniCard(
                        person = person,
                        cafe = resolved?.cafe,
                        joined = person.id in joinedFriendIds,
                        onOpenCafe = onOpenCafe,
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendMiniCard(
    person: FriendPerson,
    cafe: Cafe?,
    joined: Boolean,
    onOpenCafe: (String) -> Unit,
) {
    val openCafe = cafe?.let { target ->
        Modifier.clickable(
            role = Role.Button,
            onClickLabel = "Buka ${target.name}",
        ) { onOpenCafe(target.id) }
    } ?: Modifier

    Surface(
        modifier = Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(openCafe),
        shape = RoundedCornerShape(18.dp),
        color = if (joined) CarifeSecondaryContainer.copy(alpha = 0.45f) else CarifeSurfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FriendAvatar(person, 38.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (person.isOnline) "Aktif" else "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (person.isOnline) CarifeSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = cafe?.name ?: "Belum check-in",
                style = MaterialTheme.typography.labelSmall,
                color = if (cafe != null) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (joined) {
                Text(
                    text = "Tergabung",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CarifeSecondary,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FriendSpotCard(
    resolved: FriendSpotResolved,
    joined: Boolean,
    onToggleJoin: () -> Unit,
    onOpenCafe: (String) -> Unit,
) {
    val person = resolved.person
    val spot = resolved.spot
    val cafe = resolved.cafe
    val badge = cafe?.let { DemoFriendsData.badgeLabel(spot, it) }
    val openCafe = cafe?.let { target ->
        Modifier
            .clickable(
                role = Role.Button,
                onClickLabel = "Buka ${target.name}",
            ) { onOpenCafe(target.id) }
            .semantics(mergeDescendants = true) {}
    } ?: Modifier

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(openCafe)
            .then(
                if (joined) {
                    Modifier.border(1.dp, CarifeSecondary, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (joined) {
            CarifeSecondaryContainer.copy(alpha = 0.28f)
        } else {
            CarifeSurfaceContainerLowest
        },
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FriendAvatar(person, 44.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = person.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = DemoFriendsData.relativeTimeLabel(spot.minutesAgo),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = if (cafe != null) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = cafe?.name ?: "Belum ada kafe demo di kota ini",
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (cafe != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (cafe != null) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        spot.floor?.let { floor ->
                            Text(
                                text = "($floor)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                badge?.let { label ->
                    FriendBadge(label = label, prominent = spot.groupProject)
                }
            }

            NoteBubble(spot.note)

            if (joined) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = CarifeSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Kamu ikut nongkrong di sini.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CarifeSecondary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    cafe?.let { resolvedCafe ->
                        cafeMeta(resolvedCafe).forEach { meta -> FriendMetaItem(meta) }
                    }
                }
                cafe?.let {
                    FriendCta(
                        label = if (joined) "Tergabung" else "Gabung",
                        prominent = !joined,
                        selected = joined,
                        enabled = true,
                        onClick = onToggleJoin,
                    )
                }
            }
        }
    }
}

@Composable
internal fun ManageFriendsDialog(
    allPeople: List<FriendPerson>,
    addedIds: Set<String>,
    onToggleFriend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teman Saya") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Tambah atau hapus teman demo. Aktivitas mereka muncul di feed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                allPeople.forEach { person ->
                    val added = person.id in addedIds
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FriendAvatar(person, 38.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = person.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${person.campus} · ${if (person.isOnline) "Aktif" else "Offline"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TextButton(onClick = { onToggleFriend(person.id) }) {
                            Text(if (added) "Hapus" else "Tambah")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Selesai") } },
    )
}

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

@Composable
internal fun RoomsSection(
    rooms: List<RoomView>,
    joinedRoomIds: Set<String>,
    onCreateRoom: () -> Unit,
    onJoinRoom: (StudyRoom) -> Unit,
    onShareRoom: (RoomView) -> Unit,
    onDeleteRoom: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

        if (rooms.isEmpty()) {
            Text(
                text = "Belum ada room. Buat room untuk mengajak teman nongkrong bareng.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        } else {
            rooms.forEach { view ->
                RoomCard(
                    view = view,
                    joined = view.isHost || view.room.id in joinedRoomIds,
                    onJoin = { onJoinRoom(view.room) },
                    onShare = { onShareRoom(view) },
                    onDelete = { onDeleteRoom(view.room.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoomCard(
    view: RoomView,
    joined: Boolean,
    onJoin: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val room = view.room
    val participants = 1 + view.invitedPeople.size + if (!view.isHost && joined) 1 else 0
    val isFull = !joined && participants >= room.capacity
    val accent = if (joined) CarifeSecondary else CarifePrimary

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (joined) CarifeSecondaryContainer.copy(alpha = 0.28f) else CarifeSurfaceContainerLowest,
        border = if (joined) androidx.compose.foundation.BorderStroke(1.dp, CarifeSecondary) else null,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
                    )
                    Text(
                        text = "Host: ${view.hostName} · ${room.schedule}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FriendBadge(
                    label = if (view.isHost) "Room saya" else "Room teman",
                    prominent = view.isHost,
                )
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

            room.note.takeIf { it.isNotBlank() }?.let { NoteBubble(it) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (view.hostProfile != null) {
                    ProfileAvatar(profile = view.hostProfile, size = 26.dp)
                } else {
                    ParticipantAvatar(
                        initials = view.hostInitials,
                        color = view.hostColor,
                    )
                }
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
                TextButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text("Bagikan")
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (!view.isHost) {
                    FriendCta(
                        label = when {
                            joined -> "Tergabung"
                            isFull -> "Room penuh"
                            else -> "Gabung"
                        },
                        prominent = !joined && !isFull,
                        selected = joined,
                        enabled = !isFull,
                        onClick = onJoin,
                    )
                } else {
                    TextButton(onClick = onDelete) {
                        Text("Tutup Room", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantAvatar(initials: String, color: Long) {
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CreateRoomDialog(
    cafes: List<Cafe>,
    friends: List<FriendPerson>,
    onDismiss: () -> Unit,
    onCreate: (Cafe, String, String, String, List<String>, Int) -> Unit,
) {
    var selectedCafeId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("Hari ini · 16.00") }
    var note by remember { mutableStateOf("") }
    var invitedIds by remember { mutableStateOf(emptySet<String>()) }
    var capacity by remember { mutableStateOf(4) }

    val selectedCafe = cafes.firstOrNull { it.id == selectedCafeId }
    val canCreate = selectedCafe != null && title.isNotBlank() && schedule.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Room Nongkrong") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Pilih kafe dan undang teman untuk nongkrong bareng.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (cafes.isNotEmpty()) {
                    CafePicker(cafes, selectedCafe?.id) { selectedCafeId = it }
                } else {
                    Text("Belum ada kafe di kota ini", style = MaterialTheme.typography.titleSmall)
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul room") },
                    placeholder = { Text("Contoh: Belajar bareng UTS") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = schedule,
                    onValueChange = { schedule = it },
                    label = { Text("Jadwal") },
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

                Text(
                    text = "Undang teman",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (friends.isEmpty()) {
                    Text(
                        text = "Tambahkan teman dulu lewat Kelola Teman.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        friends.forEach { person ->
                            CarifeChip(
                                label = person.name.substringBefore(" "),
                                selected = person.id in invitedIds,
                                onClick = {
                                    invitedIds = when {
                                        person.id in invitedIds -> invitedIds - person.id
                                        invitedIds.size < capacity - 1 -> invitedIds + person.id
                                        else -> invitedIds
                                    }
                                },
                            )
                        }
                    }
                }

                Text(
                    text = "Kuota peserta (termasuk kamu)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 4, 5, 6).forEach { value ->
                        CarifeChip(
                            label = "$value",
                            selected = capacity == value,
                            onClick = {
                                capacity = value
                                invitedIds = invitedIds.take(value - 1).toSet()
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedCafe?.let { cafe ->
                        onCreate(cafe, title, schedule, note, invitedIds.toList(), capacity)
                    }
                },
                enabled = canCreate,
            ) { Text("Buat Room") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun FriendAvatar(person: FriendPerson, size: androidx.compose.ui.unit.Dp) {
    Box {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(person.avatarColor)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                person.initials,
                color = CarifeOnPrimary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(13.dp)
                .clip(CircleShape)
                .background(if (person.isOnline) CarifeSecondary else MaterialTheme.colorScheme.outline)
                .border(2.dp, CarifeSurfaceContainerLowest, CircleShape),
        )
    }
}

@Composable
private fun NoteBubble(note: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CarifeSurfaceContainerLow)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = CarifePrimary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "\"$note\"",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FriendBadge(label: String, prominent: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (prominent) CarifeTertiaryFixed else CarifeSurfaceContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (prominent) CarifeOnTertiaryFixed else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun FriendCta(
    label: String,
    prominent: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        !enabled -> CarifeSurfaceContainer
        selected -> CarifeSecondaryContainer
        prominent -> CarifePrimary
        else -> CarifeSurfaceContainer
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        selected -> CarifeSecondary
        prominent -> CarifeOnPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        color = background,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            Icon(
                imageVector = if (selected) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private data class FriendMeta(val icon: ImageVector, val label: String)

@Composable
private fun FriendMetaItem(meta: FriendMeta) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = meta.icon,
            contentDescription = null,
            tint = CarifeSecondary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = meta.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun cafeMeta(cafe: Cafe): List<FriendMeta> = buildList {
    cafe.wifiMbps?.let { add(FriendMeta(Icons.Outlined.Wifi, "$it Mbps")) }
    cafe.noiseLevel?.let { add(FriendMeta(Icons.AutoMirrored.Outlined.VolumeDown, it.label)) }
    cafe.outletCount?.let { add(FriendMeta(Icons.Outlined.Power, "$it colokan")) }
    cafe.distanceMeters?.let { add(FriendMeta(Icons.Outlined.NearMe, DistanceUtil.formatDistance(it))) }
}
