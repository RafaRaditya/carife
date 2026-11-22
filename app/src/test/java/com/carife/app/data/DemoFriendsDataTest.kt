package com.carife.app.data

import com.carife.app.core.model.Cafe
import com.carife.app.core.model.DensityLevel
import com.carife.app.core.model.StudyRoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoFriendsDataTest {

    private fun cafe(
        id: String,
        name: String,
        density: DensityLevel? = null,
        outletCount: Int? = null,
    ) = Cafe(
        id = id,
        name = name,
        cityId = "bekasi",
        lat = -6.28,
        lon = 106.97,
        densityLevel = density,
        outletCount = outletCount,
    )

    @Test
    fun `default friends remain the three demo people`() {
        assertEquals(listOf("alex", "dinda", "rizky"), DemoFriendsData.defaultFriendIds)
    }

    @Test
    fun `friends are returned in directory order`() {
        val friends = DemoFriendsData.friends(setOf("rizky", "alex", "dinda"))

        assertEquals(listOf("alex", "dinda", "rizky"), friends.map { it.id })
    }

    @Test
    fun `resolve only includes added friends`() {
        val resolved = DemoFriendsData.resolve(setOf("alex", "nadia"), emptyList())

        assertEquals(listOf("alex", "nadia"), resolved.map { it.person.id })
    }

    @Test
    fun `feed is sorted from newest activity`() {
        val resolved = DemoFriendsData.resolve(
            setOf("alex", "dinda", "rizky", "nadia"),
            emptyList(),
        )

        assertEquals(listOf("alex", "nadia", "dinda", "rizky"), resolved.map { it.person.id })
    }

    @Test
    fun `pinned cafe id wins over name hints`() {
        val pinned = cafe("demo:kopitagram-galaxy", "Kopitagram Galaxy Bekasi")
        val resolved = DemoFriendsData.resolve(
            setOf("alex"),
            listOf(pinned, cafe("osm:1", "Starbucks Galaxy")),
        )

        assertEquals(pinned.id, resolved.single().cafe?.id)
    }

    @Test
    fun `resolution is deterministic regardless of catalog order`() {
        val friendIds = DemoFriendsData.people.map { it.id }.toSet()
        val cafes = (1..8).map { index -> cafe("osm:$index", "Kopi $index") }

        val first = DemoFriendsData.resolve(friendIds, cafes)
        val second = DemoFriendsData.resolve(friendIds, cafes.reversed())

        assertEquals(
            first.associate { it.person.id to it.cafe?.id },
            second.associate { it.person.id to it.cafe?.id },
        )
    }

    @Test
    fun `each friend gets a different cafe when alternatives exist`() {
        val friendIds = DemoFriendsData.people.map { it.id }.toSet()
        val cafes = (1..8).map { index -> cafe("osm:$index", "Kopi $index") }

        val resolved = DemoFriendsData.resolve(friendIds, cafes)

        assertEquals(resolved.size, resolved.mapNotNull { it.cafe?.id }.distinct().size)
    }

    @Test
    fun `friend stays bound to a cafe when hints miss`() {
        val resolved = DemoFriendsData.resolve(
            setOf("alex"),
            listOf(cafe("osm:9", "Kafe Tanpa Kecocokan")),
        )

        assertEquals("osm:9", resolved.single().cafe?.id)
    }

    @Test
    fun `friends stay visible when catalog is empty`() {
        val resolved = DemoFriendsData.resolve(DemoFriendsData.defaultFriendIds.toSet(), emptyList())

        assertEquals(3, resolved.size)
        resolved.forEach { assertNull(it.cafe) }
    }

    @Test
    fun `relative time label handles minutes hours and days`() {
        assertEquals("Baru saja", DemoFriendsData.relativeTimeLabel(0))
        assertEquals("12m lalu", DemoFriendsData.relativeTimeLabel(12))
        assertEquals("1 jam lalu", DemoFriendsData.relativeTimeLabel(60))
        assertEquals("2 hari lalu", DemoFriendsData.relativeTimeLabel(2 * 24 * 60))
    }

    @Test
    fun `badge reflects group project then cafe data`() {
        val groupSpot = DemoFriendsData.person("dinda")!!.spot!!
        val personalSpot = DemoFriendsData.person("alex")!!.spot!!
        val plainCafe = cafe("osm:1", "Kopi A")

        assertEquals("Grup Project", DemoFriendsData.badgeLabel(groupSpot, plainCafe))
        assertEquals(
            "Kondusif",
            DemoFriendsData.badgeLabel(
                personalSpot,
                cafe("osm:1", "Kopi A", density = DensityLevel.SEPI),
            ),
        )
        assertEquals(
            "Ada Colokan",
            DemoFriendsData.badgeLabel(
                personalSpot,
                cafe("osm:1", "Kopi A", outletCount = 4),
            ),
        )
        assertNull(DemoFriendsData.badgeLabel(personalSpot, plainCafe))
    }

    @Test
    fun `room cafe resolves by id hint or name`() {
        val cafes = listOf(
            cafe("osm:1", "Simetri Coffee Roasters"),
            cafe("osm:2", "Kopi Kenangan"),
        )

        assertEquals(
            "osm:1",
            DemoFriendsData.resolveRoomCafe(
                StudyRoom(
                    id = "room-1",
                    cafeId = "osm:1",
                    title = "Belajar",
                    schedule = "Hari ini",
                ),
                cafes,
            )?.id,
        )
        assertEquals(
            "osm:1",
            DemoFriendsData.resolveRoomCafe(
                StudyRoom(
                    id = "room-2",
                    cafeNameHint = "Simetri",
                    title = "Belajar",
                    schedule = "Hari ini",
                ),
                cafes,
            )?.id,
        )
        assertEquals(
            "osm:2",
            DemoFriendsData.resolveRoomCafe(
                StudyRoom(
                    id = "room-3",
                    cafeName = "Kopi Kenangan",
                    title = "Belajar",
                    schedule = "Hari ini",
                ),
                cafes,
            )?.id,
        )
        assertNull(
            DemoFriendsData.resolveRoomCafe(
                StudyRoom(
                    id = "room-4",
                    cafeName = "Kafe Hilang",
                    title = "Belajar",
                    schedule = "Hari ini",
                ),
                cafes,
            )
        )
    }

    @Test
    fun `demo room still has space for the user`() {
        val room = DemoFriendsData.demoRooms.single()
        val participantsAfterJoin = 1 + room.invitedFriendIds.size + 1

        assertTrue(participantsAfterJoin <= room.capacity)
    }
}
