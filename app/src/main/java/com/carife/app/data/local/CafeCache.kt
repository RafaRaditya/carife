package com.carife.app.data.local

import com.carife.app.core.model.Amenity
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.SmokingZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class CafeCache(private val cacheDir: File) {

    // Cache hanya menyimpan field hasil OpenStreetMap. Field contoh (harga, Wi-Fi,
    // colokan, kepadatan, menu, ulasan) diisi ulang secara deterministik oleh
    // DemoDataOverlay setiap kali katalog dimuat, jadi tidak perlu ikut disimpan.
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    @Serializable
    private data class CachedCafe(
        val id: String,
        val name: String,
        val cityId: String,
        val area: String? = null,
        val address: String? = null,
        val lat: Double,
        val lon: Double,
        val amenities: List<String> = emptyList(),
        val smokingZone: String? = null,
        val openingHoursRaw: String? = null,
        val website: String? = null,
    ) {
        fun toCafe() = Cafe(
            id = id,
            name = name,
            cityId = cityId,
            area = area,
            address = address,
            lat = lat,
            lon = lon,
            amenities = amenities.mapNotNull { name -> Amenity.entries.firstOrNull { it.name == name } }.toSet(),
            smokingZone = smokingZone?.let { name -> SmokingZone.entries.firstOrNull { it.name == name } },
            openingHoursRaw = openingHoursRaw,
            website = website,
        )

        companion object {
            fun from(cafe: Cafe) = CachedCafe(
                id = cafe.id,
                name = cafe.name,
                cityId = cafe.cityId,
                area = cafe.area,
                address = cafe.address,
                lat = cafe.lat,
                lon = cafe.lon,
                amenities = cafe.amenities.map { it.name },
                smokingZone = cafe.smokingZone?.name,
                openingHoursRaw = cafe.openingHoursRaw,
                website = cafe.website,
            )
        }
    }

    @Serializable
    private data class CachedPayload(
        val fetchedAtEpochMillis: Long,
        val cafes: List<CachedCafe>,
        val responseChecked: Boolean = false,
    )

    data class Snapshot(val cafes: List<Cafe>, val fetchedAtEpochMillis: Long)

    suspend fun read(cityId: String, radiusMeters: Int): Snapshot? = withContext(Dispatchers.IO) {
        val file = fileFor(cityId, radiusMeters)
        if (!file.exists()) return@withContext null
        val payload = runCatching { json.decodeFromString<CachedPayload>(file.readText()) }.getOrNull()
            ?: return@withContext null
        // Earlier versions could cache an empty list after decoding failed.
        if (payload.cafes.isEmpty() && !payload.responseChecked) return@withContext null
        Snapshot(payload.cafes.map { it.toCafe() }, payload.fetchedAtEpochMillis)
    }

    suspend fun write(cityId: String, radiusMeters: Int, cafes: List<Cafe>, fetchedAt: Long) =
        withContext(Dispatchers.IO) {
            runCatching {
                cacheDir.mkdirs()
                val payload = CachedPayload(fetchedAt, cafes.map { CachedCafe.from(it) }, responseChecked = true)
                fileFor(cityId, radiusMeters).writeText(json.encodeToString(payload))
            }
        }

    private fun fileFor(cityId: String, radiusMeters: Int) =
        File(cacheDir, "${cityId}_$radiusMeters.json")
}
