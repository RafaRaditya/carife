package com.carife.app.data.local

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CafeCacheTest {

    @get:Rule
    val cacheFolder = TemporaryFolder()

    @Test
    fun `empty cache is reused only when the response was validated`() = runBlocking {
        val cityId = "bekasi"
        val radiusMeters = 8000
        val cache = CafeCache(cacheFolder.root)
        cacheFolder.newFile("${cityId}_$radiusMeters.json")
            .writeText("""{"fetchedAtEpochMillis":100,"cafes":[]}""")

        assertNull(cache.read(cityId, radiusMeters))

        cache.write(cityId, radiusMeters, emptyList(), fetchedAt = 200)

        assertEquals(CafeCache.Snapshot(emptyList(), 200), cache.read(cityId, radiusMeters))
    }
}
