package com.carife.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carife.app.CarifeApp
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.City
import com.carife.app.core.model.ExploreFilters
import com.carife.app.core.model.UserPreferences
import com.carife.app.data.CafeRepository
import com.carife.app.data.local.StoredPreferences
import com.carife.app.domain.MatchScorer
import com.carife.app.location.LocationProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val MAX_COMPARE_SELECTION = 3

data class AppState(
    val selectedCity: City,
    val favorites: Set<String>,
    val compareIds: List<String>,
    val preferences: UserPreferences,
    val hasSavedPreferences: Boolean,
    val userLocation: LocationProvider.UserLocation?,
)

data class UiMessage(val text: String, val id: Long = System.nanoTime())

class CarifeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as CarifeApp).container

    val cityRepository = container.cityRepository
    val preferencesStore = container.preferencesStore
    val cafeRepository = container.cafeRepository
    val locationProvider = container.locationProvider

    val catalog: StateFlow<CafeRepository.CatalogState> = cafeRepository.state

    val storedPreferences: StateFlow<StoredPreferences> = preferencesStore.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoredPreferences())

    private val _compareIds = MutableStateFlow<List<String>>(emptyList())
    val compareIds: StateFlow<List<String>> = _compareIds

    private val _exploreFilters = MutableStateFlow(ExploreFilters())
    val exploreFilters: StateFlow<ExploreFilters> = _exploreFilters

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    private val _requestFilterPanel = MutableStateFlow<Long?>(null)
    val requestFilterPanel: StateFlow<Long?> = _requestFilterPanel

    val appState: StateFlow<AppState> = combine(
        storedPreferences,
        _compareIds,
        locationProvider.location,
    ) { prefs, compare, location ->
        val city = cityRepository.byId(prefs.selectedCityId) ?: cityRepository.defaultCity
        AppState(
            selectedCity = city,
            favorites = prefs.favorites.toSet(),
            compareIds = compare,
            preferences = if (prefs.hasSavedPreferences) prefs.toUserPreferences() else UserPreferences(),
            hasSavedPreferences = prefs.hasSavedPreferences,
            userLocation = location,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppState(
            selectedCity = cityRepository.defaultCity,
            favorites = emptySet(),
            compareIds = emptyList(),
            preferences = UserPreferences(),
            hasSavedPreferences = false,
            userLocation = null,
        ),
    )

    init {
        viewModelScope.launch {
            storedPreferences.collect { prefs ->
                val city = cityRepository.byId(prefs.selectedCityId) ?: cityRepository.defaultCity
                cafeRepository.load(
                    city = city,
                    userLat = locationProvider.location.value?.lat,
                    userLon = locationProvider.location.value?.lon,
                )
            }
        }
        viewModelScope.launch {
            locationProvider.location.collect { location ->
                if (location != null) {
                    cafeRepository.updateUserLocation(location.lat, location.lon)
                }
            }
        }
        if (locationProvider.hasPermission()) {
            locationProvider.start()
        }
    }

    fun requestLocationPermissionFlow() {
        if (locationProvider.hasPermission()) locationProvider.start()
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            locationProvider.start()
        } else {
            cafeRepository.updateUserLocation(null, null)
        }
    }

    fun selectCity(cityId: String) {
        viewModelScope.launch {
            preferencesStore.selectCity(cityId)
            val city = cityRepository.byId(cityId) ?: return@launch
            cafeRepository.load(
                city = city,
                userLat = locationProvider.location.value?.lat,
                userLon = locationProvider.location.value?.lon,
            )
        }
    }

    fun retry() {
        viewModelScope.launch {
            val city = appState.value.selectedCity
            cafeRepository.refresh(
                city = city,
                userLat = locationProvider.location.value?.lat,
                userLon = locationProvider.location.value?.lon,
            )
        }
    }

    fun toggleFavorite(cafeId: String) {
        viewModelScope.launch { preferencesStore.toggleFavorite(cafeId) }
    }

    fun toggleCompare(cafeId: String) {
        val current = _compareIds.value
        when {
            current.contains(cafeId) -> _compareIds.value = current - cafeId
            current.size >= MAX_COMPARE_SELECTION ->
                emitMessage("Maksimal $MAX_COMPARE_SELECTION kafe. Hapus satu dulu untuk mengganti.")

            else -> _compareIds.value = current + cafeId
        }
    }

    fun clearCompare() {
        _compareIds.value = emptyList()
    }

    fun setExploreFilters(filters: ExploreFilters) {
        _exploreFilters.value = filters
    }

    fun openFilterPanel() {
        _requestFilterPanel.value = System.currentTimeMillis()
    }

    fun consumeFilterPanelRequest() {
        _requestFilterPanel.value = null
    }

    fun savePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            val current = storedPreferences.value
            preferencesStore.save(
                StoredPreferences.from(preferences, current.favorites, current.selectedCityId)
            )
            emitMessage("Preferensi tersimpan. Skor rekomendasi diperbarui.")
        }
    }

    fun matchScores(cafes: List<Cafe>): Map<String, Int> {
        val prefs = appState.value.preferences
        if (!appState.value.hasSavedPreferences) return emptyMap()
        return cafes.mapNotNull { cafe -> MatchScorer.score(cafe, prefs)?.let { cafe.id to it } }.toMap()
    }

    fun emitMessage(text: String) {
        _messages.tryEmit(UiMessage(text))
    }
}
