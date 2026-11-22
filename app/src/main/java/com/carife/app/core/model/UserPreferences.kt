package com.carife.app.core.model

enum class BudgetBucket(val label: String, val min: Int?, val max: Int?) {
    UNDER_25K("< Rp25rb", null, 24_999),
    MID_25K_50K("25rb – 50rb", 25_000, 50_000),
    OVER_50K("> Rp50rb", 50_001, null),
    ANY("Bebas", null, null),
}

enum class WifiRequirement(val label: String, val minMbps: Int?, val strict: Boolean) {
    MIN_30("Min. 30 Mbps", 30, false),
    OVER_30("> 30 Mbps", 30, true),
    OVER_50("> 50 Mbps", 50, true),
    STANDARD("Ada Wi-Fi standar", null, false),
    ANY("Bebas", null, false),
}

enum class ZonePreference(val label: String) {
    NON_SMOKING_AC("Wajib Non-Smoking AC"),
    NON_SMOKING("Non-Smoking"),
    SEPARATE("Boleh Smoking Area"),
    ANY("Bebas"),
}

enum class RadiusPreference(val label: String, val minMeters: Int, val maxMeters: Int?) {
    NEAR("Dekat (<1 km)", 0, 999),
    MID("Sedang (1–3 km)", 1_000, 3_000),
    ANY("Bebas", 0, null),
}

data class UserPreferences(
    val budget: BudgetBucket = BudgetBucket.ANY,
    val wifi: WifiRequirement = WifiRequirement.ANY,
    val zone: ZonePreference = ZonePreference.ANY,
    val amenities: Set<Amenity> = emptySet(),
    val radius: RadiusPreference = RadiusPreference.ANY,
) {
    val hasActiveCriteria: Boolean
        get() = budget != BudgetBucket.ANY ||
            wifi != WifiRequirement.ANY ||
            zone != ZonePreference.ANY ||
            amenities.isNotEmpty() ||
            radius != RadiusPreference.ANY
}
