package com.carife.app.domain

import com.carife.app.core.model.Cafe
import java.net.URLEncoder

object ShareTextBuilder {

    fun mapsUrl(cafe: Cafe): String {
        val query = buildString {
            append(cafe.name)
            cafe.address?.takeIf { it.isNotBlank() }?.let { append(", ").append(it) }
            append(" @").append(cafe.lat).append(',').append(cafe.lon)
        }
        return "https://www.google.com/maps/search/?api=1&query=${encode(query)}"
    }

    fun directionsUrl(cafe: Cafe): String {
        val destination = buildString {
            append(cafe.name)
            cafe.address?.takeIf { it.isNotBlank() }?.let { append(", ").append(it) }
        }
        return "https://www.google.com/maps/dir/?api=1&destination=${encode(destination)}"
    }

    fun shareMessage(cafe: Cafe): String = buildString {
        append("Nugas di ").append(cafe.name)
        cafe.address?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
        cafe.distanceMeters?.let { append("\n").append(DistanceUtil.formatDistance(it)).append(" dari lokasimu") }
        append("\n\n").append(mapsUrl(cafe))
        append("\n\nDibagikan lewat Carife")
    }

    fun inviteMessage(cafe: Cafe, note: String? = null, floor: String? = null): String = buildString {
        append("Ayo nugas bareng di ").append(cafe.name).append("!")
        floor?.takeIf { it.isNotBlank() }?.let { append("\nLantai: ").append(it) }
        note?.takeIf { it.isNotBlank() }?.let { append("\nCatatan: ").append(it) }
        cafe.address?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
        append("\n\n").append(mapsUrl(cafe))
        append("\n\nDikirim lewat Carife")
    }

    fun mySpotMessage(cafe: Cafe, note: String?, floor: String?): String = buildString {
        append("Aku lagi nugas di ").append(cafe.name)
        floor?.takeIf { it.isNotBlank() }?.let { append(", lantai ").append(it) }
        note?.takeIf { it.isNotBlank() }?.let { append("\n\"").append(it).append("\"") }
        append("\n\n").append(mapsUrl(cafe))
        append("\n\nDibagikan lewat Carife")
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
