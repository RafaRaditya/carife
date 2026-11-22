package com.carife.app.data.demo

import com.carife.app.core.model.Cafe

/**
 * Menempelkan data contoh di atas katalog OpenStreetMap.
 *
 * Nama, alamat, koordinat, jam, dan fasilitas dari OSM dibiarkan apa adanya.
 * Hanya field yang OSM tidak sediakan yang diisi dari profil kurasi atau
 * generator deterministik, lalu kafe ditandai sebagai data demo.
 */
object DemoDataOverlay {

    fun apply(cafes: List<Cafe>, cityId: String): List<Cafe> {
        val merged = (DemoProfiles.heroCafes(cityId) + cafes).distinctBy { it.id }

        return merged.map { cafe ->
            val curated = DemoProfiles.profileFor(cafe)
            val withCurated = if (curated != null) DemoProfiles.apply(cafe, curated) else cafe
            DemoDataGenerator.fill(withCurated)
        }
    }
}
