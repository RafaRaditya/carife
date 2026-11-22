package com.carife.app.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.ExploreFilters
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.SortOption
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import com.carife.app.domain.FilterEngine
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.components.CafeCard
import com.carife.app.ui.components.CarifeChip
import com.carife.app.ui.components.CarifeEmptyState
import com.carife.app.ui.components.FilterPanel
import com.carife.app.ui.components.FullMapDialog
import com.carife.app.ui.components.OsmMapView
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: CarifeViewModel,
    onOpenCafe: (String) -> Unit,
    onOpenCompare: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val filters by viewModel.exploreFilters.collectAsStateWithLifecycle()
    val compareIds by viewModel.compareIds.collectAsStateWithLifecycle()
    val filterPanelRequest by viewModel.requestFilterPanel.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showFullMap by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(filterPanelRequest) {
        if (filterPanelRequest != null) {
            showFilterSheet = true
            viewModel.consumeFilterPanelRequest()
        }
    }

    val results = remember(catalog.cafes, filters) { FilterEngine.apply(catalog.cafes, filters) }
    val scores = remember(catalog.cafes, appState.hasSavedPreferences, appState.preferences) {
        if (!appState.hasSavedPreferences) emptyMap()
        else catalog.cafes.mapNotNull { cafe ->
            com.carife.app.domain.MatchScorer.score(cafe, appState.preferences)?.let { cafe.id to it }
        }.toMap()
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SearchBar(
                    query = filters.query,
                    cityName = appState.selectedCity.name,
                    onQueryChange = { viewModel.setExploreFilters(filters.copy(query = it)) },
                    onOpenFilters = { showFilterSheet = true },
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
                            selected = chip.isSelected(filters),
                            onClick = { viewModel.setExploreFilters(chip.toggle(filters)) },
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
                        cafes = results,
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
                        Icon(
                            Icons.Filled.Fullscreen,
                            contentDescription = "Buka peta penuh",
                            tint = CarifePrimary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                        )
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
                        text = "${filters.explicitCriteriaCount} filter aktif · ${results.size} spot nugas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = { viewModel.setExploreFilters(filters.withoutCriteria()) },
                        enabled = filters.hasExplicitCriteria,
                    ) { Text("Reset Semua") }
                }
            }

            if (showFilterSheet) {
                item {
                    Text(
                        "Panel filter terbuka sebagai bottom sheet.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Hasil Pencarian",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    SortSelector(
                        current = filters.sort,
                        onSelect = { viewModel.setExploreFilters(filters.copy(sort = it)) },
                    )
                }
            }

            when {
                catalog.errorMessage != null && catalog.cafes.isEmpty() -> item {
                    CarifeEmptyState(
                        title = "Belum ada data kafe",
                        message = catalog.errorMessage ?: "Gagal memuat kafe.",
                        primaryLabel = if (com.carife.app.BuildConfig.OFFLINE_ONLY) "Ganti Kota" else "Coba lagi",
                        onPrimary = if (com.carife.app.BuildConfig.OFFLINE_ONLY) {
                            { viewModel.emitMessage("Buka Home lalu ketuk nama kota untuk memilih kota lain.") }
                        } else {
                            viewModel::retry
                        },
                    )
                }

                catalog.isRefreshing && catalog.cafes.isEmpty() -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = CarifePrimary) }
                }

                results.isEmpty() -> item {
                    EmptyResults(
                        filters = filters,
                        hasKeyword = filters.query.isNotBlank(),
                        onResetFilters = { viewModel.setExploreFilters(filters.withoutCriteria()) },
                        onClearQuery = { viewModel.setExploreFilters(filters.copy(query = "")) },
                    )
                }

                else -> items(results, key = { it.id }) { cafe ->
                    CafeCard(
                        cafe = cafe,
                        matchScore = scores[cafe.id],
                        isFavorite = appState.favorites.contains(cafe.id),
                        isSelectedForCompare = compareIds.contains(cafe.id),
                        showCompareCheckbox = true,
                        onOpen = { onOpenCafe(cafe.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(cafe.id) },
                        onToggleCompare = { viewModel.toggleCompare(cafe.id) },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
        }

        if (compareIds.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onOpenCompare,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                containerColor = CarifePrimary,
                contentColor = androidx.compose.ui.graphics.Color.White,
                text = {
                    Text(
                        text = "Bandingkan ${compareIds.size} Kafe Pilihan",
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                icon = { Text("⚖") },
            )
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
        ) {
            FilterPanel(
                filters = filters,
                candidates = catalog.cafes,
                onChange = { viewModel.setExploreFilters(it) },
                onReset = { viewModel.setExploreFilters(filters.withoutCriteria()) },
                onDone = { showFilterSheet = false },
            )
        }
    }

    if (showFullMap) {
        FullMapDialog(
            city = appState.selectedCity,
            cafes = results,
            onOpenCafe = { cafeId ->
                showFullMap = false
                onOpenCafe(cafeId)
            },
            onDismiss = { showFullMap = false },
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    cityName: String,
    onQueryChange: (String) -> Unit,
    onOpenFilters: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Sekitar $cityName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = CarifeSurfaceContainerLowest,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = CarifePrimary, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Cari nama kafe, area, atau fasilitas…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Kolom pencarian kafe" },
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier
                            .size(40.dp)
                            .semantics { contentDescription = "Hapus kata kunci" },
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(
                    onClick = onOpenFilters,
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = "Buka panel filter" },
                ) {
                    Icon(Icons.Filled.FilterList, contentDescription = null, tint = CarifePrimary)
                }
            }
        }
    }
}

@Composable
private fun SortSelector(current: SortOption, onSelect: (SortOption) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SortOption.entries.forEach { option ->
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(999.dp),
                color = if (current == option) CarifePrimary else MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (current == option) androidx.compose.ui.graphics.Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyResults(
    filters: ExploreFilters,
    hasKeyword: Boolean,
    onResetFilters: () -> Unit,
    onClearQuery: () -> Unit,
) {
    val needsMissingData = filters.wifi == WifiRequirement.OVER_50 ||
        filters.budget != null

    if (needsMissingData) {
        CarifeEmptyState(
            title = "Data belum tersedia di area ini",
            message = "Sebagian besar kafe dari OpenStreetMap belum punya data harga atau kecepatan Wi-Fi. " +
                "Coba longgarkan filter untuk melihat kafe lain.",
            primaryLabel = "Longgarkan filter",
            onPrimary = onResetFilters,
            secondaryLabel = if (hasKeyword) "Hapus pencarian" else null,
            onSecondary = if (hasKeyword) onClearQuery else null,
        )
    } else if (hasKeyword && filters.hasExplicitCriteria) {
        CarifeEmptyState(
            title = "Tidak ada hasil",
            message = "Tidak ada kafe yang cocok dengan \"${filters.query}\" dan filter aktif.",
            primaryLabel = "Reset Filter",
            onPrimary = onResetFilters,
            secondaryLabel = "Hapus Pencarian",
            onSecondary = onClearQuery,
        )
    } else if (hasKeyword) {
        CarifeEmptyState(
            title = "Kafe tidak ditemukan",
            message = "Tidak ada kafe yang cocok dengan \"${filters.query}\" di kota ini.",
            primaryLabel = "Hapus Pencarian",
            onPrimary = onClearQuery,
        )
    } else {
        CarifeEmptyState(
            title = "Tidak ada kafe yang cocok",
            message = "Filter terlalu ketat. Coba longgarkan kriteria.",
            primaryLabel = "Reset Filter",
            onPrimary = onResetFilters,
        )
    }
}

private data class QuickChipDef(
    val label: String,
    val isSelected: (ExploreFilters) -> Boolean,
    val toggle: (ExploreFilters) -> ExploreFilters,
)

private fun wifiToggle(filter: WifiRequirement, filters: ExploreFilters): ExploreFilters =
    filters.copy(wifi = if (filters.wifi == filter) null else filter)

private val quickChips = listOf(
    QuickChipDef(
        "Buka Sekarang",
        { it.openNowOnly },
        { it.copy(openNowOnly = !it.openNowOnly) },
    ),
    QuickChipDef(
        "Akses Kursi Roda",
        { it.amenities.contains(Amenity.WHEELCHAIR) },
        { it.copy(amenities = it.amenities.toggle(Amenity.WHEELCHAIR)) },
    ),
    QuickChipDef(
        ">30 Mbps",
        { it.wifi == WifiRequirement.OVER_30 },
        { wifiToggle(WifiRequirement.OVER_30, it) },
    ),
    QuickChipDef(
        "Wi-Fi Standar",
        { it.wifi == WifiRequirement.STANDARD },
        { wifiToggle(WifiRequirement.STANDARD, it) },
    ),
    QuickChipDef(
        "AC Non-Smoking",
        { it.zone == ZonePreference.NON_SMOKING_AC },
        { it.copy(zone = if (it.zone == ZonePreference.NON_SMOKING_AC) null else ZonePreference.NON_SMOKING_AC) },
    ),
    QuickChipDef(
        "Outdoor",
        { it.amenities.contains(Amenity.OUTDOOR_SEATING) },
        { it.copy(amenities = it.amenities.toggle(Amenity.OUTDOOR_SEATING)) },
    ),
    QuickChipDef(
        "< Rp25rb",
        { it.budget == BudgetBucket.UNDER_25K },
        { it.copy(budget = if (it.budget == BudgetBucket.UNDER_25K) null else BudgetBucket.UNDER_25K) },
    ),
    QuickChipDef(
        "Dekat <1 km",
        { it.distance == RadiusPreference.NEAR },
        { it.copy(distance = if (it.distance == RadiusPreference.NEAR) null else RadiusPreference.NEAR) },
    ),
)

private fun Set<Amenity>.toggle(amenity: Amenity): Set<Amenity> =
    if (contains(amenity)) this - amenity else this + amenity
