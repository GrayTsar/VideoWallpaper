package com.graytsar.livewallpaper.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.util.Util.getImageImportDirectory
import com.graytsar.livewallpaper.util.Util.getVideoImportDirectory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class ImportMediaUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver
) {
    operator suspend fun invoke(uri: Uri, type: WallpaperType): File? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        return when (type) {
            WallpaperType.IMAGE -> importImage(inputStream, context)
            WallpaperType.VIDEO -> importVideo(inputStream, context)
            else -> null
        }
    }

    private suspend fun importFile(inputStream: InputStream, directory: File, prefix: String): File {
        val time = System.currentTimeMillis()
        val file = File(directory, "${prefix}_$time")
        file.outputStream().use { output ->
            inputStream.use { input ->
                input.copyTo(output)
            }
        }
        return file
    }

    private suspend fun importImage(inputStream: InputStream, context: Context): File {
        return importFile(inputStream, getImageImportDirectory(context), "image")
    }

    private suspend fun importVideo(inputStream: InputStream, context: Context): File {
        return importFile(inputStream, getVideoImportDirectory(context), "video")
    }
}
