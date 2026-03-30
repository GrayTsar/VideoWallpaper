package com.graytsar.livewallpaper

import android.app.WallpaperManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import com.graytsar.livewallpaper.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class ViewModelPicker @Inject constructor(
    val contentResolver: ContentResolver,
    val userPreferencesRepository: UserPreferencesRepository,
    val wallpaperManager: WallpaperManager
) : ViewModel() {

    /**
     *
     * @throws IllegalStateException if [MediaPlayer] creation failed
     */
    fun validateVideo(uri: Uri, context: Context): Boolean {
        //MediaMetadataRetriever
        val mediaPlayer: MediaPlayer? = MediaPlayer.create(context, uri).apply {
            setVolume(0f, 0f)
        }
        checkNotNull(mediaPlayer) { "Unable to create MediaPlayer for the provided URI" }
        mediaPlayer.release()
        return true
    }

    fun validateImage(uri: Uri): Boolean {
        return if (Build.VERSION.SDK_INT >= 28) {
            validateImageNew(uri)
        } else {
            validateImageLegacy(uri)
        }
    }

    /**
     *
     * @throws java.io.IOException if file is not found, is an unsupported format, or cannot be decoded for any reason.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun validateImageNew(uri: Uri): Boolean {
        val source = ImageDecoder.createSource(contentResolver, uri)
        ImageDecoder.decodeDrawable(source)
        return true
    }

    /**
     *
     * @throws IllegalArgumentException if the provided URI does not point to a valid image format.
     */
    @Suppress("DEPRECATION")
    private fun validateImageLegacy(uri: Uri): Boolean {
        val movie = openInputStreamForContentResolver(uri).use { Movie.decodeStream(it) }
        if (movie != null) {
            return true
        }//else "Not a GIF"

        // Only proceed to BitmapFactory if the first check failed
        val options = openInputStreamForContentResolver(uri).use { s ->
            BitmapFactory.Options().apply { inJustDecodeBounds = true }.also {
                BitmapFactory.decodeStream(s, null, it)
            }
        }

        if (options.outWidth == -1 || options.outHeight == -1) {
            //"Invalid image format or failed to decode"
            return false
        }
        return true
    }

    /**
     *
     * @throws java.io.FileNotFoundException if the provided URI could not be opened.
     * @throws java.io.IOException if an I/O error occurs.
     * @throws IllegalStateException if the content provider is not responding or has recently crashed.
     */
    fun openInputStreamForContentResolver(
        uri: Uri
    ): InputStream {
        val stream = contentResolver.openInputStream(uri)
        return checkNotNull(stream) { "provider recently crashed" }
    }
}
