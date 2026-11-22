package com.carife.app.data.demo

import com.carife.app.core.model.Amenity
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.DensityLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataGeneratorTest {

    private fun cafe(id: String) = Cafe(
        id = id,
        name = "Kafe $id",
        cityId = "bekasi",
        lat = -6.2383,
        lon = 106.9756,
    )

    @Test
    fun `same id produces identical values`() {
        val first = DemoDataGenerator.fill(cafe("osm:node/1"))
        val second = DemoDataGenerator.fill(cafe("osm:node/1"))

        assertEquals(first, second)
    }

    @Test
    fun `different ids produce different values`() {
        val first = DemoDataGenerator.fill(cafe("osm:node/1"))
        val second = DemoDataGenerator.fill(cafe("osm:node/2"))

        assertTrue(first.wifiMbps != second.wifiMbps || first.priceMin != second.priceMin)
    }

    @Test
    fun `fill leaves existing osm values untouched`() {
        val original = cafe("osm:node/3").copy(
            priceMin = 30_000,
            priceMax = 60_000,
            wifiMbps = 100,
            rating = 4.9,
            outletCount = 20,
            amenities = setOf(Amenity.WIFI),
        )

        val filled = DemoDataGenerator.fill(original)

        assertEquals(30_000, filled.priceMin)
        assertEquals(60_000, filled.priceMax)
        assertEquals(100, filled.wifiMbps)
        assertEquals(4.9, filled.rating!!, 0.001)
        assertEquals(20, filled.outletCount)
        assertEquals(setOf(Amenity.WIFI), filled.amenities)
    }

    @Test
    fun `filled cafes always get complete demo fields`() {
        val filled = DemoDataGenerator.fill(cafe("osm:way/9"))

        assertNotNull(filled.priceMin)
        assertNotNull(filled.priceMax)
        assertNotNull(filled.wifiMbps)
        assertNotNull(filled.outletCount)
        assertNotNull(filled.rating)
        assertNotNull(filled.reviewCount)
        assertNotNull(filled.densityPercent)
        assertNotNull(filled.densityLevel)
        assertNotNull(filled.noiseLevel)
        assertNotNull(filled.photoCount)
        assertTrue(filled.isDemoData)
        assertTrue(filled.menu.isNotEmpty())
        assertTrue(filled.reviews.isNotEmpty())
    }

    @Test
    fun `price range is coherent`() {
        (1..200).forEach { index ->
            val filled = DemoDataGenerator.fill(cafe("osm:node/$index"))
            assertTrue(
                "harga minimum harus <= maksimum untuk ${filled.id}",
                filled.priceMin!! <= filled.priceMax!!,
            )
        }
    }

    @Test
    fun `density level matches percent buckets`() {
        (1..200).forEach { index ->
            val filled = DemoDataGenerator.fill(cafe("osm:node/$index"))
            val expected = when {
                filled.densityPercent!! < 33 -> DensityLevel.SEPI
                filled.densityPercent!! < 66 -> DensityLevel.SEDANG
                else -> DensityLevel.PENUH
            }
            assertEquals(expected, filled.densityLevel)
        }
    }

    @Test
    fun `wifi values come from the documented ladder`() {
        val ladder = setOf(10, 20, 30, 45, 50, 75, 90, 100)
        (1..200).forEach { index ->
            val filled = DemoDataGenerator.fill(cafe("osm:node/$index"))
            assertTrue("${filled.wifiMbps} tidak ada di tangga Wi-Fi", filled.wifiMbps in ladder)
        }
    }
}
