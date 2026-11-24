package com.carife.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.carife.app.R
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.City
import com.carife.app.location.LocationProvider
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import kotlin.math.ln
import kotlin.math.max

@Composable
fun OsmMapView(
    city: City,
    cafes: List<Cafe>,
    onOpenCafe: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxMarkers: Int = 120,
    userLocation: LocationProvider.UserLocation? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnOpenCafe by rememberUpdatedState(onOpenCafe)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            setTilesScaledToDpi(true)
            minZoomLevel = 3.0
            maxZoomLevel = 19.0
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(city.lat, city.lon))
            overlays.add(CopyrightOverlay(context))
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(city, cafes, maxMarkers, userLocation) {
        mapView.overlays.removeAll { it is Marker }

        val visible = cafes.take(maxMarkers)
        visible.forEach { cafe ->
            val marker = Marker(mapView).apply {
                position = GeoPoint(cafe.lat, cafe.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = cafe.name
                icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
                setOnMarkerClickListener { _, _ ->
                    currentOnOpenCafe(cafe.id)
                    true
                }
            }
            mapView.overlays.add(marker)
        }

        if (userLocation != null) {
            val userMarker = Marker(mapView).apply {
                position = GeoPoint(userLocation.lat, userLocation.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                title = "Lokasi saya"
                icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
            }
            mapView.overlays.add(userMarker)
        }

        val (center, zoom) = cameraFor(city, visible, userLocation)
        mapView.controller.setCenter(center)
        mapView.controller.setZoom(zoom)
        mapView.invalidate()
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}

private fun cameraFor(
    city: City,
    cafes: List<Cafe>,
    userLocation: LocationProvider.UserLocation?,
): Pair<GeoPoint, Double> {
    if (userLocation != null) {
        return GeoPoint(userLocation.lat, userLocation.lon) to 15.5
    }
    if (cafes.isEmpty()) return GeoPoint(city.lat, city.lon) to 12.5

    var minLat = cafes.first().lat
    var maxLat = minLat
    var minLon = cafes.first().lon
    var maxLon = minLon

    cafes.forEach { cafe ->
        if (cafe.lat < minLat) minLat = cafe.lat
        if (cafe.lat > maxLat) maxLat = cafe.lat
        if (cafe.lon < minLon) minLon = cafe.lon
        if (cafe.lon > maxLon) maxLon = cafe.lon
    }

    val center = GeoPoint((minLat + maxLat) / 2.0, (minLon + maxLon) / 2.0)
    val span = max(maxLat - minLat, maxLon - minLon)
    val zoom = if (span <= 0.0) 15.5 else (ln(500.0 / span) / ln(2.0)).coerceIn(10.5, 17.0)
    return center to zoom
}
