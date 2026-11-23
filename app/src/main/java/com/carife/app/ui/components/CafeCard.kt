package com.carife.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.Cafe
import com.carife.app.data.demo.DemoCafePhotos
import com.carife.app.domain.DistanceUtil
import com.carife.app.domain.OpeningHours
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifeOutline
import com.carife.app.ui.theme.CarifeOutlineVariant
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeStar
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import com.carife.app.ui.theme.CarifeTertiary
import java.time.Instant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeaturedCafeCard(
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
        color = CarifeSurfaceContainerLowest,
        border = BorderStroke(1.dp, CarifeOutlineVariant),
        onClick = onOpen,
    ) {
        Column {
            Box {
                CafePhotoPlaceholder(
                    seed = cafe.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(152.dp),
                    cornerRadius = 0.dp,
                    iconSize = 32.dp,
                    showLabel = true,
                    photoAsset = DemoCafePhotos.assetFor(cafe),
                )
                if (matchScore != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CarifePrimary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                    ) {
                        Text(
                            text = "$matchScore% cocok",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CarifeOnPrimary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = CarifeSurfaceContainerLowest.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, CarifeOutlineVariant),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp),
                ) {
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
                            tint = if (isFavorite) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = cafe.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                MetaLine(cafe = cafe, openNow = openNow, matchScore = null)
                MetaLineFacts(cafe = cafe)
                Spacer(Modifier.height(2.dp))
                CompareToggle(
                    selected = isSelectedForCompare,
                    cafeName = cafe.name,
                    onToggle = onToggleCompare,
                )
            }
        }
    }
}

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
        shape = RoundedCornerShape(16.dp),
        color = CarifeSurfaceContainerLowest,
        border = BorderStroke(1.dp, CarifeOutlineVariant),
        onClick = onOpen,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CafePhotoPlaceholder(
                seed = cafe.id,
                modifier = Modifier.size(78.dp),
                cornerRadius = 12.dp,
                photoAsset = DemoCafePhotos.assetFor(cafe),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = cafe.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(40.dp)
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
                            tint = if (isFavorite) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                MetaLine(cafe = cafe, openNow = openNow, matchScore = matchScore)
                MetaLineFacts(cafe = cafe)

                CompareToggle(
                    selected = isSelectedForCompare,
                    cafeName = cafe.name,
                    onToggle = onToggleCompare,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaLine(cafe: Cafe, openNow: Boolean?, matchScore: Int?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        cafe.rating?.let { rating ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Star,
                    contentDescription = null,
                    tint = CarifeStar,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "%.1f".format(rating),
                    style = MaterialTheme.typography.labelSmall,
                    color = CarifeStar,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        cafe.distanceMeters?.let { distance ->
            MetaItem(Icons.Outlined.NearMe, DistanceUtil.formatDistance(distance))
        }
        openNow?.let { isOpen ->
            MetaItem(
                icon = Icons.Outlined.AccessTime,
                text = if (isOpen) "Buka" else "Tutup",
                tint = if (isOpen) CarifeSecondary else CarifeTertiary,
            )
        }
        matchScore?.let { score ->
            Text(
                text = "$score% cocok",
                style = MaterialTheme.typography.labelSmall,
                color = CarifePrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetaLineFacts(cafe: Cafe) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        if (cafe.hasWifi) {
            MetaItem(Icons.Outlined.Wifi, cafe.wifiMbps?.let { "$it Mbps" } ?: "Wi-Fi")
        }
        cafe.outletCount?.let { count ->
            MetaItem(Icons.Outlined.Power, "$count colokan")
        }
        if (cafe.priceMin != null && cafe.priceMax != null) {
            MetaItem(
                icon = null,
                text = "${formatRupiah(cafe.priceMin)}–${formatRupiah(cafe.priceMax)}",
            )
        } else {
            cafe.priceMin?.let { min ->
                MetaItem(icon = null, text = "${formatRupiah(min)}+")
            }
        }
    }
}

@Composable
private fun CompareToggle(
    selected: Boolean,
    cafeName: String,
    onToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .semantics {
                contentDescription = if (selected) {
                    "Hapus $cafeName dari perbandingan"
                } else {
                    "Tambahkan $cafeName ke perbandingan"
                }
            }
            .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = selected,
                onCheckedChange = null,
                modifier = Modifier.size(20.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = CarifePrimary,
                    uncheckedColor = CarifeOutline,
                    checkmarkColor = Color.White,
                ),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = if (selected) "Dipilih untuk banding" else "Bandingkan",
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetaItem(
    icon: ImageVector?,
    text: String,
    tint: Color? = null,
) {
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
