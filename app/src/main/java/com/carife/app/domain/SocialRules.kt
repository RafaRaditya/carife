package com.carife.app.domain

import com.carife.app.core.model.CheckIn

enum class JoinCheckInDecision {
    CHECK_IN,
    KEEP_CURRENT,
    ASK_TO_MOVE,
}

object SocialRules {

    /**
     * Gabung ke spot teman otomatis check-in jika belum ada check-in.
     * Jika sudah check-in di kafe lain, UI menanyakan apakah check-in dipindahkan;
     * status gabung tetap disimpan apa pun pilihannya.
     */
    fun joinCheckInDecision(currentCheckIn: CheckIn?, targetCafeId: String): JoinCheckInDecision = when {
        currentCheckIn == null -> JoinCheckInDecision.CHECK_IN
        currentCheckIn.cafeId == targetCafeId -> JoinCheckInDecision.KEEP_CURRENT
        else -> JoinCheckInDecision.ASK_TO_MOVE
    }
}
