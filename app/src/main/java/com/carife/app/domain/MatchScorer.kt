package com.carife.app.domain

import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.SmokingZone
import com.carife.app.core.model.UserPreferences
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import kotlin.math.roundToInt

object MatchScorer {

    fun score(cafe: Cafe, preferences: UserPreferences): Int? {
        if (!preferences.hasActiveCriteria) return null

        val results = mutableListOf<Boolean>()

        if (preferences.budget != BudgetBucket.ANY) {
            val price = cafe.priceMin
            val min = preferences.budget.min
            val max = preferences.budget.max
            results.add(
                price != null &&
                    (min == null || price >= min) &&
                    (max == null || price <= max)
            )
        }

        when (preferences.wifi) {
            WifiRequirement.ANY -> Unit
            WifiRequirement.STANDARD -> results.add(cafe.amenities.contains(Amenity.WIFI))
            WifiRequirement.MIN_30 -> results.add(cafe.wifiMbps?.let { it >= 30 } ?: false)
            WifiRequirement.OVER_30 -> results.add(cafe.wifiMbps?.let { it > 30 } ?: false)
            WifiRequirement.OVER_50 -> results.add(cafe.wifiMbps?.let { it > 50 } ?: false)
        }

        when (preferences.zone) {
            ZonePreference.ANY -> Unit
            ZonePreference.NON_SMOKING_AC -> results.add(
                cafe.smokingZone == SmokingZone.FULLY_NON_SMOKING &&
                    cafe.amenities.contains(Amenity.AIR_CONDITIONED)
            )

            ZonePreference.NON_SMOKING -> results.add(
                cafe.smokingZone != null && cafe.smokingZone != SmokingZone.SMOKING_ALLOWED
            )

            ZonePreference.SEPARATE -> results.add(
                cafe.smokingZone == SmokingZone.SEPARATE_AREA ||
                    cafe.smokingZone == SmokingZone.SMOKING_ALLOWED
            )
        }

        if (preferences.radius != RadiusPreference.ANY) {
            val distance = cafe.distanceMeters
            val max = preferences.radius.maxMeters
            results.add(
                distance != null &&
                    distance >= preferences.radius.minMeters &&
                    (max == null || distance <= max)
            )
        }

        for (amenity in preferences.amenities) {
            results.add(cafe.amenities.contains(amenity))
        }

        if (results.isEmpty()) return null

        val fulfilled = results.count { it }
        return ((fulfilled.toDouble() / results.size) * 100).roundToInt()
    }

    fun rank(cafes: List<Cafe>, preferences: UserPreferences): List<Pair<Cafe, Int>> =
        cafes.mapNotNull { cafe -> score(cafe, preferences)?.let { cafe to it } }
            .sortedWith(
                compareByDescending<Pair<Cafe, Int>> { it.second }
                    .thenBy { it.first.distanceMeters ?: Int.MAX_VALUE }
                    .thenBy { it.first.name.lowercase() }
            )

    fun bestMatch(cafes: List<Cafe>, preferences: UserPreferences): Pair<Cafe, Int>? =
        rank(cafes, preferences).firstOrNull()?.takeIf { it.second > 0 }
}
