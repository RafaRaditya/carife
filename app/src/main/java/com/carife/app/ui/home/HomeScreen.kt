package com.carife.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Deck
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.SmokeFree
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.ExploreFilters
import com.carife.app.core.model.NoiseLevel
import com.carife.app.core.model.SortOption
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import com.carife.app.domain.DistanceUtil
import com.carife.app.domain.MatchScorer
import com.carife.app.domain.OpeningHours
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CafeCard
import com.carife.app.ui.components.CarifeChip
import com.carife.app.ui.components.FullMapDialog
import com.carife.app.ui.components.OsmMapView
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifePrimaryFixed
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import com.carife.app.ui.theme.CarifeTertiary
import java.time.Instant

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
    onOpenExplore: () -> Unit,
    onOpenExploreWithFilters: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val compareIds by viewModel.compareIds.collectAsStateWithLifecycle()

    val now = remember(catalog.cafes) { Instant.now() }
    val cafes = catalog.cafes
    val openNowCount = remember(cafes, now) {
        cafes.count { OpeningHours.isOpenNow(it.openingHoursRaw, now) == true }
    }
    val nearest = remember(cafes) {
        cafes.sortedWith(compareBy({ it.distanceMeters ?: Int.MAX_VALUE }, { it.name.lowercase() }))
    }
    val scores = remember(cafes, appState.hasSavedPreferences, appState.preferences) {
        if (!appState.hasSavedPreferences) emptyMap()
        else cafes.mapNotNull { cafe -> MatchScorer.score(cafe, appState.preferences)?.let { cafe.id to it } }.toMap()
    }
    val bestMatch = remember(scores, cafes) {
        scores.entries
            .filter { it.value > 0 }
            .mapNotNull { entry -> cafes.firstOrNull { it.id == entry.key }?.let { it to entry.value } }
            .minWithOrNull(compareByDescending<Pair<Cafe, Int>> { it.second }
                .thenBy { it.first.distanceMeters ?: Int.MAX_VALUE })
    }

    var showCityPicker by remember { mutableStateOf(false) }
    var showFullMap by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HomeHeader(
                cityName = appState.selectedCity.name,
                openNowCount = openNowCount,
                onOpenExplore = onOpenExplore,
                onPickCity = { showCityPicker = true },
            )
        }

        item {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(quickChips) { chip ->
                    CarifeChip(
                        label = chip.label,
                        selected = false,
                        icon = chip.icon,
                        onClick = {
                            viewModel.setExploreFilters(chip.buildFilters(ExploreFilters()))
                            onOpenExploreWithFilters()
                        },
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                OsmMapView(
                    city = appState.selectedCity,
                    cafes = nearest,
                    onOpenCafe = onOpenCafe,
                    maxMarkers = 60,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    onClick = { showFullMap = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = CarifeSurfaceContainerLowest,
                    shadowElevation = 3.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Buka Peta",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CarifePrimary,
                        )
                        Icon(
                            Icons.Filled.NearMe,
                            contentDescription = null,
                            tint = CarifePrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        if (bestMatch != null) {
            item {
                Surface(
                    onClick = { onOpenCafe(bestMatch.first.id) },
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = CarifePrimaryFixed,
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(CarifePrimary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("◎", color = CarifeOnPrimary, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${bestMatch.second}% Sangat Cocok",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "  Carife Match",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CarifePrimary,
                                )
                            }
                            Text(
                                text = bestMatch.first.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${nearest.size} Kafe Nugas Terdekat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Jarak terdekat",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            catalog.errorMessage != null && cafes.isEmpty() -> item {
                CatalogErrorState(
                    message = catalog.errorMessage ?: "",
                    cityName = appState.selectedCity.name,
                    onRetry = viewModel::retry,
                )
            }

            catalog.isRefreshing && cafes.isEmpty() -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CarifePrimary)
                        Spacer(Modifier.size(12.dp))
                        Text(
                            "Memuat kafe di ${appState.selectedCity.name}…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Server OpenStreetMap kadang butuh 30–40 detik.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            cafes.isEmpty() -> item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Belum ada kafe ditemukan di sekitar ${appState.selectedCity.name}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "Coba kota lain atau muat ulang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = viewModel::retry) { Text("Muat ulang") }
                }
            }

            else -> items(nearest, key = { it.id }) { cafe ->
                CafeCard(
                    cafe = cafe,
                    matchScore = scores[cafe.id],
                    isFavorite = appState.favorites.contains(cafe.id),
                    isSelectedForCompare = compareIds.contains(cafe.id),
                    showCompareCheckbox = false,
                    onOpen = { onOpenCafe(cafe.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(cafe.id) },
                    onToggleCompare = { viewModel.toggleCompare(cafe.id) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
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
    }

    if (showCityPicker) {
        CityPickerDialog(
            cities = viewModel.cityRepository.cities,
            selectedId = appState.selectedCity.id,
            onDismiss = { showCityPicker = false },
            onSelect = { cityId ->
                viewModel.selectCity(cityId)
                showCityPicker = false
            },
        )
    }

    if (showFullMap) {
        FullMapDialog(
            city = appState.selectedCity,
            cafes = nearest,
            onOpenCafe = { cafeId ->
                showFullMap = false
                onOpenCafe(cafeId)
            },
            onDismiss = { showFullMap = false },
        )
    }
}

@Composable
private fun CityPickerDialog(
    cities: List<com.carife.app.core.model.City>,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Kota") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                cities.forEach { city ->
                    Surface(
                        onClick = { onSelect(city.id) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (city.id == selectedId) CarifePrimaryFixed
                        else MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(
                                city.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (city.id == selectedId) FontWeight.Bold else FontWeight.Normal,
                            )
                            Text(
                                city.province,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Tutup") } },
    )
}

@Composable
private fun HomeHeader(
    cityName: String,
    openNowCount: Int,
    onOpenExplore: () -> Unit,
    onPickCity: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onPickCity)
                    .padding(vertical = 2.dp),
            ) {
                Text("☕", style = MaterialTheme.typography.titleLarge)
                Column {
                    Text(
                        "Carife",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CarifePrimary,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Sekitar $cityName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = "Ganti kota",
                            tint = CarifePrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            if (openNowCount > 0) {
                Text(
                    text = "● $openNowCount Spot Buka Sekarang",
                    style = MaterialTheme.typography.labelSmall,
                    color = CarifeSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(CarifeSecondaryContainer)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        Text(
            "EKSPLORASI NUGAS",
            style = MaterialTheme.typography.labelSmall,
            color = CarifePrimary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Tempat Nugas Sekitarmu",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Surface(
            onClick = onOpenExplore,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
            color = CarifeSurfaceContainerLowest,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = CarifePrimary, modifier = Modifier.size(20.dp))
                Text(
                    "Cari kafe, wifi, colokan…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Buka filter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CatalogErrorState(message: String, cityName: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Belum ada data kafe", style = MaterialTheme.typography.titleSmall)
        Text(
            "$message\nKota $cityName belum punya data kafe bawaan di aplikasi ini.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!com.carife.app.BuildConfig.OFFLINE_ONLY) {
            TextButton(onClick = onRetry) { Text("Coba lagi") }
        }
    }
}

internal fun catalogSourceLabel(origin: com.carife.app.data.FeedOrigin?): String = when (origin) {
    com.carife.app.data.FeedOrigin.NETWORK ->
        "Data kafe: OpenStreetMap · baru dimuat dari internet · harga, Wi-Fi, dan kepadatan adalah data contoh"
    com.carife.app.data.FeedOrigin.CACHE ->
        "Data kafe: OpenStreetMap · tersimpan di perangkat · harga, Wi-Fi, dan kepadatan adalah data contoh"
    com.carife.app.data.FeedOrigin.BUNDLED ->
        "Data kafe: snapshot OpenStreetMap di dalam aplikasi · jalan tanpa internet · harga, Wi-Fi, dan kepadatan adalah data contoh"
    null -> ""
}
