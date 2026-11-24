package com.carife.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material3.Icon
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
import com.carife.app.ui.theme.CarifeOutlineVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import java.net.URL

private data class PhotoTone(val background: Color, val ink: Color)

private val photoTones = listOf(
    PhotoTone(Color(0xFFF3E1D4), Color(0xFF9E5A38)),
    PhotoTone(Color(0xFFE3EDDF), Color(0xFF3A5A40)),
    PhotoTone(Color(0xFFF3E7D2), Color(0xFF9C5A27)),
    PhotoTone(Color(0xFFEDE5DF), Color(0xFF655C54)),
)

@Composable
fun CafePhotoPlaceholder(
    seed: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    iconSize: Dp = 22.dp,
    showLabel: Boolean = false,
    photoAsset: String? = null,
    photoUrl: String? = null,
) {
    val context = LocalContext.current
    val tone = photoTones[abs(seed.hashCode()) % photoTones.size]
    val shape = RoundedCornerShape(cornerRadius)

    val bitmap by produceState<Bitmap?>(initialValue = null, photoAsset, photoUrl) {
        value = withContext(Dispatchers.IO) {
            photoUrl?.let { CafePhotoCache.loadUrl(it) }
                ?: photoAsset?.let { CafePhotoCache.loadAsset(context, it) }
        }
    }
    val image = bitmap

    Box(
        modifier = modifier
            .clip(shape)
            .background(tone.background)
            .border(1.dp, CarifeOutlineVariant.copy(alpha = 0.65f), shape),
    ) {
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.LocalCafe,
                contentDescription = null,
                tint = tone.ink.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(iconSize),
            )
        }

        if (showLabel && image == null) {
            Text(
                text = "Foto belum tersedia",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = tone.ink.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
            )
        }
    }
}

private object CafePhotoCache {

    private val cache = mutableMapOf<String, Bitmap?>()

    fun loadAsset(context: Context, asset: String): Bitmap? = synchronized(cache) {
        if (cache.containsKey(asset)) {
            return cache[asset]
        }

        val bitmap = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open(asset).use { BitmapFactory.decodeStream(it, null, bounds) }

            var sample = 1
            while (bounds.outWidth / sample > MAX_WIDTH) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.assets.open(asset).use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull()

        cache[asset] = bitmap
        bitmap
    }

    fun loadUrl(url: String): Bitmap? = synchronized(cache) {
        val key = "url:$url"
        if (cache.containsKey(key)) return cache[key]

        val bitmap = runCatching {
            val connection = URL(url).openConnection().apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("User-Agent", "Carife/1.0 (Android)")
            }
            connection.getInputStream().use { stream ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                val bytes = stream.readBytes()
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

                var sample = 1
                while (bounds.outWidth / sample > MAX_WIDTH) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            }
        }.getOrNull()

        cache[key] = bitmap
        bitmap
    }

    private const val MAX_WIDTH = 1080
}
