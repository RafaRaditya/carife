package com.carife.app.domain

import com.carife.app.core.model.Cafe
import com.carife.app.core.model.UserPreferences

object CompareAdvisor {

    data class Advice(
        val bestForStudyId: String?,
        val cheapestId: String?,
        val summary: String?,
    )

    fun advise(cafes: List<Cafe>, preferences: UserPreferences): Advice {
        if (cafes.isEmpty()) return Advice(null, null, null)

        val scores = cafes.associate { cafe ->
            cafe.id to MatchScorer.score(cafe, preferences)
        }

        val bestForStudy = scores
            .filterValues { it != null && it > 0 }
            .maxWithOrNull(
                compareBy({ it.value }, { -1 * (cafes.first { c -> c.id == it.key }.distanceMeters ?: Int.MAX_VALUE) })
            )?.key

        val cheapest = cafes
            .filter { it.priceMin != null }
            .minByOrNull { it.priceMin!! }
            ?.id

        return Advice(
            bestForStudyId = bestForStudy,
            cheapestId = cheapest,
            summary = buildSummary(cafes, bestForStudy, cheapest),
        )
    }

    private fun buildSummary(cafes: List<Cafe>, bestForStudy: String?, cheapest: String?): String? {
        if (cafes.size < 2) return null

        val comparableFields = mutableListOf<String>()
        val prices = cafes.mapNotNull { it.priceMin }
        if (prices.size >= 2) comparableFields.add("harga")
        val wifi = cafes.mapNotNull { it.wifiMbps }
        if (wifi.size >= 2) comparableFields.add("wifi")
        val hours = cafes.mapNotNull { it.openingHoursRaw }
        if (hours.size >= 2) comparableFields.add("jam")
        val zones = cafes.mapNotNull { it.smokingZone }
        if (zones.size >= 2) comparableFields.add("zonasi")
        val facilities = cafes.filter { it.amenities.isNotEmpty() }
        if (facilities.size >= 2) comparableFields.add("fasilitas")

        if (comparableFields.isEmpty()) return null

        val parts = mutableListOf<String>()
        bestForStudy?.let { id ->
            cafes.firstOrNull { it.id == id }?.let { parts.add("${it.name} paling sesuai preferensi nugasmu") }
        }
        cheapest?.let { id ->
            val cafe = cafes.firstOrNull { it.id == id }
            if (cafe != null && cafe.id != bestForStudy) {
                parts.add("${cafe.name} paling hemat")
            } else if (cafe != null) {
                parts.add("sekaligus paling hemat")
            }
        }

        if (parts.isEmpty()) {
            val differing = differingFields(cafes)
            if (differing.isEmpty()) {
                return "Data kafe-kafe ini masih terlalu mirip untuk disimpulkan. Buka detail masing-masing untuk memastikan."
            }
            return "Perbedaan utama: ${differing.joinToString(", ")}."
        }

        return parts.joinToString(", ").replaceFirstChar { it.uppercase() } + ". Bandingkan detail lain di tabel."
    }

    private fun differingFields(cafes: List<Cafe>): List<String> {
        val fields = mutableListOf<String>()
        val prices = cafes.map { it.priceMin }
        if (prices.filterNotNull().distinct().size > 1) fields.add("harga")
        val wifi = cafes.map { it.wifiMbps }
        if (wifi.filterNotNull().distinct().size > 1) fields.add("kecepatan Wi-Fi")
        val zones = cafes.map { it.smokingZone }
        if (zones.filterNotNull().distinct().size > 1) fields.add("zonasi rokok")
        val hours = cafes.map { it.openingHoursRaw }
        if (hours.filterNotNull().distinct().size > 1) fields.add("jam operasional")
        return fields
    }
}
