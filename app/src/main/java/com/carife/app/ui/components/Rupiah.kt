package com.carife.app.ui.components

import java.text.NumberFormat
import java.util.Locale

/**
 * Format rupiah ringkas untuk kartu: 22000 -> "Rp22rb", 25500 -> "Rp26rb".
 */
fun formatRupiah(amount: Int): String = when {
    amount % 1_000 == 0 -> "Rp${amount / 1_000}rb"
    else -> "Rp${Math.round(amount / 1_000.0)}rb"
}

/**
 * Format rupiah penuh untuk detail dan menu: 24000 -> "Rp24.000".
 */
fun formatRupiahFull(amount: Int): String =
    "Rp" + NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID")).format(amount)
