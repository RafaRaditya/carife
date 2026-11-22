package com.carife.app.core.model

enum class SortOption(val label: String) {
    MATCH("Kecocokan"),
    NEAREST("Jarak Terdekat"),
    CHEAPEST("Harga Termurah"),
}

data class ExploreFilters(
    val query: String = "",
    val budget: BudgetBucket? = null,
    val wifi: WifiRequirement? = null,
    val zone: ZonePreference? = null,
    val amenities: Set<Amenity> = emptySet(),
    val distance: RadiusPreference? = null,
    val openNowOnly: Boolean = false,
    val noise: NoiseLevel? = null,
    val minOutlets: Int? = null,
    val priceCap: Int? = null,
    val sort: SortOption = SortOption.MATCH,
) {
    val explicitCriteriaCount: Int
        get() = listOfNotNull(budget, wifi, zone, distance, noise, minOutlets, priceCap).size +
            amenities.size +
            (if (openNowOnly) 1 else 0)

    fun withoutCriteria(): ExploreFilters = copy(
        budget = null,
        wifi = null,
        zone = null,
        amenities = emptySet(),
        distance = null,
        openNowOnly = false,
        noise = null,
        minOutlets = null,
        priceCap = null,
    )

    val hasExplicitCriteria: Boolean get() = explicitCriteriaCount > 0
}
