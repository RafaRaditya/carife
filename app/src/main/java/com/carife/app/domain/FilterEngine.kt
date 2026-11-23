package com.carife.app.domain

import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.ExploreFilters
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.SmokingZone
import com.carife.app.core.model.SortOption
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import java.time.Instant

object FilterEngine {

    private val whitespace = Regex("\\s+")
    private val wifiSpelling = Regex("\\bwi[\\s-]*fi\\b")

    fun apply(
        cafes: List<Cafe>,
        filters: ExploreFilters,
        now: Instant = Instant.now(),
        matchScores: Map<String, Int> = emptyMap(),
    ): List<Cafe> {
        val filtered = cafes.filter { matches(it, filters, now) }
        val byDistance = compareBy<Cafe>({ it.distanceMeters ?: Int.MAX_VALUE }, { it.name.lowercase() })
        return when (filters.sort) {
            SortOption.NEAREST -> filtered.sortedWith(byDistance)

            SortOption.CHEAPEST -> filtered.sortedWith(
                compareBy({ it.priceMin ?: Int.MAX_VALUE }, { it.distanceMeters ?: Int.MAX_VALUE })
            )

            SortOption.MATCH -> filtered.sortedWith(
                compareByDescending<Cafe> { matchScores[it.id] ?: -1 }.then(byDistance)
            )
        }
    }

    fun matches(cafe: Cafe, filters: ExploreFilters, now: Instant = Instant.now()): Boolean {
        if (!matchesQuery(cafe, filters.query)) return false

        filters.budget?.let { bucket -> if (!matchesBudget(cafe, bucket)) return false }
        filters.wifi?.let { requirement -> if (!matchesWifi(cafe, requirement)) return false }
        filters.zone?.let { zone -> if (!matchesZone(cafe, zone)) return false }
        filters.distance?.let { radius -> if (!matchesRadius(cafe, radius)) return false }
        filters.noise?.let { noise -> if (cafe.noiseLevel != noise) return false }
        filters.minOutlets?.let { minimum ->
            val outlets = cafe.outletCount ?: return false
            if (outlets < minimum) return false
        }
        filters.priceCap?.let { cap ->
            val price = cafe.priceMin ?: return false
            if (price >= cap) return false
        }

        if (filters.amenities.isNotEmpty()) {
            if (!filters.amenities.all(cafe::hasAmenity)) return false
        }

        if (filters.openNowOnly) {
            if (OpeningHours.isOpenNow(cafe, now) != true) return false
        }

        return true
    }

    private fun matchesQuery(cafe: Cafe, rawQuery: String): Boolean {
        val query = normalize(rawQuery)
        if (query.isEmpty()) return true
        val haystack = buildString {
            append(cafe.name).append(' ')
            cafe.area?.let { append(it).append(' ') }
            cafe.address?.let { append(it).append(' ') }
            cafe.amenities.forEach { append(it.label).append(' ') }
            if (cafe.hasWifi) append("wifi ")
            if ((cafe.outletCount ?: 0) > 0) append("colokan ")
        }
        return normalize(haystack).contains(query)
    }

    private fun matchesBudget(cafe: Cafe, bucket: BudgetBucket): Boolean {
        if (bucket == BudgetBucket.ANY) return true
        val price = cafe.priceMin ?: return false
        val min = bucket.min
        val max = bucket.max
        if (min != null && price < min) return false
        if (max != null && price > max) return false
        return true
    }

    private fun matchesWifi(cafe: Cafe, requirement: WifiRequirement): Boolean = when (requirement) {
        WifiRequirement.ANY -> true
        WifiRequirement.STANDARD -> cafe.hasWifi
        WifiRequirement.MIN_30 -> cafe.wifiMbps?.let { it >= 30 } ?: false
        WifiRequirement.OVER_30 -> cafe.wifiMbps?.let { it > 30 } ?: false
        WifiRequirement.OVER_50 -> cafe.wifiMbps?.let { it > 50 } ?: false
    }

    private fun matchesZone(cafe: Cafe, zone: ZonePreference): Boolean = when (zone) {
        ZonePreference.ANY -> true
        ZonePreference.NON_SMOKING_AC -> cafe.smokingZone == SmokingZone.FULLY_NON_SMOKING &&
            cafe.amenities.contains(Amenity.AIR_CONDITIONED)

        ZonePreference.NON_SMOKING -> cafe.smokingZone != null &&
            cafe.smokingZone != SmokingZone.SMOKING_ALLOWED

        ZonePreference.SEPARATE -> cafe.smokingZone == SmokingZone.SEPARATE_AREA ||
            cafe.smokingZone == SmokingZone.SMOKING_ALLOWED
    }

    private fun matchesRadius(cafe: Cafe, radius: RadiusPreference): Boolean {
        if (radius == RadiusPreference.ANY) return true
        val distance = cafe.distanceMeters ?: return false
        if (distance < radius.minMeters) return false
        val max = radius.maxMeters ?: return true
        return distance <= max
    }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace(wifiSpelling, "wifi").replace(whitespace, " ")
}
