package com.carife.app.data.demo

import com.carife.app.core.model.Amenity
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.DensityLevel
import com.carife.app.core.model.MenuItem
import com.carife.app.core.model.NoiseLevel
import com.carife.app.core.model.ReviewSample
import com.carife.app.core.model.SmokingZone

/**
 * Angka contoh yang dikurasi manual untuk kafe yang tampil di prototipe.
 *
 * Nama, alamat, dan koordinat tetap milik OpenStreetMap. Yang ditambahkan hanya
 * field yang OSM tidak pernah menyediakan: harga, Wi-Fi, colokan, rating,
 * kepadatan, menu, dan ulasan.
 */
object DemoProfiles {

    data class Profile(
        val priceMin: Int? = null,
        val priceMax: Int? = null,
        val wifiMbps: Int? = null,
        val outletCount: Int? = null,
        val rating: Double? = null,
        val reviewCount: Int? = null,
        val noise: NoiseLevel? = null,
        val density: DensityLevel? = null,
        val densityPercent: Int? = null,
        val photoCount: Int? = null,
        val openingHoursRaw: String? = null,
        val menu: List<MenuItem> = emptyList(),
        val reviews: List<ReviewSample> = emptyList(),
    )

    private val studyMenu = listOf(
        MenuItem("Kopi Susu Gula Aren", 24_000),
        MenuItem("Americano", 20_000),
        MenuItem("Cappuccino", 28_000),
        MenuItem("V60 Single Origin", 32_000),
        MenuItem("Matcha Latte", 30_000),
        MenuItem("Croissant Butter", 22_000),
    )

    private val galaxyReviews = listOf(
        ReviewSample(
            author = "Sarah Maharani",
            initials = "SM",
            rating = 5,
            text = "Sebenarnya income-nya lumayan branding studio. Nyaman buat nugas lama, Wi-Fi stabil.",
            daysAgo = 16,
        ),
        ReviewSample(
            author = "Bagas Prakoso",
            initials = "BP",
            rating = 4,
            text = "Colokan ada di hampir semua meja. Sore agak ramai, tapi masih bisa fokus.",
            daysAgo = 9,
        ),
    )

    private val byName: Map<String, Profile> = mapOf(
        "kopitagram galaxy bekasi" to Profile(
            priceMin = 22_000,
            priceMax = 45_000,
            wifiMbps = 50,
            outletCount = 12,
            rating = 4.8,
            reviewCount = 330,
            noise = NoiseLevel.NORMAL,
            density = DensityLevel.SEDANG,
            densityPercent = 45,
            photoCount = 14,
            openingHoursRaw = "Mo-Su 08:00-22:00",
            menu = studyMenu,
            reviews = galaxyReviews,
        ),
        "koma junkyard bekasi timur" to Profile(
            priceMin = 20_000,
            priceMax = 42_000,
            wifiMbps = 45,
            outletCount = 8,
            rating = 4.6,
            reviewCount = 210,
            noise = NoiseLevel.RAMAI,
            density = DensityLevel.SEDANG,
            densityPercent = 52,
            photoCount = 11,
            openingHoursRaw = "Mo-Su 10:00-23:00",
            menu = studyMenu.drop(1).take(5),
            reviews = galaxyReviews.take(1),
        ),
        "titik temu galaxy bekasi" to Profile(
            priceMin = 28_000,
            priceMax = 50_000,
            wifiMbps = 90,
            outletCount = 14,
            rating = 4.7,
            reviewCount = 180,
            noise = NoiseLevel.HENING,
            density = DensityLevel.SEPI,
            densityPercent = 22,
            photoCount = 9,
            openingHoursRaw = "Mo-Su 07:00-22:00",
            menu = studyMenu,
            reviews = galaxyReviews.take(1),
        ),
        "simetri coffee roasters" to Profile(
            priceMin = 28_000,
            priceMax = 55_000,
            wifiMbps = 75,
            outletCount = 10,
            rating = 4.7,
            reviewCount = 240,
            noise = NoiseLevel.NORMAL,
            density = DensityLevel.SEDANG,
            densityPercent = 38,
            photoCount = 16,
            openingHoursRaw = "Mo-Su 07:00-21:00",
            menu = studyMenu,
            reviews = galaxyReviews,
        ),
        "starbucks taman galaxy" to Profile(
            priceMin = 30_000,
            priceMax = 65_000,
            wifiMbps = 50,
            outletCount = 9,
            rating = 4.5,
            reviewCount = 520,
            noise = NoiseLevel.RAMAI,
            density = DensityLevel.PENUH,
            densityPercent = 74,
            photoCount = 12,
            menu = studyMenu.take(4),
            reviews = galaxyReviews.take(1),
        ),
        "starbucks golden city" to Profile(
            priceMin = 30_000,
            priceMax = 65_000,
            wifiMbps = 50,
            outletCount = 7,
            rating = 4.4,
            reviewCount = 410,
            noise = NoiseLevel.NORMAL,
            density = DensityLevel.SEDANG,
            densityPercent = 55,
            photoCount = 8,
            menu = studyMenu.take(4),
            reviews = galaxyReviews.take(1),
        ),
        "chimica laboratory cafe" to Profile(
            priceMin = 25_000,
            priceMax = 48_000,
            wifiMbps = 75,
            outletCount = 11,
            rating = 4.6,
            reviewCount = 160,
            noise = NoiseLevel.HENING,
            density = DensityLevel.SEPI,
            densityPercent = 26,
            photoCount = 10,
            menu = studyMenu,
        ),
        "warunk upnormal" to Profile(
            priceMin = 15_000,
            priceMax = 35_000,
            wifiMbps = 30,
            outletCount = 14,
            rating = 4.3,
            reviewCount = 900,
            noise = NoiseLevel.RAMAI,
            density = DensityLevel.PENUH,
            densityPercent = 81,
            photoCount = 20,
            menu = studyMenu.take(5),
        ),
        "kopi nako" to Profile(
            priceMin = 18_000,
            priceMax = 38_000,
            wifiMbps = 20,
            outletCount = 6,
            rating = 4.2,
            reviewCount = 130,
            noise = NoiseLevel.NORMAL,
            density = DensityLevel.SEPI,
            densityPercent = 30,
            photoCount = 7,
        ),
    )

    private val byId: Map<String, Profile> = mapOf(
        "demo:kopitagram-galaxy" to byName.getValue("kopitagram galaxy bekasi"),
        "demo:koma-junkyard" to byName.getValue("koma junkyard bekasi timur"),
        "demo:titik-temu-galaxy" to byName.getValue("titik temu galaxy bekasi"),
    )

    fun profileFor(cafe: Cafe): Profile? =
        byId[cafe.id] ?: byName[cafe.name.trim().lowercase()]

    /** Mengisi field yang masih kosong. Nilai dari OSM selalu menang. */
    fun apply(cafe: Cafe, profile: Profile): Cafe = cafe.copy(
        priceMin = cafe.priceMin ?: profile.priceMin,
        priceMax = cafe.priceMax ?: profile.priceMax,
        wifiMbps = cafe.wifiMbps ?: profile.wifiMbps,
        outletCount = cafe.outletCount ?: profile.outletCount,
        rating = cafe.rating ?: profile.rating,
        reviewCount = cafe.reviewCount ?: profile.reviewCount,
        noiseLevel = cafe.noiseLevel ?: profile.noise,
        densityLevel = cafe.densityLevel ?: profile.density,
        densityPercent = cafe.densityPercent ?: profile.densityPercent,
        photoCount = cafe.photoCount ?: profile.photoCount,
        openingHoursRaw = cafe.openingHoursRaw ?: profile.openingHoursRaw,
        menu = if (cafe.menu.isEmpty()) profile.menu else cafe.menu,
        reviews = if (cafe.reviews.isEmpty()) profile.reviews else cafe.reviews,
    )

    /**
     * Kafe hero prototipe yang tidak terdaftar di OpenStreetMap.
     * Koordinat memakai titik nyata di Bekasi agar pin tetap jatuh di peta yang benar.
     */
    fun heroCafes(cityId: String): List<Cafe> {
        if (cityId != "bekasi") return emptyList()

        return listOf(
            Cafe(
                id = "demo:kopitagram-galaxy",
                name = "Kopitagram Galaxy Bekasi",
                cityId = cityId,
                area = "Grand Galaxy City",
                address = "Jl. Boulevard Raya, Grand Galaxy City",
                lat = -6.2834,
                lon = 106.9734,
                amenities = setOf(Amenity.WIFI, Amenity.AIR_CONDITIONED, Amenity.OUTDOOR_SEATING),
                smokingZone = SmokingZone.SEPARATE_AREA,
                isDemoData = true,
            ),
            Cafe(
                id = "demo:koma-junkyard",
                name = "Koma Junkyard Bekasi Timur",
                cityId = cityId,
                area = "Bekasi Timur",
                address = "Jl. Ir. H. Juanda, Bekasi Timur",
                lat = -6.2400,
                lon = 107.0000,
                amenities = setOf(Amenity.WIFI, Amenity.OUTDOOR_SEATING),
                smokingZone = SmokingZone.SMOKING_ALLOWED,
                isDemoData = true,
            ),
            Cafe(
                id = "demo:titik-temu-galaxy",
                name = "Titik Temu Galaxy Bekasi",
                cityId = cityId,
                area = "Grand Galaxy City",
                address = "Jl. Boulevard Utara, Grand Galaxy City",
                lat = -6.2850,
                lon = 106.9715,
                amenities = setOf(Amenity.WIFI, Amenity.AIR_CONDITIONED),
                smokingZone = SmokingZone.FULLY_NON_SMOKING,
                isDemoData = true,
            ),
        )
    }
}
