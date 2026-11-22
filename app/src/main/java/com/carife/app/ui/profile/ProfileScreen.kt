package com.carife.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.UserProfile
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import com.carife.app.domain.MatchScorer
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CarifeChip
import com.carife.app.ui.components.ProfileAvatar
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    viewModel: CarifeViewModel,
    onBack: () -> Unit,
    onOpenAccountSettings: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val profile = appState.profile

    var draft by remember(appState.hasSavedPreferences, appState.preferences) {
        mutableStateOf(appState.preferences)
    }
    var savingPreferences by remember { mutableStateOf(false) }

    val bestMatchScore = remember(catalog.cafes, appState.hasSavedPreferences, appState.preferences) {
        if (appState.hasSavedPreferences) {
            MatchScorer.bestMatch(catalog.cafes, appState.preferences)?.second
        } else {
            null
        }
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
                text = "Profil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ProfileCard(
                    profile = profile,
                    cityName = appState.selectedCity.name,
                    onEditProfile = onOpenAccountSettings,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatCard(
                        value = appState.favorites.size.toString(),
                        label = "Spot Favorit",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = bestMatchScore?.let { "$it%" } ?: "—",
                        label = "Fokus Match",
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        value = appState.friendIds.size.toString(),
                        label = "Teman",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Preferensi Nugas",
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
                                    viewModel.savePreferences(draft)
                                } finally {
                                    savingPreferences = false
                                }
                            }
                        },
                        enabled = !savingPreferences,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Simpan Preferensi")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: UserProfile,
    cityName: String,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CarifeSurfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ProfileAvatar(profile = profile, size = 64.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = listOfNotNull(cityName, profile.tagline.takeIf { it.isNotBlank() })
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            }
            TextButton(onClick = onEditProfile) {
                Text("Edit Profil", color = CarifePrimary)
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
