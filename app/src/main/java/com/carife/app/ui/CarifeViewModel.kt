package com.carife.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.carife.app.CarifeApp
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.CheckIn
import com.carife.app.core.model.City
import com.carife.app.core.model.ExploreFilters
import com.carife.app.core.model.StudyRoom
import com.carife.app.core.model.UserPreferences
import com.carife.app.core.model.UserProfile
import com.carife.app.data.CafeRepository
import com.carife.app.data.DemoFriendsData
import com.carife.app.data.local.StoredPreferences
import com.carife.app.location.LocationProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Instant

const val MAX_COMPARE_SELECTION = 3

enum class ExploreEntry { SEARCH, FILTERS, QUICK_FILTER }

data class AppState(
    val selectedCity: City,
    val favorites: Set<String>,
    val compareIds: List<String>,
    val preferences: UserPreferences,
    val hasSavedPreferences: Boolean,
    val userLocation: LocationProvider.UserLocation?,
    val profile: UserProfile,
    val friendIds: Set<String>,
    val joinedSpotFriendIds: Set<String>,
    val rooms: List<StudyRoom>,
    val joinedRoomIds: Set<String>,
    val checkIn: CheckIn?,
)

data class UiMessage(val text: String, val id: Long = System.nanoTime())

class CarifeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as CarifeApp).container

    val cityRepository = container.cityRepository
    val preferencesStore = container.preferencesStore
    val profilePhotoStore = container.profilePhotoStore
    val cafeRepository = container.cafeRepository
    val locationProvider = container.locationProvider

    val catalog: StateFlow<CafeRepository.CatalogState> = cafeRepository.state
    val openingHoursTime: StateFlow<Instant> = flow {
        while (true) {
            val now = Instant.now()
            emit(now)
            delay(60_000L - Math.floorMod(now.toEpochMilli(), 60_000L))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Instant.now())

    private val storedPreferences: StateFlow<StoredPreferences> = preferencesStore.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoredPreferences())

    private val _compareIds = MutableStateFlow<List<String>>(emptyList())
    val compareIds: StateFlow<List<String>> = combine(_compareIds, catalog) { ids, current ->
        availableCompareIds(ids, current.cafes)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private var catalogJob: Job? = null

    private val _exploreFilters = MutableStateFlow(ExploreFilters())
    val exploreFilters: StateFlow<ExploreFilters> = _exploreFilters

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 4)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    private val _exploreEntry = MutableStateFlow<ExploreEntry?>(null)
    val exploreEntry: StateFlow<ExploreEntry?> = _exploreEntry

    val appState: StateFlow<AppState> = combine(
        storedPreferences,
        compareIds,
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
            profile = prefs.profile,
            friendIds = prefs.addedFriendIds.toSet(),
            joinedSpotFriendIds = prefs.joinedSpotFriendIds.toSet(),
            rooms = prefs.rooms,
            joinedRoomIds = prefs.joinedRoomIds.toSet(),
            checkIn = prefs.checkIn,
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
            profile = UserProfile(),
            friendIds = DemoFriendsData.defaultFriendIds.toSet(),
            joinedSpotFriendIds = emptySet(),
            rooms = emptyList(),
            joinedRoomIds = emptySet(),
            checkIn = null,
        ),
    )

    init {
        viewModelScope.launch {
            storedPreferences
                .map { it.selectedCityId }
                .distinctUntilChanged()
                .collect { cityId ->
                    val city = cityRepository.byId(cityId) ?: cityRepository.defaultCity
                    if (catalog.value.city?.id != city.id) clearCompare()
                    loadCatalog(city)
                }
        }
        viewModelScope.launch {
            locationProvider.location.collect { location ->
                cafeRepository.updateUserLocation(location?.lat, location?.lon)
            }
        }
        if (locationProvider.hasPermission()) {
            locationProvider.start()
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            locationProvider.start()
        } else {
            locationProvider.stop()
        }
    }

    override fun onCleared() {
        locationProvider.stop()
    }

    fun selectCity(cityId: String) {
        if (cityRepository.byId(cityId) == null) return
        viewModelScope.launch {
            try {
                preferencesStore.selectCity(cityId)
            } catch (_: IOException) {
                emitMessage("Kota belum bisa diganti. Coba lagi.")
            }
        }
    }

    fun retry() {
        val city = cityRepository.byId(storedPreferences.value.selectedCityId) ?: cityRepository.defaultCity
        loadCatalog(city, forceRefresh = true)
    }

    private fun loadCatalog(city: City, forceRefresh: Boolean = false) {
        catalogJob?.cancel()
        catalogJob = viewModelScope.launch {
            cafeRepository.load(city, forceRefresh = forceRefresh)
        }
    }

    fun toggleFavorite(cafeId: String) {
        viewModelScope.launch {
            try {
                preferencesStore.toggleFavorite(cafeId)
            } catch (_: IOException) {
                emitMessage("Favorit gagal diperbarui. Coba lagi.")
            }
        }
    }

    fun toggleCompare(cafeId: String) {
        val cafes = catalog.value.cafes
        if (cafes.none { it.id == cafeId }) return
        val current = availableCompareIds(_compareIds.value, cafes)
        when {
            current.contains(cafeId) -> _compareIds.value = current - cafeId
            current.size >= MAX_COMPARE_SELECTION ->
                emitMessage("Maksimal $MAX_COMPARE_SELECTION kafe. Hapus satu dulu untuk mengganti.")

            else -> _compareIds.value = current + cafeId
        }
    }

    private fun availableCompareIds(ids: List<String>, cafes: List<Cafe>): List<String> =
        ids.filter { id -> cafes.any { it.id == id } }

    fun clearCompare() {
        _compareIds.value = emptyList()
    }

    fun setExploreFilters(filters: ExploreFilters) {
        _exploreFilters.value = filters
    }

    fun requestExploreEntry(entry: ExploreEntry) {
        _exploreEntry.value = entry
    }

    fun consumeExploreEntry() {
        _exploreEntry.value = null
    }

    suspend fun savePreferences(preferences: UserPreferences): Boolean =
        try {
            preferencesStore.savePreferences(preferences)
            emitMessage("Preferensi tersimpan. Skor rekomendasi diperbarui.")
            true
        } catch (_: IOException) {
            emitMessage("Preferensi gagal disimpan. Coba simpan lagi.")
            false
        }

    fun addFriend(friendId: String) {
        viewModelScope.launch {
            try {
                preferencesStore.addFriend(friendId)
                emitMessage("${DemoFriendsData.person(friendId)?.name ?: "Teman"} ditambahkan.")
            } catch (_: IOException) {
                emitMessage("Teman gagal ditambahkan. Coba lagi.")
            }
        }
    }

    fun removeFriend(friendId: String) {
        viewModelScope.launch {
            try {
                preferencesStore.removeFriend(friendId)
                emitMessage("${DemoFriendsData.person(friendId)?.name ?: "Teman"} dihapus dari daftar.")
            } catch (_: IOException) {
                emitMessage("Teman gagal dihapus. Coba lagi.")
            }
        }
    }

    fun toggleJoinSpot(friendId: String) {
        val joining = friendId !in storedPreferences.value.joinedSpotFriendIds
        viewModelScope.launch {
            try {
                preferencesStore.toggleJoinedSpot(friendId)
                val name = DemoFriendsData.person(friendId)?.name ?: "teman"
                emitMessage(if (joining) "Kamu gabung nongkrong bareng $name." else "Gabung dibatalkan.")
            } catch (_: IOException) {
                emitMessage("Status gabung gagal diperbarui. Coba lagi.")
            }
        }
    }

    fun createRoom(
        cafe: Cafe,
        title: String,
        schedule: String,
        note: String,
        invitedFriendIds: List<String>,
        capacity: Int,
    ) {
        val room = StudyRoom(
            id = "room-${System.currentTimeMillis()}",
            cafeId = cafe.id,
            cafeName = cafe.name,
            title = title.trim(),
            schedule = schedule.trim(),
            note = note.trim(),
            invitedFriendIds = invitedFriendIds,
            capacity = capacity,
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            try {
                preferencesStore.createRoom(room)
                emitMessage("Room \"${room.title}\" dibuat. Bagikan undangannya ke temanmu.")
            } catch (_: IOException) {
                emitMessage("Room gagal dibuat. Coba lagi.")
            }
        }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            try {
                preferencesStore.deleteRoom(roomId)
                emitMessage("Room ditutup.")
            } catch (_: IOException) {
                emitMessage("Room gagal ditutup. Coba lagi.")
            }
        }
    }

    fun checkIn(cafe: Cafe, floor: String?, note: String?) {
        val checkIn = CheckIn(
            cafeId = cafe.id,
            cafeName = cafe.name,
            floor = floor?.takeIf { it.isNotBlank() },
            note = note?.takeIf { it.isNotBlank() },
            createdAtEpochMillis = System.currentTimeMillis(),
        )
        viewModelScope.launch {
            try {
                preferencesStore.saveCheckIn(checkIn)
                emitMessage("Check-in tersimpan di ${cafe.name}.")
            } catch (_: IOException) {
                emitMessage("Check-in gagal disimpan. Coba lagi.")
            }
        }
    }

    fun checkOut() {
        viewModelScope.launch {
            try {
                preferencesStore.clearCheckIn()
                emitMessage("Kamu sudah check-out.")
            } catch (_: IOException) {
                emitMessage("Check-out gagal. Coba lagi.")
            }
        }
    }

    fun updateRoomInvites(roomId: String, invitedFriendIds: List<String>) {
        viewModelScope.launch {
            try {
                preferencesStore.updateRoomInvites(roomId, invitedFriendIds)
                emitMessage("Daftar teman di room diperbarui.")
            } catch (_: IOException) {
                emitMessage("Daftar undangan gagal diperbarui. Coba lagi.")
            }
        }
    }

    fun updateRoomDetails(roomId: String, title: String, schedule: String) {
        viewModelScope.launch {
            try {
                preferencesStore.updateRoomDetails(roomId, title, schedule)
                emitMessage("Detail room diperbarui.")
            } catch (_: IOException) {
                emitMessage("Detail room gagal diperbarui. Coba lagi.")
            }
        }
    }

    fun updateRoomCapacity(roomId: String, capacity: Int) {
        viewModelScope.launch {
            try {
                preferencesStore.updateRoomCapacity(roomId, capacity)
                emitMessage("Kuota room diperbarui.")
            } catch (_: IOException) {
                emitMessage("Kuota room gagal diperbarui. Coba lagi.")
            }
        }
    }

    fun toggleJoinRoom(room: StudyRoom) {
        val joined = room.id in storedPreferences.value.joinedRoomIds
        val participantsAfterJoin = 1 + room.invitedFriendIds.size + if (joined) 0 else 1
        if (!joined && participantsAfterJoin > room.capacity) {
            emitMessage("Room sudah penuh.")
            return
        }
        viewModelScope.launch {
            try {
                preferencesStore.toggleJoinedRoom(room.id)
                emitMessage(
                    if (joined) {
                        "Kamu keluar dari room \"${room.title}\"."
                    } else {
                        "Kamu gabung ke room \"${room.title}\"."
                    }
                )
            } catch (_: IOException) {
                emitMessage("Status room gagal diperbarui. Coba lagi.")
            }
        }
    }

    suspend fun saveAccount(
        profile: UserProfile,
        cityId: String,
        photoUri: Uri?,
        removePhoto: Boolean,
    ): Boolean {
        return try {
            var updated = profile.copy(
                displayName = profile.displayName.trim(),
                tagline = profile.tagline.trim(),
                status = profile.status.trim(),
            )
            when {
                photoUri != null -> {
                    if (!profilePhotoStore.save(photoUri)) {
                        emitMessage("Foto profil gagal diproses. Coba foto lain.")
                        return false
                    }
                    updated = updated.copy(
                        hasPhoto = true,
                        photoUpdatedAtEpochMillis = System.currentTimeMillis(),
                    )
                }

                removePhoto -> {
                    profilePhotoStore.delete()
                    updated = updated.copy(
                        hasPhoto = false,
                        photoUpdatedAtEpochMillis = System.currentTimeMillis(),
                    )
                }
            }
            preferencesStore.saveAccount(updated, cityId)
            emitMessage("Profil tersimpan.")
            true
        } catch (_: IOException) {
            emitMessage("Profil gagal disimpan. Coba lagi.")
            false
        }
    }

    fun emitMessage(text: String) {
        _messages.tryEmit(UiMessage(text))
    }
}
