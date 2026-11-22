package com.carife.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.CheckIn
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.StudyRoom
import com.carife.app.core.model.UserPreferences
import com.carife.app.core.model.UserProfile
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import com.carife.app.data.DemoFriendsData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "carife")

@Serializable
data class StoredPreferences(
    val favorites: List<String> = emptyList(),
    val selectedCityId: String = "bekasi",
    val budget: String = BudgetBucket.ANY.name,
    val wifi: String = WifiRequirement.ANY.name,
    val zone: String = ZonePreference.ANY.name,
    val amenities: List<String> = emptyList(),
    val radius: String = RadiusPreference.ANY.name,
    val hasSavedPreferences: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val addedFriendIds: List<String> = DemoFriendsData.defaultFriendIds,
    val joinedSpotFriendIds: List<String> = emptyList(),
    val rooms: List<StudyRoom> = emptyList(),
    val joinedRoomIds: List<String> = emptyList(),
    val checkIn: CheckIn? = null,
) {
    fun toUserPreferences(): UserPreferences = UserPreferences(
        budget = BudgetBucket.entries.firstOrNull { it.name == budget } ?: BudgetBucket.ANY,
        wifi = WifiRequirement.entries.firstOrNull { it.name == wifi } ?: WifiRequirement.ANY,
        zone = ZonePreference.entries.firstOrNull { it.name == zone } ?: ZonePreference.ANY,
        amenities = amenities.mapNotNull { name -> Amenity.entries.firstOrNull { it.name == name } }.toSet(),
        radius = RadiusPreference.entries.firstOrNull { it.name == radius } ?: RadiusPreference.ANY,
    )
}

class PreferencesStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("carife_state")

    val flow: Flow<StoredPreferences> = context.dataStore.data.map { prefs -> decode(prefs[key]) }

    suspend fun savePreferences(value: UserPreferences) = update { current ->
        current.withPreferences(value)
    }

    suspend fun saveAccount(profile: UserProfile, cityId: String) = update { current ->
        current.copy(profile = profile, selectedCityId = cityId)
    }

    suspend fun setProfilePhoto(hasPhoto: Boolean, updatedAtEpochMillis: Long) = update { current ->
        current.copy(
            profile = current.profile.copy(
                hasPhoto = hasPhoto,
                photoUpdatedAtEpochMillis = updatedAtEpochMillis,
            )
        )
    }

    suspend fun addFriend(friendId: String) = update { current ->
        if (friendId in current.addedFriendIds) {
            current
        } else {
            current.copy(addedFriendIds = current.addedFriendIds + friendId)
        }
    }

    suspend fun removeFriend(friendId: String) = update { current ->
        val hostedDemoRoomIds = DemoFriendsData.demoRooms
            .filter { room -> room.hostFriendId == friendId }
            .map { room -> room.id }
            .toSet()
        current.copy(
            addedFriendIds = current.addedFriendIds - friendId,
            joinedSpotFriendIds = current.joinedSpotFriendIds - friendId,
            joinedRoomIds = current.joinedRoomIds - hostedDemoRoomIds,
        )
    }

    suspend fun toggleJoinedSpot(friendId: String) = update { current ->
        val joined = current.joinedSpotFriendIds.toMutableSet()
        if (!joined.add(friendId)) joined.remove(friendId)
        current.copy(joinedSpotFriendIds = joined.toList())
    }

    suspend fun createRoom(room: StudyRoom) = update { current ->
        current.copy(rooms = current.rooms + room)
    }

    suspend fun deleteRoom(roomId: String) = update { current ->
        current.copy(
            rooms = current.rooms.filterNot { it.id == roomId },
            joinedRoomIds = current.joinedRoomIds - roomId,
        )
    }

    suspend fun toggleJoinedRoom(roomId: String) = update { current ->
        val joined = current.joinedRoomIds.toMutableSet()
        if (!joined.add(roomId)) joined.remove(roomId)
        current.copy(joinedRoomIds = joined.toList())
    }

    suspend fun saveCheckIn(checkIn: CheckIn) = update { current ->
        current.copy(checkIn = checkIn)
    }

    suspend fun clearCheckIn() = update { current ->
        current.copy(checkIn = null)
    }

    suspend fun updateRoomInvites(roomId: String, invitedFriendIds: List<String>) = updateRoom(roomId) { room ->
        room.copy(invitedFriendIds = invitedFriendIds)
    }

    suspend fun updateRoomDetails(roomId: String, title: String, schedule: String) = updateRoom(roomId) { room ->
        room.copy(title = title.trim(), schedule = schedule.trim())
    }

    suspend fun updateRoomCapacity(roomId: String, capacity: Int) = updateRoom(roomId) { room ->
        room.copy(capacity = capacity)
    }

    private suspend fun updateRoom(roomId: String, transform: (StudyRoom) -> StudyRoom) = update { current ->
        current.copy(
            rooms = current.rooms.map { room ->
                if (room.id == roomId) transform(room) else room
            }
        )
    }

    suspend fun toggleFavorite(cafeId: String) = update { current ->
        val favorites = current.favorites.toMutableList()
        if (favorites.contains(cafeId)) favorites.remove(cafeId) else favorites.add(cafeId)
        current.copy(favorites = favorites)
    }

    suspend fun selectCity(cityId: String) = update { current ->
        current.copy(selectedCityId = cityId)
    }

    private suspend fun update(transform: (StoredPreferences) -> StoredPreferences) {
        context.dataStore.edit { prefs ->
            prefs[key] = json.encodeToString(transform(decode(prefs[key])))
        }
    }

    private fun StoredPreferences.withPreferences(value: UserPreferences): StoredPreferences = copy(
        budget = value.budget.name,
        wifi = value.wifi.name,
        zone = value.zone.name,
        amenities = value.amenities.map { it.name },
        radius = value.radius.name,
        hasSavedPreferences = true,
    )

    private fun decode(raw: String?): StoredPreferences =
        raw?.let { runCatching { json.decodeFromString<StoredPreferences>(it) }.getOrNull() }
            ?: StoredPreferences()
}
