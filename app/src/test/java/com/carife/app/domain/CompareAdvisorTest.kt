package com.carife.app.domain

import com.carife.app.core.model.BudgetBucket
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.UserPreferences
import com.carife.app.core.model.WifiRequirement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompareAdvisorTest {

    private fun cafe(id: String, priceMin: Int? = null, wifiMbps: Int? = null, distance: Int? = null) = Cafe(
        id = id,
        name = id,
        cityId = "bekasi",
        lat = 0.0,
        lon = 0.0,
        priceMin = priceMin,
        priceMax = priceMin?.plus(10_000),
        wifiMbps = wifiMbps,
        distanceMeters = distance,
    )

    @Test
    fun `cheapest picks lowest price and ignores unknown`() {
        val cafes = listOf(cafe("a", priceMin = 30_000), cafe("b", priceMin = 20_000), cafe("c"))
        val advice = CompareAdvisor.advise(cafes, UserPreferences())
        assertEquals("b", advice.cheapestId)
    }

    @Test
    fun `best for study uses highest score`() {
        val prefs = UserPreferences(wifi = WifiRequirement.OVER_50)
        val cafes = listOf(cafe("slow", wifiMbps = 30), cafe("fast", wifiMbps = 90))
        val advice = CompareAdvisor.advise(cafes, prefs)
        assertEquals("fast", advice.bestForStudyId)
    }

    @Test
    fun `summary hidden when fewer than two comparable fields`() {
        val advice = CompareAdvisor.advise(listOf(cafe("a"), cafe("b")), UserPreferences())
        assertNull(advice.summary)
    }

    @Test
    fun `summary mentions differing facts when scores absent`() {
        val cafes = listOf(cafe("a", priceMin = 20_000), cafe("b", priceMin = 45_000))
        val advice = CompareAdvisor.advise(cafes, UserPreferences())
        assertEquals("a", advice.cheapestId)
        org.junit.Assert.assertTrue(advice.summary != null)
    }

    @Test
    fun `budget preference scoring is deterministic`() {
        val prefs = UserPreferences(budget = BudgetBucket.MID_25K_50K)
        val target = cafe("a", priceMin = 30_000)
        repeat(3) { assertEquals(100, MatchScorer.score(target, prefs)) }
    }
}
