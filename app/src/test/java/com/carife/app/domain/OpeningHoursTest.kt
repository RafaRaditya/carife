package com.carife.app.domain

import com.carife.app.core.model.Cafe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class OpeningHoursTest {

    @Test
    fun `always open is recognized`() {
        val parsed = OpeningHours.parse("24/7")
        assertNotNull(parsed)
        assertTrue(OpeningHours.isAlwaysOpen("24/7"))
        assertEquals(true, OpeningHours.isOpenNow("24/7", Instant.parse("2024-03-03T19:00:00Z")))
    }

    @Test
    fun `range crossing midnight works on both sides`() {
        // Senin 20:00 WIB = Senin 13:00 UTC
        val mondayEvening = Instant.parse("2024-01-01T13:00:00Z")
        // Selasa 01:00 WIB = Senin 18:00 UTC
        val afterMidnight = Instant.parse("2024-01-01T18:00:00Z")
        // Selasa 10:00 WIB = Selasa 03:00 UTC (harus tutup)
        val closed = Instant.parse("2024-01-02T03:00:00Z")

        assertEquals(true, OpeningHours.isOpenNow("Mo 18:00-02:00", mondayEvening))
        assertEquals(true, OpeningHours.isOpenNow("Mo 18:00-02:00", afterMidnight))
        assertEquals(false, OpeningHours.isOpenNow("Mo 18:00-02:00", closed))
    }

    @Test
    fun `weekday range covers listed days only`() {
        // Rabu 10:00 WIB = Rabu 03:00 UTC
        val wednesday = Instant.parse("2024-01-03T03:00:00Z")
        // Sabtu 10:00 WIB = Sabtu 03:00 UTC
        val saturday = Instant.parse("2024-01-06T03:00:00Z")

        assertEquals(true, OpeningHours.isOpenNow("Mo-Fr 08:00-22:00", wednesday))
        assertEquals(false, OpeningHours.isOpenNow("Mo-Fr 08:00-22:00", saturday))
    }

    @Test
    fun `multiple rules combined with semicolon`() {
        val saturdayNoon = Instant.parse("2024-01-06T05:00:00Z") // Sabtu 12:00 WIB
        assertEquals(true, OpeningHours.isOpenNow("Mo-Fr 08:00-22:00; Sa-Su 10:00-15:00", saturdayNoon))
    }

    @Test
    fun `later weekly rules override earlier hours`() {
        val saturdayAfternoon = Instant.parse("2024-01-06T07:00:00Z")
        val sundayMorning = Instant.parse("2024-01-07T03:00:00Z")

        assertEquals(false, OpeningHours.isOpenNow("Tu-Su 08:00-15:00; Sa 08:00-12:00", saturdayAfternoon))
        assertEquals(false, OpeningHours.isOpenNow("Mo-Su 08:00-22:00; Su off", sundayMorning))
        assertEquals(true, OpeningHours.isOpenNow("Mo-Su,PH 07:00-22:00", sundayMorning))
        assertNull(OpeningHours.isOpenNow("Mo-Su 08:00-22:00; PH unknown", sundayMorning))
    }

    @Test
    fun `cafe in Makassar uses local time for open now`() {
        val cafe = Cafe(
            id = "test",
            name = "Kafe",
            cityId = "makassar",
            lat = -5.1,
            lon = 119.4,
            openingHoursRaw = "08:00-22:00",
        )
        val halfPastSevenWib = Instant.parse("2024-01-01T00:30:00Z")

        assertEquals(true, OpeningHours.isOpenNow(cafe, halfPastSevenWib))
        assertEquals(false, OpeningHours.isOpenNow(cafe.copy(cityId = "bekasi"), halfPastSevenWib))
    }

    @Test
    fun `unknown format yields null instead of guessing`() {
        assertNull(OpeningHours.parse("kapan saja"))
        assertNull(OpeningHours.parse(""))
        assertNull(OpeningHours.parse(null))
        assertNull(OpeningHours.isOpenNow("sunrise to sunset"))
        assertFalse(OpeningHours.isAlwaysOpen("24/7 elu"))
    }
}
