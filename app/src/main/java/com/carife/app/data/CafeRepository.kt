package com.carife.app.data

import com.carife.app.core.model.Cafe
import com.carife.app.core.model.City
import com.carife.app.data.demo.DemoDataOverlay
import com.carife.app.data.local.BundledCatalog
import com.carife.app.data.local.CafeCache
import com.carife.app.data.remote.CafeSourceException
import com.carife.app.data.remote.OverpassClient
import com.carife.app.data.remote.OverpassQuery
import com.carife.app.domain.DistanceUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class FeedOrigin {
    /** Snapshot OpenStreetMap yang dibundel di APK. Butuh nol koneksi. */
    BUNDLED,

    /** Hasil unduhan terakhir yang disimpan di perangkat. */
    CACHE,

    /** Hasil permintaan langsung ke Overpass. */
    NETWORK,
}

class CafeRepository(
    private val client: OverpassClient,
    private val cache: CafeCache,
    private val bundled: BundledCatalog,
    private val networkEnabled: Boolean,
) {

    data class CatalogState(
        val city: City? = null,
        val cafes: List<Cafe> = emptyList(),
        val origin: FeedOrigin? = null,
        val isRefreshing: Boolean = false,
        val fetchedAtEpochMillis: Long? = null,
        val errorMessage: String? = null,
    ) {
        val hasData: Boolean get() = cafes.isNotEmpty()
        val isOfflineData: Boolean get() = origin == FeedOrigin.BUNDLED
        val isEmptyAfterLoad: Boolean
            get() = !isRefreshing && errorMessage == null && origin != null && cafes.isEmpty()
    }

    private val _state = MutableStateFlow(CatalogState())
    val state: StateFlow<CatalogState> = _state.asStateFlow()

    private val loadMutex = Mutex()
    private var userCoordinates: Pair<Double, Double>? = null

    suspend fun load(city: City, forceRefresh: Boolean = false) {
        loadMutex.withLock {
            val cached = if (networkEnabled) cache.read(city.id, city.radiusMeters) else null
            val baseline = cached?.cafes ?: bundled.cafesFor(city)
            val origin = if (cached != null) FeedOrigin.CACHE else FeedOrigin.BUNDLED
            val now = System.currentTimeMillis()
            val cacheFresh = cached != null && now - cached.fetchedAtEpochMillis < CACHE_TTL_MS
            val shouldRefresh = networkEnabled && (!cacheFresh || forceRefresh)

            _state.value = CatalogState(
                city = city,
                cafes = withDistances(DemoDataOverlay.apply(baseline, city.id), city),
                origin = origin,
                isRefreshing = shouldRefresh,
                fetchedAtEpochMillis = cached?.fetchedAtEpochMillis,
            )

            if (shouldRefresh) refresh(city)
        }
    }

    private suspend fun refresh(city: City) {
        try {
            val cafes = client.fetchCafes(OverpassQuery.cafesAround(city), city.id)
            val fetchedAt = System.currentTimeMillis()
            cache.write(city.id, city.radiusMeters, cafes, fetchedAt)

            _state.value = CatalogState(
                city = city,
                cafes = withDistances(DemoDataOverlay.apply(cafes, city.id), city),
                origin = FeedOrigin.NETWORK,
                fetchedAtEpochMillis = fetchedAt,
            )
        } catch (_: CafeSourceException) {
            val current = _state.value
            _state.value = current.copy(
                isRefreshing = false,
                errorMessage = if (current.cafes.isEmpty()) {
                    "Belum ada katalog tersimpan dan pembaruan gagal."
                } else {
                    "Pembaruan gagal. Katalog tersimpan masih bisa digunakan."
                },
            )
        }
    }

    fun updateUserLocation(lat: Double?, lon: Double?) {
        userCoordinates = if (lat != null && lon != null) lat to lon else null
        val current = _state.value
        val city = current.city ?: return
        _state.value = current.copy(cafes = withDistances(current.cafes, city))
    }

    private fun withDistances(cafes: List<Cafe>, city: City): List<Cafe> {
        val (refLat, refLon) = userCoordinates ?: (city.lat to city.lon)
        return cafes.map { cafe ->
            cafe.copy(distanceMeters = DistanceUtil.haversineMeters(refLat, refLon, cafe.lat, cafe.lon))
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 30 * 60 * 1000L
    }
}
