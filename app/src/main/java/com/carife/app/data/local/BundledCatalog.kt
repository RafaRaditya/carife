package com.carife.app.data.local

import android.content.Context
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.City
import com.carife.app.data.remote.OsmCafeMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Membaca katalog kafe yang dibundel di dalam APK (assets/offline/<cityId>.json).
 *
 * Data ini adalah snapshot OpenStreetMap yang diambil saat build, sehingga aplikasi
 * tetap menampilkan kafe nyata tanpa koneksi internet sama sekali.
 */
class BundledCatalog(private val context: Context) {

    private val memo = mutableMapOf<String, List<Cafe>>()

    suspend fun cafesFor(city: City): List<Cafe> = withContext(Dispatchers.IO) {
        memo[city.id]?.let { return@withContext it }

        val loaded = runCatching {
            context.assets.open(ASSET_DIR + city.id + ".json")
                .bufferedReader()
                .use { it.readText() }
                .let { OsmCafeMapper.mapResponse(it, city.id) }
        }.getOrDefault(emptyList())

        memo[city.id] = loaded
        loaded
    }

    fun hasBundledData(city: City): Boolean = runCatching {
        context.assets.open(ASSET_DIR + city.id + ".json").use { it.read() }
        true
    }.getOrDefault(false)

    private companion object {
        const val ASSET_DIR = "offline/"
    }
}
