package com.carife.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class City(
    val id: String,
    val name: String,
    val province: String,
    val lat: Double,
    val lon: Double,
    val radiusMeters: Int,
)

enum class Amenity(val label: String) {
    WIFI("Wi-Fi"),
    OUTDOOR_SEATING("Outdoor"),
    WHEELCHAIR("Akses Kursi Roda"),
    AIR_CONDITIONED("AC"),
}

enum class SmokingZone(val label: String) {
    FULLY_NON_SMOKING("100% bebas asap"),
    SEPARATE_AREA("Ada area merokok terpisah"),
    SMOKING_ALLOWED("Boleh merokok"),
}
