package com.carife.app.domain

import com.carife.app.core.model.Cafe
import com.carife.app.core.model.StudyRoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareTextBuilderTest {
    private val cafe = Cafe(
        id = "osm:node/1",
        name = "Kafe & Kopi",
        cityId = "bekasi",
        lat = -6.2383,
        lon = 106.9756,
        distanceMeters = 400,
    )

    @Test
    fun `directions and share link use the selected branch coordinates`() {
        val point = "-6.2383%2C106.9756"

        assertEquals("https://www.google.com/maps/dir/?api=1&destination=$point", ShareTextBuilder.directionsUrl(cafe))
        assertEquals("https://www.google.com/maps/search/?api=1&query=$point", ShareTextBuilder.mapsUrl(cafe))
        assertTrue(ShareTextBuilder.shareMessage(cafe).contains(cafe.name))
        assertFalse(ShareTextBuilder.shareMessage(cafe).contains("400 m"))
    }

    @Test
    fun `room invite includes title schedule note and map link`() {
        val room = StudyRoom(
            id = "room-1",
            cafeName = "Kafe Lain",
            title = "Belajar Bareng",
            schedule = "Hari ini · 16.00",
            note = "Bawa laptop",
        )

        val message = ShareTextBuilder.roomInviteMessage(room, cafe)

        assertTrue(message.contains("Belajar Bareng"))
        assertTrue(message.contains(cafe.name))
        assertTrue(message.contains("Hari ini · 16.00"))
        assertTrue(message.contains("Bawa laptop"))
        assertTrue(message.contains(ShareTextBuilder.mapsUrl(cafe)))
    }

    @Test
    fun `room invite falls back to stored cafe name without a resolved cafe`() {
        val room = StudyRoom(
            id = "room-1",
            cafeName = "Kafe Lain",
            title = "Belajar Bareng",
            schedule = "Besok · 10.00",
        )

        val message = ShareTextBuilder.roomInviteMessage(room, null)

        assertTrue(message.contains("Kafe Lain"))
        assertFalse(message.contains("https://"))
    }
}
