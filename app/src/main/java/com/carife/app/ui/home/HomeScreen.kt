package com.carife.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Deck
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.SmokeFree
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.BuildConfig
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.ExploreFilters
import com.carife.app.core.model.NoiseLevel
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import com.carife.app.domain.MatchScorer
import com.carife.app.domain.OpeningHours
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CafeCard
import com.carife.app.ui.components.CafeMapCard
import com.carife.app.ui.components.CarifeChip
import com.carife.app.ui.components.CarifeEmptyState
import com.carife.app.ui.components.CarifeSearchField
import com.carife.app.ui.components.CarifeSectionHeader
import com.carife.app.ui.components.CityPickerDialog
import com.carife.app.ui.components.FeaturedCafeCard
import com.carife.app.ui.components.FullMapDialog
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifePrimaryFixed
import com.carife.app.ui.theme.CarifeSecondary

private data class QuickChip(
    val label: String,
    val icon: ImageVector,
    val buildFilters: (ExploreFilters) -> ExploreFilters,
)

private val quickChips = listOf(
    QuickChip("Buka Sekarang", Icons.Outlined.AccessTime) { it.copy(openNowOnly = true) },
    QuickChip("Hening", Icons.Outlined.VolumeOff) { it.copy(noise = NoiseLevel.HENING) },
    QuickChip("Banyak Colokan", Icons.Outlined.Power) { it.copy(minOutlets = 8) },
    QuickChip("AC Non-Smoking", Icons.Outlined.SmokeFree) {
        it.copy(zone = ZonePreference.NON_SMOKING_AC)
    },
    QuickChip("< Rp30rb", Icons.Outlined.Payments) { it.copy(priceCap = 30_000) },
    QuickChip("Wi-Fi >50Mbps", Icons.Outlined.Wifi) { it.copy(wifi = WifiRequirement.OVER_50) },
    QuickChip("Outdoor Adem", Icons.Outlined.Deck) {
        it.copy(amenities = it.amenities + Amenity.OUTDOOR_SEATING)
    },
)

@Composable
fun HomeScreen(
    viewModel: CarifeViewModel,
    onOpenCafe: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenExploreWithFilters: () -> Unit,
    onOpenQuickFilter: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val compareIds by viewModel.compareIds.collectAsStateWithLifecycle()
    val now by viewModel.openingHoursTime.collectAsStateWithLifecycle()
    val city = catalog.city ?: appState.selectedCity

    val cafes = catalog.cafes
    val openNowCount = remember(cafes, now) {
        cafes.count { OpeningHours.isOpenNow(it, now) == true }
    }
    val nearest = remember(cafes) {
        cafes.sortedWith(compareBy({ it.distanceMeters ?: Int.MAX_VALUE }, { it.name.lowercase() }))
    }
    val scores = remember(cafes, appState.hasSavedPreferences, appState.preferences) {
        if (!appState.hasSavedPreferences) emptyMap()
        else cafes.mapNotNull { cafe -> MatchScorer.score(cafe, appState.preferences)?.let { cafe.id to it } }.toMap()
    }
    val bestMatch = remember(nearest, scores) {
        val cafe = nearest.maxByOrNull { scores[it.id] ?: 0 }
        val score = cafe?.let { scores[it.id] } ?: 0
        if (cafe != null && score > 0) cafe to score else null
    }
    val featured = bestMatch?.first ?: nearest.firstOrNull()
    val featuredScore = bestMatch?.second
    val remaining = nearest.filterNot { it.id == featured?.id }

    var showCityPicker by remember { mutableStateOf(false) }
    var showFullMap by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HomeMasthead(
                cityName = city.name,
                openNowCount = openNowCount,
                cafeCount = cafes.size,
                onOpenSearch = onOpenSearch,
                onOpenFilters = onOpenExploreWithFilters,
                onPickCity = { showCityPicker = true },
            )
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(quickChips) { chip ->
                    CarifeChip(
                        label = chip.label,
                        selected = false,
                        icon = chip.icon,
                        onClick = {
                            viewModel.setExploreFilters(chip.buildFilters(ExploreFilters()))
                            onOpenQuickFilter()
                        },
                    )
                }
            }
        }

        if (cafes.isNotEmpty()) {
            item {
                CafeMapCard(
                    city = city,
                    cafes = nearest,
                    title = "Peta sekitar",
                    onOpenCafe = onOpenCafe,
                    onOpenFullMap = { showFullMap = true },
                    modifier = Modifier.padding(horizontal = 20.dp),
                    maxMarkers = 20,
                    userLocation = appState.userLocation,
                )
            }

            if (featured != null) {
                item {
                    CarifeSectionHeader(
                        title = if (featuredScore != null) "Pilihan untukmu" else "Paling dekat",
                        supporting = if (featuredScore != null) {
                            "Dihitung dari preferensi nugas yang kamu simpan"
                        } else if (appState.userLocation != null) {
                            "Jarak terdekat dari lokasimu"
                        } else {
                            "Jarak terdekat dari pusat kota"
                        },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                item {
                    FeaturedCafeCard(
                        cafe = featured,
                        matchScore = featuredScore,
                        isFavorite = appState.favorites.contains(featured.id),
                        isSelectedForCompare = compareIds.contains(featured.id),
                        now = now,
                        onOpen = { onOpenCafe(featured.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(featured.id) },
                        onToggleCompare = { viewModel.toggleCompare(featured.id) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            item {
                CarifeSectionHeader(
                    title = "Semua kafe",
                    supporting = "${nearest.size} spot · urut jarak terdekat",
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            if (catalog.errorMessage != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = catalog.errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = viewModel::retry,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) { Text("Coba lagi") }
                    }
                }
            }

            items(remaining, key = { it.id }) { cafe ->
                CafeCard(
                    cafe = cafe,
                    matchScore = scores[cafe.id],
                    isFavorite = appState.favorites.contains(cafe.id),
                    isSelectedForCompare = compareIds.contains(cafe.id),
                    now = now,
                    onOpen = { onOpenCafe(cafe.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(cafe.id) },
                    onToggleCompare = { viewModel.toggleCompare(cafe.id) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            if (catalog.origin != null) {
                item {
                    Text(
                        text = catalogSourceLabel(catalog.origin),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        } else {
            when {
                catalog.errorMessage != null -> item {
                    CarifeEmptyState(
                        title = "Katalog belum bisa dimuat",
                        message = catalog.errorMessage.orEmpty(),
                        primaryLabel = "Coba lagi",
                        onPrimary = viewModel::retry,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                catalog.city == null || catalog.isRefreshing -> item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(color = CarifePrimary)
                        Text(
                            "Memuat kafe di ${city.name}…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> item {
                    CarifeEmptyState(
                        title = "Belum ada kafe di ${city.name}",
                        message = "Coba pilih kota lain, atau muat ulang katalog.",
                        primaryLabel = if (BuildConfig.OFFLINE_ONLY) "Pilih kota" else "Muat ulang",
                        onPrimary = if (BuildConfig.OFFLINE_ONLY) {
                            { showCityPicker = true }
                        } else {
                            viewModel::retry
                        },
                    )
                }
            }
        }
    }

    if (showCityPicker) {
        CityPickerDialog(
            cities = viewModel.cityRepository.cities,
            selectedId = city.id,
            onDismiss = { showCityPicker = false },
            onSelect = { cityId ->
                viewModel.selectCity(cityId)
                showCityPicker = false
            },
        )
    }

    if (showFullMap) {
        FullMapDialog(
            city = city,
            cafes = nearest,
            onOpenCafe = { cafeId ->
                showFullMap = false
                onOpenCafe(cafeId)
            },
            onDismiss = { showFullMap = false },
            userLocation = appState.userLocation,
        )
    }
}

@Composable
private fun HomeMasthead(
    cityName: String,
    openNowCount: Int,
    cafeCount: Int,
    onOpenSearch: () -> Unit,
    onOpenFilters: () -> Unit,
    onPickCity: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CarifePrimaryFixed),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalCafe,
                        contentDescription = null,
                        tint = CarifePrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column {
                    Text(
                        text = "Carife",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Panduan nugas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onPickCity)
                    .padding(horizontal = 6.dp)
                    .semantics { contentDescription = "Ganti kota, sekarang $cityName" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = CarifePrimary,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = cityName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(
                text = "Tempat nugas\nsekitarmu.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Kafe di $cityName dengan Wi-Fi kencang, colokan, dan meja yang tenang untuk fokus.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (openNowCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = CarifeSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "$openNowCount buka sekarang · $cafeCount spot",
                        style = MaterialTheme.typography.labelMedium,
                        color = CarifeSecondary,
                    )
                }
            }
        }

        CarifeSearchField(
            value = "",
            onValueChange = {},
            onOpenFilters = onOpenFilters,
            readOnly = true,
            onClick = onOpenSearch,
            placeholder = "Cari kafe, area, atau fasilitas",
        )
    }
}

internal fun catalogSourceLabel(origin: com.carife.app.data.FeedOrigin?): String = when (origin) {
    com.carife.app.data.FeedOrigin.NETWORK ->
        "Data kafe: OpenStreetMap · baru dimuat dari internet"
    com.carife.app.data.FeedOrigin.CACHE ->
        "Data kafe: OpenStreetMap · tersimpan di perangkat"
    com.carife.app.data.FeedOrigin.BUNDLED ->
        "Data kafe: snapshot OpenStreetMap di dalam aplikasi · jalan tanpa internet"
    null -> ""
}
