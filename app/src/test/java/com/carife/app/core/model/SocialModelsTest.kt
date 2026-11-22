package com.carife.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialModelsTest {

    @Test
    fun `initials use the first two words`() {
        assertEquals("DP", UserProfile(displayName = "Dimas Pratama").initials)
        assertEquals("DK", UserProfile(displayName = "  dinda   kirana  ").initials)
    }

    @Test
    fun `initials handle one word and blank names`() {
        assertEquals("A", UserProfile(displayName = "Alex").initials)
        assertEquals("DP", UserProfile(displayName = "   ").initials)
        assertEquals("DP", UserProfile(displayName = "").initials)
    }

    @Test
    fun `avatar palette colors are unique`() {
        assertEquals(profileAvatarColors.size, profileAvatarColors.distinct().size)
        assertTrue(profileAvatarColors.isNotEmpty())
    }
}
