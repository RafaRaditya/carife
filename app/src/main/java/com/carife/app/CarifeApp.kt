package com.carife.app

import android.app.Application
import com.carife.app.data.CafeRepository
import com.carife.app.data.CityRepository
import com.carife.app.data.local.BundledCatalog
import com.carife.app.data.local.CafeCache
import com.carife.app.data.local.PreferencesStore
import com.carife.app.data.remote.OverpassClient
import com.carife.app.location.LocationProvider
import org.osmdroid.config.Configuration
import java.io.File

class AppContainer(application: Application) {

    val cityRepository = CityRepository(application)

    val preferencesStore = PreferencesStore(application)

    val bundledCatalog = BundledCatalog(application)

    val cafeRepository = CafeRepository(
        client = OverpassClient(),
        cache = CafeCache(File(application.cacheDir, "cafes")),
        bundled = bundledCatalog,
        networkEnabled = !BuildConfig.OFFLINE_ONLY,
    )

    val locationProvider = LocationProvider(application)
}

class CarifeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        configureOsmdroid()
        container = AppContainer(this)
    }

    private fun configureOsmdroid() {
        val osmConfiguration = Configuration.getInstance()
        osmConfiguration.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        val basePath = File(cacheDir, "osmdroid")
        val tileCache = File(basePath, "tiles")
        basePath.mkdirs()
        tileCache.mkdirs()
        osmConfiguration.osmdroidBasePath = basePath
        osmConfiguration.osmdroidTileCache = tileCache
        osmConfiguration.userAgentValue = "Carife/1.0 ($packageName)"
    }
}
