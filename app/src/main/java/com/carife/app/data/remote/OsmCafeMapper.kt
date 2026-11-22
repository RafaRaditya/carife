package com.carife.app.data.remote

import com.carife.app.core.model.Amenity
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.SmokingZone
import com.carife.app.domain.DistanceUtil
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object OsmCafeMapper {

    private val json = Json { ignoreUnknownKeys = true }

    fun mapResponse(body: String, cityId: String): List<Cafe> =
        mapCompleteResponse(body, cityId).orEmpty()

    internal fun mapCompleteResponse(body: String, cityId: String): List<Cafe>? {
        val root = runCatching { json.parseToJsonElement(body) as? JsonObject }.getOrNull() ?: return null
        if ("remark" in root) return null
        val elements = root["elements"] as? JsonArray ?: return null
        return dedupe(elements.mapNotNull { mapElement(it, cityId) })
    }

    fun mapElement(element: JsonElement, cityId: String): Cafe? {
        val obj = element as? JsonObject ?: return null
        val tags = obj["tags"] as? JsonObject ?: return null

        val name = tags.text("name") ?: tags.text("brand") ?: return null
        if (name.isBlank()) return null

        val type = obj.text("type") ?: "node"
        val elementId = obj.text("id") ?: return null
        val center = obj["center"] as? JsonObject
        val lat = obj.double("lat") ?: center?.double("lat") ?: return null
        val lon = obj.double("lon") ?: center?.double("lon") ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null

        val street = tags.text("addr:street")
        val houseNumber = tags.text("addr:housenumber")
        val area = tags.text("addr:suburb") ?: tags.text("addr:city") ?: tags.text("addr:district")

        val address = buildString {
            if (street != null) {
                append(street)
                if (houseNumber != null) append(" No. ").append(houseNumber)
            }
        }.ifBlank { null }

        val amenities = buildSet {
            val internet = tags.text("internet_access")
            if (internet != null && internet != "no") add(Amenity.WIFI)
            if (tags.text("outdoor_seating") == "yes") add(Amenity.OUTDOOR_SEATING)
            if (tags.text("wheelchair") == "yes") add(Amenity.WHEELCHAIR)
            if (tags.text("air_conditioning") == "yes") add(Amenity.AIR_CONDITIONED)
        }

        val smokingZone = when (tags.text("smoking")) {
            "no" -> SmokingZone.FULLY_NON_SMOKING
            "isolated", "separated", "outside", "dedicated" -> SmokingZone.SEPARATE_AREA
            "yes" -> SmokingZone.SMOKING_ALLOWED
            else -> null
        }

        return Cafe(
            id = "osm:$type/$elementId",
            name = name.trim(),
            cityId = cityId,
            area = area?.trim(),
            address = address,
            lat = lat,
            lon = lon,
            amenities = amenities,
            smokingZone = smokingZone,
            openingHoursRaw = tags.text("opening_hours"),
            website = tags.text("website") ?: tags.text("contact:website"),
        )
    }

    fun dedupe(cafes: List<Cafe>): List<Cafe> {
        val kept = mutableListOf<Cafe>()
        for (cafe in cafes) {
            val duplicate = kept.any { existing ->
                existing.name.equals(cafe.name, ignoreCase = true) &&
                    DistanceUtil.haversineMeters(existing.lat, existing.lon, cafe.lat, cafe.lon) < 50
            }
            if (!duplicate) kept.add(cafe)
        }
        return kept
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonElement)?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }

    private fun JsonObject.double(key: String): Double? =
        (this[key] as? JsonElement)?.let { runCatching { it.jsonPrimitive.content.toDouble() }.getOrNull() }
}
