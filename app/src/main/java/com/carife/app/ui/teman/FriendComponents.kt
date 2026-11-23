package com.carife.app.ui.teman

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.Cafe
import com.carife.app.data.DemoFriendsData
import com.carife.app.data.FriendPerson
import com.carife.app.data.FriendSpotResolved
import com.carife.app.domain.DistanceUtil
import com.carife.app.ui.theme.CarifeOnPrimary
import com.carife.app.ui.theme.CarifeOnTertiaryFixed
import com.carife.app.ui.theme.CarifeOutlineVariant
import com.carife.app.ui.theme.CarifePrimary
import com.carife.app.ui.theme.CarifeSecondary
import com.carife.app.ui.theme.CarifeSecondaryContainer
import com.carife.app.ui.theme.CarifeSurfaceContainerLow
import com.carife.app.ui.theme.CarifeSurfaceContainerLowest
import com.carife.app.ui.theme.CarifeTertiaryFixed

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FriendSpotCard(
    resolved: FriendSpotResolved,
    joined: Boolean,
    onToggleJoin: () -> Unit,
    onOpenCafe: (String) -> Unit,
) {
    val person = resolved.person
    val spot = resolved.spot
    val cafe = resolved.cafe
    val badge = cafe?.let { DemoFriendsData.badgeLabel(spot, it) }
    val openCafe = cafe?.let { target ->
        Modifier
            .clickable(
                role = Role.Button,
                onClickLabel = "Buka ${target.name}",
            ) { onOpenCafe(target.id) }
            .semantics(mergeDescendants = true) {}
    } ?: Modifier

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(openCafe),
        shape = RoundedCornerShape(16.dp),
        color = if (joined) {
            CarifeSecondaryContainer.copy(alpha = 0.5f)
        } else {
            CarifeSurfaceContainerLowest
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (joined) CarifeSecondary.copy(alpha = 0.4f) else CarifeOutlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FriendAvatar(person, 42.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = person.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = DemoFriendsData.relativeTimeLabel(spot.minutesAgo),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = if (cafe != null) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = cafe?.name ?: "Belum ada kafe di kota ini",
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (cafe != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (cafe != null) CarifePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        spot.floor?.let { floor ->
                            Text(
                                text = "($floor)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                badge?.let { label ->
                    FriendBadge(label = label, prominent = spot.groupProject)
                }
            }

            NoteBubble(spot.note)

            if (joined) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = CarifeSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = "Kamu ikut nongkrong di sini.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = CarifeSecondary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    cafe?.let { resolvedCafe ->
                        cafeMeta(resolvedCafe).forEach { meta -> FriendMetaItem(meta) }
                    }
                }
                cafe?.let {
                    FriendCta(
                        label = if (joined) "Tergabung" else "Gabung",
                        prominent = !joined,
                        selected = joined,
                        enabled = true,
                        onClick = onToggleJoin,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FriendAvatar(person: FriendPerson, size: Dp) {
    Box {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(person.avatarColor)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                person.initials,
                color = CarifeOnPrimary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(12.dp)
                .clip(CircleShape)
                .background(if (person.isOnline) CarifeSecondary else MaterialTheme.colorScheme.outline)
                .border(2.dp, CarifeSurfaceContainerLowest, CircleShape),
        )
    }
}

@Composable
internal fun NoteBubble(note: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CarifeSurfaceContainerLow)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = CarifePrimary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "\"$note\"",
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun FriendBadge(label: String, prominent: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (prominent) CarifeTertiaryFixed else CarifeSurfaceContainerLow,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (prominent) CarifeOnTertiaryFixed else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
internal fun FriendCta(
    label: String,
    prominent: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background = when {
        !enabled -> CarifeSurfaceContainerLow
        selected -> CarifeSecondaryContainer
        prominent -> CarifePrimary
        else -> CarifeSurfaceContainerLow
    }
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        selected -> CarifeSecondary
        prominent -> CarifeOnPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = background,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(15.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}

internal data class FriendMeta(val icon: ImageVector, val label: String)

@Composable
internal fun FriendMetaItem(meta: FriendMeta) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = meta.icon,
            contentDescription = null,
            tint = CarifeSecondary,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = meta.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun cafeMeta(cafe: Cafe): List<FriendMeta> = buildList {
    cafe.wifiMbps?.let { add(FriendMeta(Icons.Outlined.Wifi, "$it Mbps")) }
    cafe.noiseLevel?.let { add(FriendMeta(Icons.AutoMirrored.Outlined.VolumeDown, it.label)) }
    cafe.outletCount?.let { add(FriendMeta(Icons.Outlined.Power, "$it colokan")) }
    cafe.distanceMeters?.let { add(FriendMeta(Icons.Outlined.NearMe, DistanceUtil.formatDistance(it))) }
}
