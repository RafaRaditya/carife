package com.carife.app.ui.teman

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                text = "Teman Saya",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cari nama atau kampus") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "Teman Saya (${friends.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            if (filteredFriends.isEmpty()) {
                item {
                    Text(
                        text = if (friends.isEmpty()) {
                            "Belum ada teman. Pilih dari Saran Teman di bawah."
                        } else {
                            "Tidak ada teman yang cocok dengan pencarian."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            } else {
                items(filteredFriends, key = { it.id }) { person ->
                    FriendRow(
                        person = person,
                        resolved = resolvedByFriend[person.id],
                        actionLabel = "Hapus",
                        onAction = { viewModel.removeFriend(person.id) },
                    )
                }
            }

            item {
                Text(
                    text = "Saran Teman (${suggestions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            if (filteredSuggestions.isEmpty()) {
                item {
                    Text(
                        text = if (suggestions.isEmpty()) {
                            "Semua saran teman sudah kamu tambahkan."
                        } else {
                            "Tidak ada saran yang cocok dengan pencarian."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            } else {
                items(filteredSuggestions, key = { it.id }) { person ->
                    FriendRow(
                        person = person,
                        resolved = null,
                        actionLabel = "Tambah",
                        onAction = { viewModel.addFriend(person.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendRow(
    person: FriendPerson,
    resolved: FriendSpotResolved?,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        color = CarifeSurfaceContainerLowest,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FriendAvatar(person, 40.dp)
            Column(Modifier.weight(1f)) {
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
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}
