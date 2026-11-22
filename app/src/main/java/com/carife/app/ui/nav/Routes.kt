package com.carife.app.ui.nav

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val COMPARE = "compare"
    const val PROFILE = "profile"
    const val DETAIL = "detail/{cafeId}"
    const val ARG_CAFE_ID = "cafeId"

    fun detail(cafeId: String): String = "detail/${Uri.encode(cafeId)}"
}
