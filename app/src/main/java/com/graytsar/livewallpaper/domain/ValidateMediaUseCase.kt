package com.graytsar.livewallpaper.domain

import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ValidateMediaUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver
) {
    suspend fun validateVideo(uri: Uri): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            // Check if the file actually contains a video track
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            hasVideo != null
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            false
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun validateImage(uri: Uri): Boolean {
        return if (Build.VERSION.SDK_INT >= 28) {
            validateImageNew(uri)
        } else {
            validateImageLegacy(uri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun validateImageNew(uri: Uri): Boolean {
        return try {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeDrawable(source)
            true
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun validateImageLegacy(uri: Uri): Boolean {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                // Check for GIF first
                if (Movie.decodeStream(inputStream) != null) return true
            }

            // Only proceed to BitmapFactory if the first check failed
            contentResolver.openInputStream(uri)?.use { s ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(s, null, options)
                return !(options.outWidth == -1 || options.outHeight == -1)
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
}
