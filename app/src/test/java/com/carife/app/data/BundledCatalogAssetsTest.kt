package com.carife.app.data

import com.carife.app.data.remote.OsmCafeMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memastikan berkas snapshot offline yang dibundel bisa diparse menjadi kafe nyata.
 * Test ini membaca assets langsung dari disk supaya data yang dikirim ke APK ikut divalidasi.
 */
class BundledCatalogAssetsTest {

    private fun assetFile(cityId: String) =
        java.io.File("src/main/assets/offline/$cityId.json")

    @Test
    fun `all declared cities have bundled data`() {
        val cities = listOf(
            "bekasi", "jakarta", "depok", "tangerang", "tangerang-selatan", "bogor",
            "bandung", "semarang", "yogyakarta", "surabaya", "malang", "medan", "makassar",
        )

        cities.forEach { cityId ->
            val file = assetFile(cityId)
            assertTrue("Berkas offline untuk $cityId tidak ada", file.exists())
        }
    }

    @Test
    fun `bundled bekasi data parses into named cafes`() {
        val raw = assetFile("bekasi").readText()
        val cafes = OsmCafeMapper.mapResponse(raw, "bekasi")

        assertTrue("Snapshot Bekasi harus berisi kafe", cafes.size > 20)
        assertTrue(cafes.all { it.name.isNotBlank() })
        assertTrue(cafes.all { it.cityId == "bekasi" })
    }

    @Test
    fun `bundled jakarta data is the largest snapshot`() {
        val jakarta = OsmCafeMapper.mapResponse(assetFile("jakarta").readText(), "jakarta")
        val medan = OsmCafeMapper.mapResponse(assetFile("medan").readText(), "medan")

        assertTrue(jakarta.size > medan.size)
        assertTrue(jakarta.size > 300)
    }

    @Test
    fun `bundled data carries opening hours and amenities when present in osm`() {
        val cafes = OsmCafeMapper.mapResponse(assetFile("jakarta").readText(), "jakarta")

        assertTrue(cafes.any { it.openingHoursRaw != null })
        assertTrue(cafes.any { it.amenities.isNotEmpty() })
    }

    @Test
    fun `total bundled cafes across cities is substantial`() {
        val cities = listOf("bekasi", "jakarta", "bandung", "surabaya", "makassar")
        val total = cities.sumOf { cityId ->
            OsmCafeMapper.mapResponse(assetFile(cityId).readText(), cityId).size
        }

        assertEquals(true, total > 800)
    }
}
