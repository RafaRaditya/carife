package com.carife.app.domain

import com.carife.app.core.model.Amenity
import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.ExploreFilters
import com.carife.app.core.model.NoiseLevel
import com.carife.app.core.model.RadiusPreference
import com.carife.app.core.model.SmokingZone
import com.carife.app.core.model.SortOption
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class FilterEngineTest {

    private fun cafe(
        id: String,
        name: String = id,
        wifiMbps: Int? = null,
        priceMin: Int? = null,
        distance: Int? = null,
        zone: SmokingZone? = null,
        amenities: Set<Amenity> = emptySet(),
        hours: String? = null,
        outlets: Int? = null,
        noise: NoiseLevel? = null,
    ) = Cafe(
        id = id,
        name = name,
        cityId = "bekasi",
        lat = 0.0,
        lon = 0.0,
        distanceMeters = distance,
        wifiMbps = wifiMbps,
        priceMin = priceMin,
        smokingZone = zone,
        amenities = amenities,
        openingHoursRaw = hours,
        outletCount = outlets,
        noiseLevel = noise,
    )

    @Test
    fun `wifi above fifty excludes cafes with null speed`() {
        val cafes = listOf(
            cafe("fast", wifiMbps = 75),
            cafe("edge", wifiMbps = 50),
            cafe("unknown", wifiMbps = null, amenities = setOf(Amenity.WIFI)),
        )

        val result = FilterEngine.apply(
            cafes,
            ExploreFilters(wifi = WifiRequirement.OVER_50, sort = SortOption.NEAREST),
        )

        assertEquals(listOf("fast"), result.map { it.id })
    }

    @Test
    fun `combined filters use AND`() {
        val cafes = listOf(
            cafe("both", wifiMbps = 90, zone = SmokingZone.FULLY_NON_SMOKING, amenities = setOf(Amenity.AIR_CONDITIONED)),
            cafe("wifiOnly", wifiMbps = 90),
            cafe("zoneOnly", zone = SmokingZone.FULLY_NON_SMOKING, amenities = setOf(Amenity.AIR_CONDITIONED)),
        )

        val result = FilterEngine.apply(
            cafes,
            ExploreFilters(
                wifi = WifiRequirement.OVER_50,
                zone = ZonePreference.NON_SMOKING_AC,
            ),
        )

        assertEquals(listOf("both"), result.map { it.id })
    }

    @Test
    fun `keyword matches name address and amenity label ignoring case`() {
        val cafes = listOf(
            cafe("a", name = "Kopi Senja", amenities = setOf(Amenity.OUTDOOR_SEATING)),
            cafe("b", name = "Titik Temu"),
        )

        assertEquals(listOf("a"), FilterEngine.apply(cafes, ExploreFilters(query = "  KOPI ")).map { it.id })
        assertEquals(listOf("a"), FilterEngine.apply(cafes, ExploreFilters(query = "outdoor")).map { it.id })
        assertTrue(FilterEngine.apply(cafes, ExploreFilters(query = "tidak-ada")).isEmpty())
    }

    @Test
    fun `search finds wifi and colokan advertised by cafe cards`() {
        val cafes = listOf(
            cafe("speed", wifiMbps = 50, outlets = 2),
            cafe("tag", amenities = setOf(Amenity.WIFI), outlets = 0),
            cafe("unknown"),
        )

        for (query in listOf("wifi", "Wi-Fi", " wi  fi ")) {
            val results = FilterEngine.apply(cafes, ExploreFilters(query = query))
            assertEquals(setOf("speed", "tag"), results.map { it.id }.toSet())
        }
        assertEquals(listOf("speed"), FilterEngine.apply(cafes, ExploreFilters(query = "colokan")).map { it.id })
        val combined = ExploreFilters(query = "wifi", amenities = setOf(Amenity.WIFI), minOutlets = 1)
        assertEquals(listOf("speed"), FilterEngine.apply(cafes, combined).map { it.id })

        val standard = ExploreFilters(wifi = WifiRequirement.STANDARD)
        assertEquals(setOf("speed", "tag"), FilterEngine.apply(cafes, standard).map { it.id }.toSet())
    }

    @Test
    fun `match sort ranks by score then by distance`() {
        val cafes = listOf(
            cafe("near", distance = 50),
            cafe("highNear", distance = 200),
            cafe("highFar", distance = 800),
        )
        val result = FilterEngine.apply(
            cafes,
            ExploreFilters(sort = SortOption.MATCH),
            matchScores = mapOf("near" to 50, "highNear" to 100, "highFar" to 100),
        )

        assertEquals(listOf("highNear", "highFar", "near"), result.map { it.id })
    }

    @Test
    fun `open now filter requires parseable hours`() {
        val monday = Instant.parse("2024-01-01T03:00:00Z")
        val cafes = listOf(
            cafe("open", hours = "24/7"),
            cafe("closed", hours = "Mo-Fr 08:00-10:00"),
            cafe("unknown", hours = null),
        )

        val result = FilterEngine.apply(cafes, ExploreFilters(openNowOnly = true), now = monday)

        assertEquals(listOf("open"), result.map { it.id })
    }

    @Test
    fun `price sort puts unknown prices last`() {
        val cafes = listOf(
            cafe("unknown", priceMin = null, distance = 10),
            cafe("cheap", priceMin = 18_000, distance = 900),
            cafe("pricey", priceMin = 60_000, distance = 100),
        )

        val result = FilterEngine.apply(cafes, ExploreFilters(sort = SortOption.CHEAPEST))

        assertEquals(listOf("cheap", "pricey", "unknown"), result.map { it.id })
    }

    @Test
    fun `budget bucket excludes missing price`() {
        val cafes = listOf(cafe("priced", priceMin = 20_000), cafe("unpriced"))
        val result = FilterEngine.apply(cafes, ExploreFilters(budget = BudgetBucket.UNDER_25K))
        assertEquals(listOf("priced"), result.map { it.id })
    }

    @Test
    fun `reset without criteria keeps query`() {
        val filters = ExploreFilters(query = "kopi", wifi = WifiRequirement.OVER_50, openNowOnly = true)
        val reset = filters.withoutCriteria()
        assertEquals("kopi", reset.query)
        assertFalse(reset.hasExplicitCriteria)
        assertNull(reset.wifi)
    }

    @Test
    fun `noise filter matches exact level only`() {
        val cafes = listOf(
            cafe("hening", noise = NoiseLevel.HENING),
            cafe("normal", noise = NoiseLevel.NORMAL),
            cafe("unknown"),
        )

        val result = FilterEngine.apply(cafes, ExploreFilters(noise = NoiseLevel.HENING))

        assertEquals(listOf("hening"), result.map { it.id })
    }

    @Test
    fun `min outlets filter excludes fewer outlets and unknown counts`() {
        val cafes = listOf(
            cafe("many", outlets = 12),
            cafe("edge", outlets = 8),
            cafe("few", outlets = 3),
            cafe("unknown", outlets = null),
        )

        val result = FilterEngine.apply(cafes, ExploreFilters(minOutlets = 8))

        assertEquals(setOf("many", "edge"), result.map { it.id }.toSet())
    }

    @Test
    fun `price cap is strict and excludes missing price`() {
        val cafes = listOf(
            cafe("cheap", priceMin = 20_000),
            cafe("edge", priceMin = 30_000),
            cafe("pricey", priceMin = 45_000),
            cafe("unpriced"),
        )

        val result = FilterEngine.apply(cafes, ExploreFilters(priceCap = 30_000))

        assertEquals(listOf("cheap"), result.map { it.id })
    }

    @Test
    fun `radius bands split at one and three kilometers`() {
        val cafes = listOf(
            cafe("near", distance = 999),
            cafe("edge", distance = 1_000),
            cafe("mid", distance = 3_000),
            cafe("far", distance = 3_001),
            cafe("veryFar", distance = 9_000),
            cafe("unknown"),
        )

        fun ids(radius: RadiusPreference) = FilterEngine.apply(
            cafes,
            ExploreFilters(distance = radius, sort = SortOption.NEAREST),
        ).map { it.id }

        assertEquals(listOf("near"), ids(RadiusPreference.NEAR))
        assertEquals(listOf("edge", "mid"), ids(RadiusPreference.MID))
        assertEquals(listOf("far", "veryFar"), ids(RadiusPreference.FAR))
    }

    @Test
    fun `new criteria count toward explicit filters and get reset`() {
        val filters = ExploreFilters(
            noise = NoiseLevel.HENING,
            minOutlets = 8,
            priceCap = 30_000,
            query = "kopi",
        )

        assertEquals(3, filters.explicitCriteriaCount)

        val reset = filters.withoutCriteria()
        assertEquals("kopi", reset.query)
        assertNull(reset.noise)
        assertNull(reset.minOutlets)
        assertNull(reset.priceCap)
        assertFalse(reset.hasExplicitCriteria)
    }
}
