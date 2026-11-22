package com.carife.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.City
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun IllustratedMap(
    city: City,
    cafes: List<Cafe>,
    onPinClick: (Cafe) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp? = 160.dp,
    maxPins: Int = 10,
    onMapClick: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val pins = remember(cafes, city, maxPins) { normalizedPins(city, cafes, maxPins) }

    val blockGap = with(density) { 3.dp.toPx() }
    val blockCorner = with(density) { 3.dp.toPx() }
    val mainRoadWidth = with(density) { 4.dp.toPx() }
    val sideRoadWidth = with(density) { 3.dp.toPx() }
    val haloRadius = with(density) { 34.dp.toPx() }
    val centerDotRadius = with(density) { 5.dp.toPx() }
    val pinOuterRadius = with(density) { 10.dp.toPx() }
    val pinInnerRadius = with(density) { 6.dp.toPx() }
    val pinHitRadius = with(density) { 24.dp.toPx() }

    val semanticsModifier = if (onMapClick != null) {
        Modifier.semantics {
            contentDescription = "Peta ilustrasi ${city.name}, ${pins.size} pin kafe"
            onClick(label = "Buka peta penuh") {
                onMapClick()
                true
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF1EDE8))
            .then(semanticsModifier),
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(pins, pinHitRadius) {
                    detectTapGestures { tap ->
                        val nearest = pins.minByOrNull { (position, _) ->
                            val dx = position.x * size.width - tap.x
                            val dy = position.y * size.height - tap.y
                            dx * dx + dy * dy
                        }
                        val hit = nearest?.takeIf { (position, _) ->
                            val dx = position.x * size.width - tap.x
                            val dy = position.y * size.height - tap.y
                            dx * dx + dy * dy <= pinHitRadius * pinHitRadius
                        }
                        if (hit != null) onPinClick(hit.second) else onMapClick?.invoke()
                    }
                },
        ) {
            val w = size.width
            val h = size.height

            val blockColor = Color(0xFFE4DED8)
            for (row in 0..5) {
                for (col in 0..3) {
                    if ((row + col) % 3 == 0) continue
                    drawRoundRect(
                        color = blockColor,
                        topLeft = Offset(col * w / 4f + blockGap, row * h / 6f + blockGap),
                        size = Size(w / 4f - blockGap * 2, h / 6f - blockGap * 2),
                        cornerRadius = CornerRadius(blockCorner, blockCorner),
                    )
                }
            }

            val road = Path().apply {
                moveTo(0f, h * 0.62f)
                cubicTo(w * 0.3f, h * 0.5f, w * 0.6f, h * 0.75f, w, h * 0.55f)
            }
            drawPath(road, color = Color(0xFFD6CFC7), style = Stroke(width = mainRoadWidth))

            val road2 = Path().apply {
                moveTo(w * 0.42f, 0f)
                cubicTo(w * 0.5f, h * 0.4f, w * 0.36f, h * 0.7f, w * 0.55f, h)
            }
            drawPath(road2, color = Color(0xFFD6CFC7), style = Stroke(width = sideRoadWidth))

            drawCircle(
                color = CarifeSecondary.copy(alpha = 0.12f),
                radius = min(w, h) * 0.34f,
                center = Offset(w / 2f, h / 2f),
            )
            drawCircle(
                color = CarifeSecondary.copy(alpha = 0.35f),
                radius = haloRadius,
                center = Offset(w / 2f, h / 2f),
            )
            drawCircle(color = CarifeSecondary, radius = centerDotRadius, center = Offset(w / 2f, h / 2f))

            pins.forEach { (position, _) ->
                val center = Offset(position.x * w, position.y * h)
                drawCircle(color = Color.White, radius = pinOuterRadius, center = center)
                drawCircle(color = CarifePrimary, radius = pinInnerRadius, center = center)
            }
        }

        Text(
            text = "${city.name} & sekitarnya",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.92f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

private fun normalizedPins(city: City, cafes: List<Cafe>, maxPins: Int): List<Pair<Offset, Cafe>> {
    val positioned = cafes.filter { it.lat.isFinite() && it.lon.isFinite() }
    if (positioned.isEmpty()) return emptyList()

    val shown = positioned.take(maxPins)
    val maxDelta = shown.maxOf { cafe ->
        max(abs(cafe.lat - city.lat), abs(cafe.lon - city.lon))
    }.coerceAtLeast(0.002)

    return shown.map { cafe ->
        val x = 0.5f + ((cafe.lon - city.lon) / (maxDelta * 2.2)).toFloat()
        val y = 0.5f - ((cafe.lat - city.lat) / (maxDelta * 2.2)).toFloat()
        Offset(x.coerceIn(0.06f, 0.94f), y.coerceIn(0.1f, 0.9f)) to cafe
    }
}
