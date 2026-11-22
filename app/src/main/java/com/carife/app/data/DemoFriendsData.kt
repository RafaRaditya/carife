package com.carife.app.data

import com.carife.app.core.model.Cafe
import com.carife.app.domain.DistanceUtil

data class FriendSpot(
    val id: String,
    val name: String,
    val initials: String,
    val timeAgo: String,
    val cafeNameHint: String,
    val note: String,
    val tags: List<String>,
    val groupProject: Boolean,
    val avatarColor: Long,
)

data class FriendSpotResolved(
    val spot: FriendSpot,
    val cafe: Cafe?,
)

object DemoFriendsData {

    val spots: List<FriendSpot> = listOf(
        FriendSpot(
            id = "alex",
            name = "Alex Chandra",
            initials = "AC",
            timeAgo = "12m lalu",
            cafeNameHint = "Starbucks",
            note = "Lagi ngerjain kalkulus semester 4, ada 2 kursi kosong nih. Gas gabung!",
            tags = listOf("Ada Colokan"),
            groupProject = false,
            avatarColor = 0xFF45664B,
        ),
        FriendSpot(
            id = "dinda",
            name = "Dinda Kirana",
            initials = "DK",
            timeAgo = "35m lalu",
            cafeNameHint = "Kopi",
            note = "Bahas tugas kelompok DKV sampai jam 5 sore. Outdoor area adem banget.",
            tags = listOf("Promo Pelajar"),
            groupProject = true,
            avatarColor = 0xFF814323,
        ),
        FriendSpot(
            id = "rizky",
            name = "Rizky Ramadhan",
            initials = "RR",
            timeAgo = "1 jam lalu",
            cafeNameHint = "Warkop",
            note = "Ngopi santai baca jurnal. Nggak terlalu rame sekarang.",
            tags = listOf("Banyak Steker"),
            groupProject = false,
            avatarColor = 0xFF804415,
        ),
    )

    fun resolve(spots: List<FriendSpot>, cafes: List<Cafe>): List<FriendSpotResolved> =
        spots.map { spot ->
            val match = cafes
                .filter { it.name.contains(spot.cafeNameHint, ignoreCase = true) }
                .minByOrNull { it.distanceMeters ?: Int.MAX_VALUE }
            FriendSpotResolved(spot, match)
        }

    fun summaryText(spot: FriendSpot, cafe: Cafe): String = buildString {
        append(cafe.name)
        cafe.distanceMeters?.let { append(" • ").append(DistanceUtil.formatDistance(it)) }
    }
}
