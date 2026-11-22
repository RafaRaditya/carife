package com.carife.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeTertiary
import kotlin.math.abs

private val placeholderPalettes = listOf(
    listOf(Color(0xFFFFDBCC), Color(0xFFF5ECED)),
    listOf(Color(0xFFC4E9C7), Color(0xFFFBF1F2)),
    listOf(Color(0xFFFFDBC7), Color(0xFFFFF8F8)),
    listOf(Color(0xFFEFE6E7), Color(0xFFFFEAE1)),
)

@Composable
fun CafePhotoPlaceholder(
    seed: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    iconSize: Dp = 22.dp,
) {
    val palette = placeholderPalettes[abs(seed.hashCode()) % placeholderPalettes.size]
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalCafe,
            contentDescription = null,
            tint = when (placeholderPalettes.indexOf(palette)) {
                1 -> CarifeSecondary
                2 -> CarifeTertiary
                else -> CarifePrimary
            },
            modifier = Modifier.size(iconSize),
        )
    }
}
