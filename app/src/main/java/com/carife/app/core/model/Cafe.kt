package com.carife.app.core.model

data class Cafe(
    val id: String,
    val name: String,
    val cityId: String,
    val area: String? = null,
    val address: String? = null,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Int? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val priceMin: Int? = null,
    val priceMax: Int? = null,
    val wifiMbps: Int? = null,
    val outlets: String? = null,
    val outletCount: Int? = null,
    val smokingZone: SmokingZone? = null,
    val amenities: Set<Amenity> = emptySet(),
    val openingHoursRaw: String? = null,
    val website: String? = null,
    val noiseLevel: NoiseLevel? = null,
    val densityLevel: DensityLevel? = null,
    val densityPercent: Int? = null,
    val photoCount: Int? = null,
    val menu: List<MenuItem> = emptyList(),
    val reviews: List<ReviewSample> = emptyList(),
    val isDemoData: Boolean = false,
) {
    val hasWifi: Boolean
        get() = Amenity.WIFI in amenities || (wifiMbps ?: 0) > 0

    fun hasAmenity(amenity: Amenity): Boolean =
        if (amenity == Amenity.WIFI) hasWifi else amenity in amenities
}
