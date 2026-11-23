package com.carife.app.ui.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import com.carife.app.ui.components.CarifeHairline
import com.carife.app.ui.components.CarifeSectionHeader
import com.carife.app.ui.components.CarifeTag
import com.carife.app.ui.components.CarifeTopBar
import com.carife.app.ui.components.ProfileAvatar
import com.carife.app.ui.theme.CarifeOutlineVariant
import com.carife.app.ui.theme.CarifePrimary
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
        CarifeTopBar(
            title = "Profil",
            onBack = onBack,
            actions = {
                TextButton(
                    onClick = onOpenAccountSettings,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) { Text("Edit") }
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                ProfileIdentity(
                    profile = profile,
                    cityName = appState.selectedCity.name,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item {
                ProfileStats(
                    favorites = appState.favorites.size,
                    matchScore = bestMatchScore,
                    friends = appState.friendIds.size,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CarifeSectionHeader(
                        title = "Preferensi nugas",
                        supporting = "Memengaruhi skor Carife Match dan urutan Kecocokan.",
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = CarifeSurfaceContainerLowest,
                    border = BorderStroke(1.dp, CarifeOutlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
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
                        PreferenceDivider()
                        PreferenceSection("Kebutuhan Wi-Fi") {
                            listOf(WifiRequirement.MIN_30, WifiRequirement.STANDARD, WifiRequirement.ANY).forEach { requirement ->
                                CarifeChip(
                                    label = requirement.label,
                                    selected = draft.wifi == requirement,
                                    onClick = { draft = draft.copy(wifi = requirement) },
                                )
                            }
                        }
                        PreferenceDivider()
                        PreferenceSection("Ruangan & rokok") {
                            listOf(ZonePreference.NON_SMOKING_AC, ZonePreference.SEPARATE, ZonePreference.ANY).forEach { zone ->
                                CarifeChip(
                                    label = zone.label,
                                    selected = draft.zone == zone,
                                    onClick = { draft = draft.copy(zone = zone) },
                                )
                            }
                        }
                        PreferenceDivider()
                        PreferenceSection("Fasilitas wajib") {
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
                        PreferenceDivider()
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
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Text("Simpan preferensi")
                    }
                    Text(
                        text = "Preferensi tersimpan di perangkat ini dan dipakai ulang saat aplikasi dibuka.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileIdentity(
    profile: UserProfile,
    cityName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProfileAvatar(profile = profile, size = 72.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.headlineSmall,
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
        profile.status.takeIf { it.isNotBlank() }?.let { status ->
            CarifeTag(status)
        }
    }
}

@Composable
private fun ProfileStats(
    favorites: Int,
    matchScore: Int?,
    friends: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CarifeSurfaceContainerLowest,
        border = BorderStroke(1.dp, CarifeOutlineVariant),
    ) {
        Row(Modifier.fillMaxWidth()) {
            StatCell(
                value = favorites.toString(),
                label = "Spot favorit",
                modifier = Modifier.weight(1f),
            )
            StatDivider()
            StatCell(
                value = matchScore?.let { "$it%" } ?: "—",
                label = "Fokus match",
                modifier = Modifier.weight(1f),
            )
            StatDivider()
            StatCell(
                value = friends.toString(),
                label = "Teman",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CarifePrimary,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(CarifeOutlineVariant),
    )
}

@Composable
private fun PreferenceDivider() {
    CarifeHairline(Modifier.padding(vertical = 12.dp))
}

@Composable
private fun PreferenceSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
        }
    }
}
