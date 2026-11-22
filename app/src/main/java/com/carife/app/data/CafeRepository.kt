package com.carife.app.data

import com.carife.app.core.model.Cafe
import com.carife.app.core.model.City
import com.carife.app.data.demo.DemoDataOverlay
import com.carife.app.data.local.BundledCatalog
import com.carife.app.data.local.CafeCache
import com.carife.app.data.remote.CafeSourceException
import com.carife.app.data.remote.OsmCafeMapper
import com.carife.app.data.remote.OverpassClient
import com.carife.app.data.remote.OverpassQuery
import com.carife.app.domain.DistanceUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

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
    private val inFlight = AtomicBoolean(false)

    suspend fun load(
        city: City,
        userLat: Double? = null,
        userLon: Double? = null,
        forceRefresh: Boolean = false,
    ) {
        loadMutex.withLock {
            val cached = cache.read(city.id, city.radiusMeters)
            val baseline = cached?.cafes ?: bundled.cafesFor(city)
            val origin = if (cached != null) FeedOrigin.CACHE else FeedOrigin.BUNDLED
            val now = System.currentTimeMillis()
            val cacheFresh = cached != null && now - cached.fetchedAtEpochMillis < CACHE_TTL_MS

            _state.value = CatalogState(
                city = city,
                cafes = decorate(baseline, city, userLat, userLon),
                origin = if (baseline.isEmpty()) null else origin,
                isRefreshing = networkEnabled && (!cacheFresh || forceRefresh),
                fetchedAtEpochMillis = cached?.fetchedAtEpochMillis,
                errorMessage = null,
            )

            if (!networkEnabled) return
            if (cacheFresh && !forceRefresh) return

            refresh(city, userLat, userLon)
        }
    }

    suspend fun refresh(city: City, userLat: Double? = null, userLon: Double? = null) {
        if (!networkEnabled) return
        if (!inFlight.compareAndSet(false, true)) return

        _state.value = _state.value.copy(city = city, isRefreshing = true, errorMessage = null)

        try {
            val body = client.fetchCafes(OverpassQuery.cafesAround(city))
            val cafes = OsmCafeMapper.mapResponse(body, city.id)
            val fetchedAt = System.currentTimeMillis()
            cache.write(city.id, city.radiusMeters, cafes, fetchedAt)

            _state.value = CatalogState(
                city = city,
                cafes = decorate(cafes, city, userLat, userLon),
                origin = FeedOrigin.NETWORK,
                isRefreshing = false,
                fetchedAtEpochMillis = fetchedAt,
            )
        } catch (e: CafeSourceException) {
            val current = _state.value
            val fallback = current.cafes.ifEmpty {
                decorate(cache.read(city.id, city.radiusMeters)?.cafes ?: bundled.cafesFor(city), city, userLat, userLon)
            }

            _state.value = CatalogState(
                city = city,
                cafes = fallback,
                origin = current.origin ?: FeedOrigin.BUNDLED,
                isRefreshing = false,
                fetchedAtEpochMillis = current.fetchedAtEpochMillis,
                errorMessage = if (fallback.isEmpty()) {
                    "Tidak ada kafe bawaan untuk kota ini dan koneksi gagal."
                } else {
                    "Tidak bisa memperbarui. Menampilkan data offline."
                },
            )
        } finally {
            inFlight.set(false)
        }
    }

    fun updateUserLocation(lat: Double?, lon: Double?) {
        val current = _state.value
        val city = current.city ?: return
        _state.value = current.copy(cafes = decorate(current.cafes, city, lat, lon))
    }

    private fun decorate(cafes: List<Cafe>, city: City, userLat: Double?, userLon: Double?): List<Cafe> {
        val refLat = userLat ?: city.lat
        val refLon = userLon ?: city.lon
        return DemoDataOverlay.apply(cafes, city.id).map { cafe ->
            cafe.copy(distanceMeters = DistanceUtil.haversineMeters(refLat, refLon, cafe.lat, cafe.lon))
        }
    }

    companion object {
        private const val CACHE_TTL_MS = 30 * 60 * 1000L
    }
}
