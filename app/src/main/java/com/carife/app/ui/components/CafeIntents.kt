package com.carife.app.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.carife.app.core.model.Cafe
import com.carife.app.domain.ShareTextBuilder

internal fun openCafeDirections(context: Context, cafe: Cafe, onMessage: (String) -> Unit) {
    val directions = Intent(Intent.ACTION_VIEW, Uri.parse(ShareTextBuilder.directionsUrl(cafe)))
    try {
        context.startActivity(directions)
    } catch (_: ActivityNotFoundException) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val address = cafe.address?.takeIf { it.isNotBlank() }
        val text = address ?: ShareTextBuilder.mapsUrl(cafe)
        clipboard.setPrimaryClip(ClipData.newPlainText(cafe.name, text))
        onMessage(if (address != null) "Alamat disalin." else "Tautan peta disalin.")
    }
}

internal fun openShareSheet(context: Context, text: String, onMessage: (String) -> Unit): Boolean {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return try {
        context.startActivity(Intent.createChooser(intent, "Bagikan lewat"))
        true
    } catch (_: ActivityNotFoundException) {
        onMessage("Menu berbagi belum bisa dibuka. Coba lagi.")
        false
    }
}
