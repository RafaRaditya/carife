package com.carife.app.domain

import com.carife.app.core.model.Cafe
import com.carife.app.core.model.StudyRoom
import java.net.URLEncoder

object ShareTextBuilder {

    fun mapsUrl(cafe: Cafe): String =
        "https://www.google.com/maps/search/?api=1&query=${coordinates(cafe)}"

    fun directionsUrl(cafe: Cafe): String =
        "https://www.google.com/maps/dir/?api=1&destination=${coordinates(cafe)}"

    fun shareMessage(cafe: Cafe): String = buildString {
        append("Nugas di ").append(cafe.name)
        cafe.address?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
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

    fun roomInviteMessage(room: StudyRoom, cafe: Cafe?): String = buildString {
        append("Ayo gabung room \"").append(room.title).append("\"")
        (cafe?.name ?: room.cafeName)
            ?.takeIf { it.isNotBlank() }
            ?.let { append(" di ").append(it) }
        append("!\n").append(room.schedule)
        room.note.takeIf { it.isNotBlank() }?.let { append("\nCatatan: ").append(it) }
        cafe?.let { append("\n\n").append(mapsUrl(it)) }
        append("\n\nDikirim lewat Carife")
    }

    private fun coordinates(cafe: Cafe): String =
        URLEncoder.encode("${cafe.lat},${cafe.lon}", "UTF-8")
}
