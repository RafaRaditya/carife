package com.carife.app.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val displayName: String = "Dimas Pratama",
    val tagline: String = "Productive Workspace Seeker",
    val status: String = "Solo Deep Worker",
    val avatarColor: Long = 0xFF814323,
    val hasPhoto: Boolean = false,
    val photoUpdatedAtEpochMillis: Long = 0L,
) {
    val initials: String
        get() {
            val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (parts.isEmpty()) return "DP"
            return parts.take(2).joinToString("") { part -> part.first().uppercase() }
        }
}

@Serializable
data class CheckIn(
    val cafeId: String? = null,
    val cafeName: String,
    val floor: String? = null,
    val note: String? = null,
    val createdAtEpochMillis: Long = 0L,
)

@Serializable
data class StudyRoom(
    val id: String,
    val cafeId: String? = null,
    val cafeName: String? = null,
    val cafeNameHint: String? = null,
    val hostFriendId: String? = null,
    val title: String,
    val schedule: String,
    val note: String = "",
    val invitedFriendIds: List<String> = emptyList(),
    val capacity: Int = 4,
    val createdAtEpochMillis: Long = 0L,
)

val profileAvatarColors: List<Long> = listOf(
    0xFF814323,
    0xFF45664B,
    0xFF804415,
    0xFF3F5D8C,
    0xFF7A4E8C,
    0xFF8C4E5D,
)
