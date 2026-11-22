package com.carife.app.data

import com.carife.app.core.model.Cafe
import com.carife.app.core.model.DensityLevel
import com.carife.app.core.model.StudyRoom

data class FriendPerson(
    val id: String,
    val name: String,
    val initials: String,
    val avatarColor: Long,
    val campus: String,
    val isOnline: Boolean,
    val spot: FriendSpot?,
)

data class FriendSpot(
    val minutesAgo: Int,
    val note: String,
    val groupProject: Boolean,
    val floor: String? = null,
    val pinnedCafeId: String? = null,
    val nameHints: List<String> = emptyList(),
)

data class FriendSpotResolved(
    val person: FriendPerson,
    val spot: FriendSpot,
    val cafe: Cafe?,
)

object DemoFriendsData {

    val defaultFriendIds: List<String> = listOf("alex", "dinda", "rizky")

    val people: List<FriendPerson> = listOf(
        FriendPerson(
            id = "alex",
            name = "Alex Chandra",
            initials = "AC",
            avatarColor = 0xFF45664B,
            campus = "BINUS Bekasi",
            isOnline = true,
            spot = FriendSpot(
                minutesAgo = 12,
                note = "Lagi ngerjain kalkulus semester 4, ada 2 kursi kosong nih. Gas gabung!",
                groupProject = false,
                floor = "Lt. 2",
                pinnedCafeId = "demo:kopitagram-galaxy",
                nameHints = listOf("Starbucks", "Starbuck", "Kopi"),
            ),
        ),
        FriendPerson(
            id = "dinda",
            name = "Dinda Kirana",
            initials = "DK",
            avatarColor = 0xFF814323,
            campus = "Universitas Gunadarma",
            isOnline = true,
            spot = FriendSpot(
                minutesAgo = 35,
                note = "Bahas tugas kelompok DKV sampai jam 5 sore. Boleh ikut nimbrung.",
                groupProject = true,
                nameHints = listOf("Simetri Coffee Roasters", "Kopi"),
            ),
        ),
        FriendPerson(
            id = "rizky",
            name = "Rizky Ramadhan",
            initials = "RR",
            avatarColor = 0xFF3F5D8C,
            campus = "Universitas Presiden",
            isOnline = false,
            spot = FriendSpot(
                minutesAgo = 60,
                note = "Ngopi santai sambil baca jurnal. Boleh mampir.",
                groupProject = false,
                pinnedCafeId = "demo:koma-junkyard",
                nameHints = listOf("Warkop", "Warung", "Kopi"),
            ),
        ),
        FriendPerson(
            id = "nadia",
            name = "Nadia Utami",
            initials = "NU",
            avatarColor = 0xFF7A4E8C,
            campus = "Universitas Indonesia",
            isOnline = true,
            spot = FriendSpot(
                minutesAgo = 18,
                note = "Ngerjain skripsi bab 3. Butuh teman biar nggak ngantuk.",
                groupProject = false,
                floor = "Lt. 3",
                nameHints = listOf("Kopi Kenangan", "Kopi", "Coffee"),
            ),
        ),
        FriendPerson(
            id = "bagas",
            name = "Bagas Prakoso",
            initials = "BP",
            avatarColor = 0xFF8C4E5D,
            campus = "Institut Teknologi Bandung",
            isOnline = true,
            spot = FriendSpot(
                minutesAgo = 48,
                note = "Latihan presentasi kelompok sore ini. Yang mau bantu review, gabung aja.",
                groupProject = true,
                nameHints = listOf("Starbucks", "Kopi"),
            ),
        ),
        FriendPerson(
            id = "fajar",
            name = "Fajar Nugroho",
            initials = "FN",
            avatarColor = 0xFF2F6F6B,
            campus = "Universitas Brawijaya",
            isOnline = false,
            spot = FriendSpot(
                minutesAgo = 90,
                note = "Belajar buat ujian minggu depan. Nugas bareng yuk.",
                groupProject = false,
                nameHints = listOf("Coffee", "Cafe", "Kopi"),
            ),
        ),
        FriendPerson(
            id = "intan",
            name = "Intan Permata",
            initials = "IP",
            avatarColor = 0xFF8A6D3B,
            campus = "Universitas Padjadjaran",
            isOnline = true,
            spot = FriendSpot(
                minutesAgo = 150,
                note = "Nyusun portofolio sambil ngopi. Mampir kalau lewat.",
                groupProject = false,
                nameHints = listOf("Kopi", "Coffee"),
            ),
        ),
        FriendPerson(
            id = "sarah",
            name = "Sarah Maharani",
            initials = "SM",
            avatarColor = 0xFF5A5A8C,
            campus = "Universitas Gadjah Mada",
            isOnline = false,
            spot = FriendSpot(
                minutesAgo = 240,
                note = "Baca jurnal buat tesis. Suasananya enak buat fokus.",
                groupProject = false,
                nameHints = listOf("Cafe", "Coffee", "Kopi"),
            ),
        ),
    )

    val demoRooms: List<StudyRoom> = listOf(
        StudyRoom(
            id = "demo:room-dkv",
            cafeNameHint = "Kopi",
            hostFriendId = "dinda",
            title = "Tugas Kelompok DKV",
            schedule = "Hari ini · 16.00",
            note = "Bawa laptop dan referensi moodboard.",
            invitedFriendIds = listOf("nadia", "bagas"),
            capacity = 5,
        ),
    )

    private val cafeOrder = compareBy<Cafe> { it.name.lowercase() }.thenBy { it.id }

    fun friends(friendIds: Set<String>): List<FriendPerson> =
        people.filter { it.id in friendIds }

    fun person(id: String): FriendPerson? = people.firstOrNull { it.id == id }

    /**
     * Mengikat setiap aktivitas teman yang sudah ditambahkan ke kafe di katalog
     * kota aktif. Urutan hasil dari aktivitas terbaru dan satu kafe tidak dipakai
     * dua teman selama masih ada kafe lain di katalog.
     */
    fun resolve(friendIds: Set<String>, cafes: List<Cafe>): List<FriendSpotResolved> {
        val usedCafeIds = mutableSetOf<String>()
        return people
            .asSequence()
            .filter { it.id in friendIds && it.spot != null }
            .sortedBy { it.spot!!.minutesAgo }
            .map { person ->
                val spot = person.spot!!
                val cafe = findCafe(spot, cafes, usedCafeIds)
                cafe?.let { usedCafeIds += it.id }
                FriendSpotResolved(person, spot, cafe)
            }
            .toList()
    }

    private fun findCafe(spot: FriendSpot, cafes: List<Cafe>, usedCafeIds: Set<String>): Cafe? {
        val available = cafes.filterNot { it.id in usedCafeIds }

        spot.pinnedCafeId
            ?.let { id -> available.firstOrNull { it.id == id } }
            ?.let { return it }

        for (hint in spot.nameHints) {
            available
                .filter { it.name.contains(hint, ignoreCase = true) }
                .minWithOrNull(cafeOrder)
                ?.let { return it }
        }

        return available.minWithOrNull(cafeOrder)
    }

    fun resolveRoomCafe(room: StudyRoom, cafes: List<Cafe>): Cafe? {
        room.cafeId
            ?.let { id -> cafes.firstOrNull { it.id == id } }
            ?.let { return it }

        room.cafeNameHint
            ?.let { hint ->
                cafes.filter { it.name.contains(hint, ignoreCase = true) }
                    .minWithOrNull(cafeOrder)
            }
            ?.let { return it }

        return room.cafeName
            ?.let { name -> cafes.firstOrNull { it.name.equals(name, ignoreCase = true) } }
    }

    fun badgeLabel(spot: FriendSpot, cafe: Cafe): String? = when {
        spot.groupProject -> "Grup Project"
        cafe.densityLevel == DensityLevel.SEPI -> "Kondusif"
        (cafe.outletCount ?: 0) > 0 -> "Ada Colokan"
        cafe.hasWifi -> "Wi-Fi"
        else -> null
    }

    fun relativeTimeLabel(minutesAgo: Int): String = when {
        minutesAgo <= 0 -> "Baru saja"
        minutesAgo < 60 -> "${minutesAgo}m lalu"
        minutesAgo < 24 * 60 -> "${minutesAgo / 60} jam lalu"
        else -> "${minutesAgo / (24 * 60)} hari lalu"
    }
}
