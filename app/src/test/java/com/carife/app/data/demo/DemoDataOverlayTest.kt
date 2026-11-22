package com.carife.app.data.demo

import com.carife.app.core.model.Cafe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataOverlayTest {

    private fun osmCafe(id: String, name: String) = Cafe(
        id = id,
        name = name,
        cityId = "bekasi",
        lat = -6.2235,
        lon = 106.9968,
    )

    @Test
    fun `hero cafes only appear in bekasi`() {
        val bekasi = DemoDataOverlay.apply(emptyList(), "bekasi")
        val jakarta = DemoDataOverlay.apply(emptyList(), "jakarta")

        assertEquals(3, bekasi.size)
        assertTrue(jakarta.isEmpty())
        assertTrue(bekasi.all { it.isDemoData })
    }

    @Test
    fun `hero cafes carry the curated video values`() {
        val heroes = DemoDataOverlay.apply(emptyList(), "bekasi")
        val kopitagram = heroes.first { it.name.contains("Kopitagram") }

        assertEquals(50, kopitagram.wifiMbps)
        assertEquals(22_000, kopitagram.priceMin)
        assertEquals(45_000, kopitagram.priceMax)
        assertEquals(4.8, kopitagram.rating!!, 0.001)
        assertEquals(330, kopitagram.reviewCount)
        assertEquals(45, kopitagram.densityPercent)
        assertEquals(14, kopitagram.photoCount)
    }

    @Test
    fun `simetri gets the documented seventy five mbps`() {
        val cafes = listOf(osmCafe("osm:way/1474301168", "Simetri Coffee Roasters"))

        val result = DemoDataOverlay.apply(cafes, "bekasi")
        val simetri = result.first { it.name == "Simetri Coffee Roasters" }

        assertEquals(75, simetri.wifiMbps)
    }

    @Test
    fun `osm identity fields are preserved`() {
        val original = osmCafe("osm:node/42", "Kopi Nako")

        val result = DemoDataOverlay.apply(listOf(original), "bekasi")
        val merged = result.first { it.id == "osm:node/42" }

        assertEquals("Kopi Nako", merged.name)
        assertEquals(original.lat, merged.lat, 0.0)
        assertEquals(original.lon, merged.lon, 0.0)
    }

    @Test
    fun `no duplicate ids after merging heroes`() {
        val heroes = DemoDataOverlay.apply(emptyList(), "bekasi")
        val duplicated = heroes + heroes

        val result = DemoDataOverlay.apply(duplicated, "bekasi")

        assertEquals(heroes.size, result.size)
        assertEquals(result.size, result.map { it.id }.distinct().size)
    }

    @Test
    fun `every cafe ends up with complete demo data`() {
        val cafes = (1..20).map { osmCafe("osm:node/$it", "Kafe $it") }

        val result = DemoDataOverlay.apply(cafes, "bekasi")

        result.forEach { cafe ->
            assertNotNull("${cafe.name} tidak punya harga", cafe.priceMin)
            assertNotNull("${cafe.name} tidak punya Wi-Fi", cafe.wifiMbps)
            assertNotNull("${cafe.name} tidak punya rating", cafe.rating)
            assertNotNull("${cafe.name} tidak punya kepadatan", cafe.densityPercent)
            assertTrue("${cafe.name} tidak ditandai demo", cafe.isDemoData)
        }
    }

    @Test
    fun `overlay is deterministic across calls`() {
        val cafes = listOf(osmCafe("osm:node/7", "Kafe Tujuh"))

        val first = DemoDataOverlay.apply(cafes, "bekasi")
        val second = DemoDataOverlay.apply(cafes, "bekasi")

        assertEquals(first, second)
    }
}
