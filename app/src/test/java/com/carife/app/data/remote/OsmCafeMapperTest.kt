package com.carife.app.data.remote

import com.carife.app.core.model.Cafe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OsmCafeMapperTest {

    private val fixture = """
    {
      "elements": [
        {
          "type": "node",
          "id": 111,
          "lat": -6.2383,
          "lon": 106.9756,
          "tags": {
            "amenity": "cafe",
            "name": "Kopi Senja",
            "addr:street": "Jl. Ahmad Yani",
            "addr:housenumber": "12",
            "addr:suburb": "Bekasi Selatan",
            "opening_hours": "Mo-Su 08:00-22:00",
            "internet_access": "wlan",
            "outdoor_seating": "yes",
            "smoking": "no",
            "website": "https://example.com"
          }
        },
        {
          "type": "node",
          "id": 112,
          "lat": -6.2390,
          "lon": 106.9760,
          "tags": { "amenity": "cafe" }
        },
        {
          "type": "way",
          "id": 222,
          "center": { "lat": -6.23840, "lon": 106.97570 },
          "tags": {
            "amenity": "cafe",
            "name": "Kopi Senja",
            "addr:street": "Jl. Ahmad Yani",
            "wheelchair": "yes"
          }
        },
        {
          "type": "node",
          "id": 333,
          "lat": -6.2500,
          "lon": 106.9900,
          "tags": {
            "amenity": "cafe",
            "name": "Titik Temu",
            "smoking": "isolated",
            "internet_access": "no"
          }
        }
      ]
    }
    """.trimIndent()

    @Test
    fun `maps tags to cafe fields`() {
        val cafes = OsmCafeMapper.mapResponse(fixture, "bekasi")
        val kopi = cafes.first { it.id == "osm:node/111" }

        assertEquals("Kopi Senja", kopi.name)
        assertEquals("Jl. Ahmad Yani No. 12", kopi.address)
        assertEquals("Bekasi Selatan", kopi.area)
        assertEquals("Mo-Su 08:00-22:00", kopi.openingHoursRaw)
        assertEquals("https://example.com", kopi.website)
        assertTrue(kopi.amenities.contains(com.carife.app.core.model.Amenity.WIFI))
        assertTrue(kopi.amenities.contains(com.carife.app.core.model.Amenity.OUTDOOR_SEATING))
        assertEquals(com.carife.app.core.model.SmokingZone.FULLY_NON_SMOKING, kopi.smokingZone)
        assertNull(kopi.priceMin)
        assertNull(kopi.wifiMbps)
        assertNull(kopi.rating)
    }

    @Test
    fun `skips elements without name`() {
        val cafes = OsmCafeMapper.mapResponse(fixture, "bekasi")
        assertTrue(cafes.none { it.id == "osm:node/112" })
    }

    @Test
    fun `way uses center coordinates`() {
        val cafes = OsmCafeMapper.mapResponse(fixture, "bekasi")
        // "Kopi Senja" dari way berada <50 m dari node bernama sama, jadi ter-dedupe.
        assertEquals(1, cafes.count { it.name == "Kopi Senja" })
    }

    @Test
    fun `internet access no does not add wifi amenity`() {
        val cafes = OsmCafeMapper.mapResponse(fixture, "bekasi")
        val titikTemu = cafes.first { it.name == "Titik Temu" }
        assertTrue(!titikTemu.amenities.contains(com.carife.app.core.model.Amenity.WIFI))
        assertEquals(com.carife.app.core.model.SmokingZone.SEPARATE_AREA, titikTemu.smokingZone)
    }

    @Test
    fun `malformed body returns empty list`() {
        assertTrue(OsmCafeMapper.mapResponse("bukan json", "bekasi").isEmpty())
        assertTrue(OsmCafeMapper.mapResponse("{}", "bekasi").isEmpty())
    }

    @Test
    fun `incomplete refresh is distinct from a valid empty catalog`() {
        assertNull(OsmCafeMapper.mapCompleteResponse("""{"elements":{}}""", "bekasi"))
        assertNull(OsmCafeMapper.mapCompleteResponse("""{"remark":"timeout","elements":[]}""", "bekasi"))
        assertEquals(
            emptyList<Cafe>(),
            OsmCafeMapper.mapCompleteResponse("""{"elements":[]}""", "bekasi"),
        )
    }

    @Test
    fun `query builder escapes properly`() {
        val query = OverpassQuery.cafesAround(-6.2383, 106.9756, 8000)
        assertTrue(query.contains("around:8000,-6.238300,106.975600"))
        assertTrue(query.contains("amenity"))
        assertTrue(query.startsWith("[out:json]"))
    }
}
