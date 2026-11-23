package com.carife.app.data.demo

import com.carife.app.core.model.Cafe

/**
 * Foto suasana contoh dari prototipe.
 *
 * Kafe yang punya foto kurasi memakai fotonya sendiri. Kafe OSM lain memakai
 * foto dari kolam yang sama secara deterministik.
 */
object DemoCafePhotos {

    private const val DIR = "photos/"

    private val curatedById = mapOf(
        "demo:kopitagram-galaxy" to "cafe_kopitagram.jpg",
        "demo:koma-junkyard" to "cafe_koma.jpg",
        "demo:titik-temu-galaxy" to "cafe_titik_temu.jpg",
    )

    private val curatedByName = listOf(
        "simetri coffee roasters" to "cafe_simetri.jpg",
    )

    private val pool = listOf(
        "cafe_scandinavian.jpg",
        "cafe_roastery.jpg",
        "cafe_minimalist.jpg",
        "cafe_industrial.jpg",
        "cafe_japanese.jpg",
    )

    fun assetFor(cafe: Cafe): String {
        curatedById[cafe.id]?.let { return DIR + it }

        val normalizedName = cafe.name.trim().lowercase()
        curatedByName.firstOrNull { (needle, _) -> normalizedName.contains(needle) }?.let {
            return DIR + it.second
        }

        val index = (cafe.id.hashCode() and Int.MAX_VALUE) % pool.size
        return DIR + pool[index]
    }
}
