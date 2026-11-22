package com.carife.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ProfilePhotoStore(private val context: Context) {

    fun file(): File = fileFor(context)

    suspend fun save(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val bitmap = decode(uri) ?: return@withContext false
        val temp = File(context.cacheDir, "profile_avatar_upload.jpg")
        try {
            val written = temp.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            }
            if (!written) return@withContext false
            if (!temp.renameTo(file())) {
                temp.copyTo(file(), overwrite = true)
                temp.delete()
            }
            true
        } catch (_: IOException) {
            temp.delete()
            false
        } finally {
            bitmap.recycle()
        }
    }

    suspend fun delete() = withContext(Dispatchers.IO) {
        file().delete()
        Unit
    }

    private fun decode(uri: Uri): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val largest = maxOf(info.size.width, info.size.height)
                if (largest > MAX_AVATAR_PX) {
                    val scale = MAX_AVATAR_PX.toFloat() / largest
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        } else {
            decodeLegacy(uri)
        }
    } catch (_: Exception) {
        null
    }

    private fun decodeLegacy(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_AVATAR_PX * 2) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    companion object {
        const val FILE_NAME = "profile_avatar.jpg"
        private const val MAX_AVATAR_PX = 512

        fun fileFor(context: Context): File = File(context.filesDir, FILE_NAME)
    }
}
