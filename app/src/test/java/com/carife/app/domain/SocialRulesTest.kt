package com.carife.app.domain

import com.carife.app.core.model.CheckIn
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialRulesTest {

    private fun checkIn(cafeId: String) = CheckIn(
        cafeId = cafeId,
        cafeName = "Kafe $cafeId",
    )

    @Test
    fun `join checks in when there is no current check-in`() {
        assertEquals(
            JoinCheckInDecision.CHECK_IN,
            SocialRules.joinCheckInDecision(null, "kopi-a"),
        )
    }

    @Test
    fun `join keeps check-in when the cafe is the same`() {
        assertEquals(
            JoinCheckInDecision.KEEP_CURRENT,
            SocialRules.joinCheckInDecision(checkIn("kopi-a"), "kopi-a"),
        )
    }

    @Test
    fun `join asks before moving check-in to another cafe`() {
        assertEquals(
            JoinCheckInDecision.ASK_TO_MOVE,
            SocialRules.joinCheckInDecision(checkIn("kopi-a"), "kopi-b"),
        )
    }
}
