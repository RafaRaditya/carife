package com.carife.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.Cafe
import com.carife.app.core.model.DensityLevel
import com.carife.app.domain.DistanceUtil
import com.carife.app.domain.OpeningHours
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifeOutlineVariant
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSurfaceContainer
import com.carife.app.ui.theme.CarifeTertiary
import java.time.Instant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CafeCard(
    cafe: Cafe,
    matchScore: Int?,
    isFavorite: Boolean,
    isSelectedForCompare: Boolean,
    now: Instant,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCompare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openNow = remember(cafe, now) { OpeningHours.isOpenNow(cafe, now) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box {
                CafePhotoPlaceholder(
                    seed = cafe.id,
                    modifier = Modifier.size(92.dp),
                    cornerRadius = 14.dp,
                )
                if (matchScore != null) {
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (matchScore >= 90) CarifePrimary else CarifeTertiary)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "$matchScore% Match",
                            style = MaterialTheme.typography.labelSmall,
                            color = CarifeOnPrimary,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = cafe.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val subtitle = listOfNotNull(cafe.area, cafe.address).firstOrNull()
                        Text(
                            text = subtitle ?: "Alamat belum tersedia",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = if (isFavorite) {
                                    "Hapus ${cafe.name} dari favorit"
                                } else {
                                    "Simpan ${cafe.name} ke favorit"
                                }
                            },
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    cafe.rating?.let { rating ->
                        Text(
                            text = "★ %.1f".format(rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = CarifeTertiary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    cafe.distanceMeters?.let { distance ->
                        MetaItem(Icons.Outlined.MeetingRoom, DistanceUtil.formatDistance(distance))
                    }
                    openNow?.let { isOpen ->
                        MetaItem(
                            icon = Icons.Outlined.AccessTime,
                            text = if (isOpen) "Buka" else "Tutup",
                            tint = if (isOpen) CarifeSecondary else CarifeTertiary,
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    if (cafe.hasWifi) {
                        MetaItem(Icons.Outlined.Wifi, cafe.wifiMbps?.let { "$it Mbps" } ?: "Wi-Fi")
                    }
                    cafe.outletCount?.let { MetaItem(Icons.Outlined.Power, "$it colokan") }
                    if (cafe.priceMin != null && cafe.priceMax != null) {
                        MetaItem(icon = null, text = "${formatRupiah(cafe.priceMin)}–${formatRupiah(cafe.priceMax)}")
                    } else {
                        cafe.priceMin?.let { min -> MetaItem(icon = null, text = "${formatRupiah(min)}+") }
                    }
                }

                cafe.densityLevel?.let { density ->
                    MetaItem(
                        icon = null,
                        text = "${density.label.uppercase()}${cafe.densityPercent?.let { " $it%" } ?: ""}",
                        tint = when (density) {
                            DensityLevel.SEPI -> CarifeSecondary
                            DensityLevel.SEDANG -> CarifeTertiary
                            DensityLevel.PENUH -> MaterialTheme.colorScheme.error
                        },
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isSelectedForCompare) CarifePrimary.copy(alpha = 0.12f)
                            else CarifeSurfaceContainer
                        )
                        .toggleable(
                            value = isSelectedForCompare,
                            role = Role.Checkbox,
                            onValueChange = { onToggleCompare() },
                        )
                        .padding(start = 4.dp, end = 14.dp),
                ) {
                    Checkbox(
                        checked = isSelectedForCompare,
                        onCheckedChange = null,
                        modifier = Modifier.size(44.dp),
                    )
                    Text(
                        text = if (isSelectedForCompare) "Dipilih" else "Bandingkan",
                        style = MaterialTheme.typography.labelMedium,
                        color = CarifePrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaItem(icon: ImageVector?, text: String, tint: androidx.compose.ui.graphics.Color? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
