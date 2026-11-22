package com.carife.app.data.demo

import com.carife.app.core.model.ExploreFilters
import com.carife.app.core.model.NoiseLevel
import com.carife.app.core.model.SortOption
import com.carife.app.core.model.WifiRequirement
import com.carife.app.data.remote.OsmCafeMapper
import com.carife.app.domain.FilterEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checklist penerimaan dari CARIFE_ANDROID_MVP.md §7, dijalankan pada katalog
 * Bekasi sungguhan (snapshot OSM di assets) yang sudah dilapisi data demo.
 */
class DemoCatalogAcceptanceTest {

    private fun bekasiCatalog() = DemoDataOverlay.apply(
        OsmCafeMapper.mapResponse(
            java.io.File("src/main/assets/offline/bekasi.json").readText(),
            "bekasi",
        ),
        "bekasi",
    )

    @Test
    fun `wifi above fifty keeps simetri and titik temu but rejects kopitagram`() {
        val result = FilterEngine.apply(
            bekasiCatalog(),
            ExploreFilters(wifi = WifiRequirement.OVER_50, sort = SortOption.NEAREST),
        )
        val names = result.map { it.name }

        assertTrue("Simetri harus lolos filter >50 Mbps", names.any { it.contains("Simetri") })
        assertTrue("Titik Temu harus lolos filter >50 Mbps", names.any { it.contains("Titik Temu") })
        assertFalse("Kopitagram 50 Mbps tidak boleh lolos ambang ketat", names.any { it.contains("Kopitagram") })
    }

    @Test
    fun `searching kopitagram returns the hero cafe`() {
        val result = FilterEngine.apply(bekasiCatalog(), ExploreFilters(query = "KOPITAGRAM"))

        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.name.contains("Kopitagram", ignoreCase = true) })
    }

    @Test
    fun `cheap budget filter finds hero and generated cafes`() {
        val result = FilterEngine.apply(bekasiCatalog(), ExploreFilters(priceCap = 30_000))

        assertTrue("harus ada kafe di bawah Rp30rb", result.isNotEmpty())
        assertTrue(result.all { (it.priceMin ?: Int.MAX_VALUE) < 30_000 })
    }

    @Test
    fun `only hening cafes survive the hening filter`() {
        val catalog = bekasiCatalog()
        val result = FilterEngine.apply(catalog, ExploreFilters(noise = NoiseLevel.HENING))

        assertTrue(result.isNotEmpty())
        assertTrue(result.size < catalog.size)
        assertTrue(result.all { it.noiseLevel == NoiseLevel.HENING })
    }

    @Test
    fun `outlet filter only keeps cafes with enough sockets`() {
        val result = FilterEngine.apply(bekasiCatalog(), ExploreFilters(minOutlets = 8))

        assertTrue(result.isNotEmpty())
        assertTrue(result.all { (it.outletCount ?: 0) >= 8 })
    }

    @Test
    fun `every catalog entry is labeled as demo data`() {
        assertTrue(bekasiCatalog().all { it.isDemoData })
    }

    @Test
    fun `hero cafes are merged into the real osm catalog`() {
        val names = bekasiCatalog().map { it.name }

        assertTrue(names.any { it.contains("Kopitagram") })
        assertTrue(names.any { it.contains("Simetri") })
        assertTrue(names.any { it.contains("Starbucks") })
    }
}
