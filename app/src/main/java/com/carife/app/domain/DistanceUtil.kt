package com.carife.app.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object DistanceUtil {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (EARTH_RADIUS_METERS * c).toInt()
    }

    fun formatDistance(meters: Int): String = when {
        meters < 1_000 -> "$meters m"
        else -> {
            val km = meters / 1000.0
            val rounded = Math.round(km * 10) / 10.0
            if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()} km" else "$rounded km"
        }
    }

    fun formatWalkingMinutes(meters: Int): Int = maxOf(1, Math.round(meters / 80f))
}
