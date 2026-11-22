package com.carife.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.UserPreferences
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
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
) {
    fun toUserPreferences(): UserPreferences = UserPreferences(
        budget = BudgetBucket.entries.firstOrNull { it.name == budget } ?: BudgetBucket.ANY,
        wifi = WifiRequirement.entries.firstOrNull { it.name == wifi } ?: WifiRequirement.ANY,
        zone = ZonePreference.entries.firstOrNull { it.name == zone } ?: ZonePreference.ANY,
        amenities = amenities.mapNotNull { name -> Amenity.entries.firstOrNull { it.name == name } }.toSet(),
        radius = RadiusPreference.entries.firstOrNull { it.name == radius } ?: RadiusPreference.ANY,
    )

    companion object {
        fun from(prefs: UserPreferences, favorites: List<String>, cityId: String) = StoredPreferences(
            favorites = favorites,
            selectedCityId = cityId,
            budget = prefs.budget.name,
            wifi = prefs.wifi.name,
            zone = prefs.zone.name,
            amenities = prefs.amenities.map { it.name },
            radius = prefs.radius.name,
            hasSavedPreferences = true,
        )
    }
}

class PreferencesStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("carife_state")

    val flow: Flow<StoredPreferences> = context.dataStore.data.map { prefs ->
        val raw = prefs[key]
        if (raw.isNullOrBlank()) {
            StoredPreferences()
        } else {
            runCatching { json.decodeFromString<StoredPreferences>(raw) }.getOrDefault(StoredPreferences())
        }
    }

    suspend fun save(value: StoredPreferences) {
        context.dataStore.edit { prefs -> prefs[key] = json.encodeToString(value) }
    }

    suspend fun toggleFavorite(cafeId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[key]?.let {
                runCatching { json.decodeFromString<StoredPreferences>(it) }.getOrNull()
            } ?: StoredPreferences()
            val favorites = current.favorites.toMutableList()
            if (favorites.contains(cafeId)) favorites.remove(cafeId) else favorites.add(cafeId)
            prefs[key] = json.encodeToString(current.copy(favorites = favorites))
        }
    }

    suspend fun selectCity(cityId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[key]?.let {
                runCatching { json.decodeFromString<StoredPreferences>(it) }.getOrNull()
            } ?: StoredPreferences()
            prefs[key] = json.encodeToString(current.copy(selectedCityId = cityId))
        }
    }
}
