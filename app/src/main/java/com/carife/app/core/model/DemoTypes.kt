package com.carife.app.core.model

/**
 * Tingkat kebisingan contoh untuk filter "Hening".
 * Nilai ini bukan hasil pengukuran, melainkan bagian dari data demo.
 */
enum class NoiseLevel(val label: String) {
    HENING("Hening"),
    NORMAL("Normal"),
    RAMAI("Ramai"),
}

/**
 * Tingkat kepadatan meja contoh untuk indikator "Simulasi kepadatan".
 */
enum class DensityLevel(val label: String) {
    SEPI("Sepi"),
    SEDANG("Sedang"),
    PENUH("Penuh"),
}

data class MenuItem(
    val name: String,
    val price: Int,
)

data class ReviewSample(
    val author: String,
    val initials: String,
    val rating: Int,
    val text: String,
    val daysAgo: Int,
)
