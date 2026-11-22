package com.carife.app.data.demo

import com.carife.app.core.model.Cafe
import com.carife.app.core.model.DensityLevel
import com.carife.app.core.model.MenuItem
import com.carife.app.core.model.NoiseLevel
import com.carife.app.core.model.ReviewSample

/**
 * Membangkitkan angka contoh yang deterministik dari id kafe.
 *
 * Id yang sama selalu menghasilkan angka yang sama, sehingga nilai tidak berubah
 * saat aplikasi dibuka ulang atau saat data dibaca dari cache.
 */
object DemoDataGenerator {

    private val wifiSteps = listOf(10, 20, 30, 45, 50, 75, 90, 100)
    private val noiseCycle = listOf(NoiseLevel.HENING, NoiseLevel.NORMAL, NoiseLevel.RAMAI)

    private val drinkNames = listOf(
        "Kopi Susu Gula Aren",
        "Americano",
        "Cappuccino",
        "Latte",
        "V60 Single Origin",
        "Matcha Latte",
        "Teh Tarik",
        "Es Kopi Susu",
        "Chocolate Hazelnut",
        "Lemon Tea",
    )

    private val foodNames = listOf(
        "Croissant Butter",
        "Roti Bakar Keju",
        "Pisang Goreng Madu",
        "Kentang Goreng",
        "Mie Instan Telur",
    )

    private val reviewerNames = listOf(
        "Sarah Maharani" to "SM",
        "Bagas Prakoso" to "BP",
        "Nadia Utami" to "NU",
        "Rizky Ramadhan" to "RR",
        "Dinda Kirana" to "DK",
        "Alex Chandra" to "AC",
        "Fajar Nugroho" to "FN",
        "Intan Permata" to "IP",
    )

    private val reviewTexts = listOf(
        "Wi-Fi-nya kencang dan banyak colokan, nyaman buat nugas lama.",
        "Suasananya tenang di jam kuliah, pas buat kerja kelompok.",
        "Sore agak ramai tapi masih bisa fokus. Kopinya enak.",
        "Mejanya besar dan kursinya nyaman. Recommended buat belajar.",
        "Pelayanan cepat, tempat bersih, harga masih ramah kantong.",
    )

    fun seedOf(id: String): Int {
        var seed = 7
        for (char in id) {
            seed = seed * 31 + char.code
        }
        return seed and 0x7fffffff
    }

    /** Mengisi field yang masih kosong. Nilai yang sudah ada tidak ditimpa. */
    fun fill(cafe: Cafe): Cafe {
        val seed = seedOf(cafe.id)

        val priceMin = cafe.priceMin ?: (15 + pick(seed, 1, 20)) * 1_000
        val priceMax = cafe.priceMax ?: priceMin + (15 + pick(seed, 2, 25)) * 1_000
        val densityPercent = cafe.densityPercent ?: pick(seed, 3, 80) + 10

        return cafe.copy(
            priceMin = priceMin,
            priceMax = priceMax,
            wifiMbps = cafe.wifiMbps ?: wifiSteps[pick(seed, 4, wifiSteps.size - 1)],
            outletCount = cafe.outletCount ?: pick(seed, 5, 14),
            rating = cafe.rating ?: (38 + pick(seed, 6, 11)) / 10.0,
            reviewCount = cafe.reviewCount ?: pick(seed, 7, 480) + 20,
            noiseLevel = cafe.noiseLevel ?: noiseCycle[pick(seed, 8, noiseCycle.size - 1)],
            densityLevel = cafe.densityLevel ?: densityOf(densityPercent),
            densityPercent = densityPercent,
            photoCount = cafe.photoCount ?: pick(seed, 9, 18) + 3,
            menu = cafe.menu.ifEmpty { menuFor(seed) },
            reviews = cafe.reviews.ifEmpty { reviewsFor(seed) },
            isDemoData = true,
        )
    }

    private fun densityOf(percent: Int): DensityLevel = when {
        percent < 33 -> DensityLevel.SEPI
        percent < 66 -> DensityLevel.SEDANG
        else -> DensityLevel.PENUH
    }

    private fun menuFor(seed: Int): List<MenuItem> {
        val drinks = (0 until 4).map { offset ->
            val index = pick(seed, 20 + offset, drinkNames.size - 1)
            MenuItem(drinkNames[index], (18 + pick(seed, 30 + offset, 17)) * 1_000)
        }
        val food = (0 until 2).map { offset ->
            val index = pick(seed, 40 + offset, foodNames.size - 1)
            MenuItem(foodNames[index], (15 + pick(seed, 50 + offset, 20)) * 1_000)
        }
        return drinks + food
    }

    private fun reviewsFor(seed: Int): List<ReviewSample> = (0 until 2).map { offset ->
        val (author, initials) = reviewerNames[pick(seed, 60 + offset, reviewerNames.size - 1)]
        ReviewSample(
            author = author,
            initials = initials,
            rating = 4 + (pick(seed, 70 + offset, 1)),
            text = reviewTexts[pick(seed, 80 + offset, reviewTexts.size - 1)],
            daysAgo = pick(seed, 90 + offset, 40) + 2,
        )
    }

    private fun pick(seed: Int, salt: Int, maxExclusive: Int): Int {
        if (maxExclusive <= 0) return 0
        var value = (seed * 31 + salt * 7919) and 0x7fffffff
        value = value xor (value shr 13)
        return value % maxExclusive
    }
}
