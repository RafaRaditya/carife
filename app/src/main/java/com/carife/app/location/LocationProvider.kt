package com.carife.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocationProvider(private val context: Context) {

    data class UserLocation(val lat: Double, val lon: Double)

    private val _location = MutableStateFlow<UserLocation?>(null)
    val location: StateFlow<UserLocation?> = _location.asStateFlow()

    private var listener: LocationListener? = null

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    fun start() {
        if (!hasPermission() || listener != null) return
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

        val lastKnown = lastKnownLocation(manager)
        if (lastKnown != null) {
            _location.value = UserLocation(lastKnown.latitude, lastKnown.longitude)
        }

        val newListener = LocationListener { loc ->
            _location.value = UserLocation(loc.latitude, loc.longitude)
        }
        listener = newListener
        runCatching {
            manager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                60_000L,
                250f,
                newListener,
                Looper.getMainLooper(),
            )
        }.onFailure {
            runCatching {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    60_000L,
                    250f,
                    newListener,
                    Looper.getMainLooper(),
                )
            }
        }
    }

    fun stop() {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        listener?.let { runCatching { manager?.removeUpdates(it) } }
        listener = null
    }

    @Suppress("MissingPermission")
    private fun lastKnownLocation(manager: LocationManager): Location? {
        if (!hasPermission()) return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        return providers
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }
}
