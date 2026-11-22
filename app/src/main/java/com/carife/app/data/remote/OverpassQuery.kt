package com.carife.app.data.remote

import com.carife.app.core.model.City

object OverpassQuery {

    fun cafesAround(city: City): String = cafesAround(city.lat, city.lon, city.radiusMeters)

    fun cafesAround(lat: Double, lon: Double, radiusMeters: Int): String = buildString {
        append("[out:json][timeout:60];")
        append("nwr[\"amenity\"=\"cafe\"](around:")
        append(radiusMeters)
        append(',')
        append(formatCoord(lat))
        append(',')
        append(formatCoord(lon))
        append(");")
        append("out center tags;")
    }

    private fun formatCoord(value: Double): String =
        String.format(java.util.Locale.US, "%.6f", value)
}
