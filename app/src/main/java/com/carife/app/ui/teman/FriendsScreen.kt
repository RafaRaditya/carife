package com.carife.app.ui.teman

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.data.DemoFriendsData
import com.carife.app.data.FriendPerson
import com.carife.app.data.FriendSpotResolved
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CarifeHairline
import com.carife.app.ui.components.CarifeSectionHeader
import com.carife.app.ui.components.CarifeTopBar
import com.carife.app.ui.theme.CarifeOutlineVariant
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest

@Composable
fun FriendsScreen(
    viewModel: CarifeViewModel,
    onBack: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim()

    val friends = remember(appState.friendIds) {
        DemoFriendsData.friends(appState.friendIds)
    }
    val suggestions = remember(appState.friendIds) {
        DemoFriendsData.people.filterNot { it.id in appState.friendIds }
    }
    val resolvedByFriend = remember(appState.friendIds, catalog.cafes) {
        DemoFriendsData.resolve(appState.friendIds, catalog.cafes)
            .associateBy { it.person.id }
    }

    val filteredFriends = friends.filter { person ->
        normalizedQuery.isBlank() ||
            person.name.contains(normalizedQuery, ignoreCase = true) ||
            person.campus.contains(normalizedQuery, ignoreCase = true)
    }
    val filteredSuggestions = suggestions.filter { person ->
        normalizedQuery.isBlank() ||
            person.name.contains(normalizedQuery, ignoreCase = true) ||
            person.campus.contains(normalizedQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CarifeTopBar(
            title = "Teman saya",
            onBack = onBack,
            subtitle = "${friends.size} teman · ${suggestions.size} saran",
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Cari nama atau kampus") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CarifeSectionHeader(
                        title = "Daftar teman",
                        supporting = "${friends.size} orang di daftarmu",
                    )
                    FriendGroup {
                        if (filteredFriends.isEmpty()) {
                            GroupEmptyText(
                                if (friends.isEmpty()) {
                                    "Belum ada teman. Pilih dari saran di bawah."
                                } else {
                                    "Tidak ada teman yang cocok dengan pencarian."
                                }
                            )
                        } else {
                            filteredFriends.forEachIndexed { index, person ->
                                FriendRow(
                                    person = person,
                                    resolved = resolvedByFriend[person.id],
                                    actionLabel = "Hapus",
                                    onAction = { viewModel.removeFriend(person.id) },
                                )
                                if (index != filteredFriends.lastIndex) {
                                    CarifeHairline(Modifier.padding(start = 62.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CarifeSectionHeader(
                        title = "Saran teman",
                        supporting = "${suggestions.size} orang dari kampus sekitar",
                    )
                    FriendGroup {
                        if (filteredSuggestions.isEmpty()) {
                            GroupEmptyText(
                                if (suggestions.isEmpty()) {
                                    "Semua saran teman sudah kamu tambahkan."
                                } else {
                                    "Tidak ada saran yang cocok dengan pencarian."
                                }
                            )
                        } else {
                            filteredSuggestions.forEachIndexed { index, person ->
                                FriendRow(
                                    person = person,
                                    resolved = null,
                                    actionLabel = "Tambah",
                                    onAction = { viewModel.addFriend(person.id) },
                                )
                                if (index != filteredSuggestions.lastIndex) {
                                    CarifeHairline(Modifier.padding(start = 62.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendGroup(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CarifeSurfaceContainerLowest,
        border = BorderStroke(1.dp, CarifeOutlineVariant),
    ) {
        Column {
            content()
        }
    }
}

@Composable
private fun GroupEmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(14.dp),
    )
}

@Composable
private fun FriendRow(
    person: FriendPerson,
    resolved: FriendSpotResolved?,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FriendAvatar(person, 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = person.campus,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = resolved?.cafe?.let { cafe -> "Lagi di ${cafe.name}" }
                    ?: if (person.isOnline) "Aktif sekarang" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        TextButton(
            onClick = onAction,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(actionLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}
