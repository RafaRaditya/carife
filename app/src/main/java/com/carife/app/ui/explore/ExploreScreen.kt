package com.carife.app.ui.explore

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carife.app.BuildConfig
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.ExploreFilters
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.SortOption
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import com.carife.app.domain.FilterEngine
import com.carife.app.domain.MatchScorer
import com.carife.app.ui.CarifeViewModel
import com.carife.app.ui.ExploreEntry
import com.carife.app.ui.components.CafeCard
import com.carife.app.ui.components.CafeMapCard
import com.carife.app.ui.components.CarifeChip
import com.carife.app.ui.components.CarifeEmptyState
import com.carife.app.ui.components.CarifeSearchField
import com.carife.app.ui.components.CarifeSectionHeader
import com.carife.app.ui.components.FilterPanel
import com.carife.app.ui.components.FullMapDialog
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifeOutlineVariant
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifePrimaryFixed
import com.carife.app.ui.theme.CarifeSurfaceContainerLow
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import java.time.Instant

// Search, quick filters, map and filter status precede the results.
private const val RESULTS_HEADING_INDEX = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: CarifeViewModel,
    onOpenCafe: (String) -> Unit,
    onOpenCompare: () -> Unit,
    onOpenHome: () -> Unit,
) {
    val appState by viewModel.appState.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val filters by viewModel.exploreFilters.collectAsStateWithLifecycle()
    val compareIds by viewModel.compareIds.collectAsStateWithLifecycle()
    val exploreEntry by viewModel.exploreEntry.collectAsStateWithLifecycle()
    val now by viewModel.openingHoursTime.collectAsStateWithLifecycle()
    val city = catalog.city ?: appState.selectedCity

    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showFullMap by rememberSaveable { mutableStateOf(false) }
    var focusSearch by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(exploreEntry) {
        when (exploreEntry) {
            ExploreEntry.SEARCH -> {
                listState.scrollToItem(0)
                focusSearch = true
            }
            ExploreEntry.FILTERS -> {
                focusManager.clearFocus()
                showFilterSheet = true
            }
            ExploreEntry.QUICK_FILTER -> {
                focusManager.clearFocus()
                listState.scrollToItem(0)
            }
            null -> return@LaunchedEffect
        }
        viewModel.consumeExploreEntry()
    }

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            focusRequester.requestFocus()
            keyboard?.show()
            focusSearch = false
        }
    }

    val scores = remember(catalog.cafes, appState.hasSavedPreferences, appState.preferences) {
        if (!appState.hasSavedPreferences) emptyMap()
        else catalog.cafes.mapNotNull { cafe ->
            MatchScorer.score(cafe, appState.preferences)?.let { cafe.id to it }
        }.toMap()
    }
    val filterTime = if (filters.openNowOnly) now else Instant.EPOCH
    val results = remember(catalog.cafes, filters, scores, filterTime) {
        FilterEngine.apply(catalog.cafes, filters, now = filterTime, matchScores = scores)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = if (compareIds.size >= 2) 112.dp else 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Jelajah",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Saring kafe di ${city.name} berdasarkan Wi-Fi, colokan, suasana, dan harga.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    CarifeSearchField(
                        value = filters.query,
                        onValueChange = { viewModel.setExploreFilters(filters.copy(query = it)) },
                        onOpenFilters = {
                            focusManager.clearFocus()
                            showFilterSheet = true
                        },
                        focusRequester = focusRequester,
                    )
                }
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

            if (catalog.cafes.isNotEmpty()) {
                if (results.isNotEmpty()) {
                    item {
                        CafeMapCard(
                            city = city,
                            cafes = results.sortedBy { it.distanceMeters ?: Int.MAX_VALUE },
                            title = "Peta hasil",
                            onOpenCafe = onOpenCafe,
                            onOpenFullMap = { showFullMap = true },
                            modifier = Modifier.padding(horizontal = 20.dp),
                            height = 176.dp,
                            maxMarkers = 30,
                            userLocation = appState.userLocation,
                        )
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
                            text = if (filters.hasExplicitCriteria) {
                                "${filters.explicitCriteriaCount} filter aktif · ${results.size} spot"
                            } else {
                                "${results.size} spot di ${city.name}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        if (filters.hasExplicitCriteria) {
                            TextButton(
                                onClick = { viewModel.setExploreFilters(filters.withoutCriteria()) },
                            ) { Text("Reset") }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        CarifeSectionHeader(
                            title = "Hasil",
                            supporting = "Diurutkan: ${filters.sort.label.lowercase()}",
                        )
                        SortSelector(
                            current = filters.sort,
                            onSelect = { viewModel.setExploreFilters(filters.copy(sort = it)) },
                        )
                    }
                }

                when {
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
                            now = now,
                            onOpen = { onOpenCafe(cafe.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(cafe.id) },
                            onToggleCompare = { viewModel.toggleCompare(cafe.id) },
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            } else {
                when {
                    catalog.city != null && !catalog.isRefreshing -> item {
                        CarifeEmptyState(
                            title = "Belum ada data kafe",
                            message = catalog.errorMessage ?: "Katalog ${city.name} belum berisi kafe.",
                            primaryLabel = if (BuildConfig.OFFLINE_ONLY) "Kembali ke Home" else "Muat ulang",
                            onPrimary = if (BuildConfig.OFFLINE_ONLY) onOpenHome else viewModel::retry,
                        )
                    }

                    else -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CircularProgressIndicator(color = CarifePrimary)
                                Text(
                                    "Memuat kafe di ${city.name}…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (compareIds.size >= 2) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CarifeSurfaceContainerLowest,
                border = BorderStroke(1.dp, CarifeOutlineVariant),
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
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
                            imageVector = Icons.Outlined.CompareArrows,
                            contentDescription = null,
                            tint = CarifePrimary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(
                            text = "${compareIds.size} kafe dipilih",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Bandingkan 2–3 kafe sekaligus",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(
                        onClick = onOpenCompare,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "Bandingkan",
                            style = MaterialTheme.typography.labelMedium,
                            color = CarifeOnPrimary,
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            FilterPanel(
                filters = filters,
                candidates = catalog.cafes,
                resultCount = results.size,
                onChange = { viewModel.setExploreFilters(it) },
                onReset = { viewModel.setExploreFilters(filters.withoutCriteria()) },
                onDone = {
                    showFilterSheet = false
                    listState.requestScrollToItem(RESULTS_HEADING_INDEX)
                },
            )
        }
    }

    if (showFullMap) {
        FullMapDialog(
            city = city,
            cafes = results,
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
private fun SortSelector(current: SortOption, onSelect: (SortOption) -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CarifeSurfaceContainerLow,
        border = BorderStroke(1.dp, CarifeOutlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            SortOption.entries.forEach { option ->
                val selected = current == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) CarifeSurfaceContainerLowest else Color.Transparent)
                        .then(
                            if (selected) {
                                Modifier.clip(RoundedCornerShape(9.dp))
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onSelect(option) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when (option) {
                            SortOption.MATCH -> "Kecocokan"
                            SortOption.NEAREST -> "Terdekat"
                            SortOption.CHEAPEST -> "Termurah"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
    if (hasKeyword && filters.hasExplicitCriteria) {
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
