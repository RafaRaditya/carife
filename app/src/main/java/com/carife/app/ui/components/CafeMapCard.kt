package com.carife.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.City
import com.carife.app.ui.theme.CarifeOutlineVariant
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest

@Composable
fun CafeMapCard(
    city: City,
    cafes: List<Cafe>,
    title: String,
    onOpenCafe: (String) -> Unit,
    onOpenFullMap: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    maxMarkers: Int = 20,
) {
    val pinCount = minOf(maxMarkers, cafes.size)
    val supporting = if (cafes.size > maxMarkers) {
        "$pinCount pin terdekat · ${cafes.size} spot"
    } else {
        "$pinCount pin · ketuk untuk lihat kafe"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CarifeSurfaceContainerLowest,
        border = BorderStroke(1.dp, CarifeOutlineVariant),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 10.dp, end = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = onOpenFullMap,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Buka peta",
                        style = MaterialTheme.typography.labelMedium,
                        color = CarifePrimary,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(14.dp)),
            ) {
                OsmMapView(
                    city = city,
                    cafes = cafes,
                    onOpenCafe = onOpenCafe,
                    maxMarkers = maxMarkers,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
