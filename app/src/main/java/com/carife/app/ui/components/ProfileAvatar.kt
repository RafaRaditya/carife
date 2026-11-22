package com.carife.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.carife.app.core.model.UserProfile
import com.carife.app.data.local.ProfilePhotoStore
import com.carife.app.ui.theme.CarifeOnPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProfileAvatar(
    profile: UserProfile,
    size: Dp,
    modifier: Modifier = Modifier,
    pendingPhotoUri: Uri? = null,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        profile.hasPhoto,
        profile.photoUpdatedAtEpochMillis,
        pendingPhotoUri,
    ) {
        value = withContext(Dispatchers.IO) {
            when {
                pendingPhotoUri != null -> decodeUri(context, pendingPhotoUri)
                profile.hasPhoto -> decodeFile(ProfilePhotoStore.fileFor(context))
                else -> null
            }
        }
    }

    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image.asImageBitmap(),
            contentDescription = "Foto profil ${profile.displayName}",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(profile.avatarColor)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = profile.initials,
                color = CarifeOnPrimary,
                style = if (size >= 56.dp) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.labelMedium
                },
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun decodeUri(context: Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val largest = maxOf(info.size.width, info.size.height)
            if (largest > AVATAR_MAX_PX) {
                val scale = AVATAR_MAX_PX.toFloat() / largest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }
        }
    } else {
        decodeUriLegacy(context, uri)
    }
} catch (_: Exception) {
    null
}

private fun decodeUriLegacy(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > AVATAR_MAX_PX * 2) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
}

private fun decodeFile(file: File): Bitmap? {
    if (!file.exists()) return null
    return try {
        BitmapFactory.decodeFile(file.path)
    } catch (_: Exception) {
        null
    }
}

private const val AVATAR_MAX_PX = 512
