package com.carife.app.domain

import com.carife.app.core.model.Amenity
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.UserPreferences
import com.carife.app.core.model.WifiRequirement
import com.carife.app.core.model.ZonePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchScorerTest {

    private fun cafe(
        id: String,
        wifiMbps: Int? = null,
        distance: Int? = null,
        amenities: Set<Amenity> = emptySet(),
        zone: com.carife.app.core.model.SmokingZone? = null,
    ) = Cafe(
        id = id,
        name = id,
        cityId = "bekasi",
        lat = 0.0,
        lon = 0.0,
        distanceMeters = distance,
        wifiMbps = wifiMbps,
        smokingZone = zone,
        amenities = amenities,
    )

    @Test
    fun `no criteria means no score`() {
        assertNull(MatchScorer.score(cafe("a"), UserPreferences()))
        assertNull(MatchScorer.score(cafe("a"), UserPreferences(wifi = WifiRequirement.ANY)))
    }

    @Test
    fun `score is fulfilled over total rounded`() {
        val prefs = UserPreferences(
            wifi = WifiRequirement.OVER_50,
            zone = ZonePreference.NON_SMOKING_AC,
            amenities = setOf(Amenity.OUTDOOR_SEATING),
        )

        val cafe = cafe(
            id = "a",
            wifiMbps = 90,
            amenities = setOf(Amenity.AIR_CONDITIONED, Amenity.OUTDOOR_SEATING),
            zone = com.carife.app.core.model.SmokingZone.FULLY_NON_SMOKING,
        )

        // 3 syarat: wifi (ya), zonasi (ya), outdoor (ya)
        assertEquals(100, MatchScorer.score(cafe, prefs))
    }

    @Test
    fun `null fields count as unmet not as skipped`() {
        val prefs = UserPreferences(wifi = WifiRequirement.OVER_50)
        assertEquals(0, MatchScorer.score(cafe("a", wifiMbps = null), prefs))
    }

    @Test
    fun `ranking breaks ties by distance then name`() {
        val prefs = UserPreferences(wifi = WifiRequirement.MIN_30)
        val cafes = listOf(
            cafe("z", wifiMbps = 50, distance = 500),
            cafe("a", wifiMbps = 50, distance = 500),
            cafe("near", wifiMbps = 50, distance = 100),
        )

        val ranked = MatchScorer.rank(cafes, prefs)

        assertEquals(listOf("near", "a", "z"), ranked.map { it.first.id })
    }

    @Test
    fun `deterministic across repeated calls`() {
        val prefs = UserPreferences(wifi = WifiRequirement.OVER_30)
        val target = cafe("a", wifiMbps = 40)
        repeat(5) { assertEquals(100, MatchScorer.score(target, prefs)) }
    }
}
