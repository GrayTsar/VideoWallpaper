package com.graytsar.livewallpaper.util

import android.content.Context
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.core.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

fun WallpaperType.toServiceType() = when (this) {
    WallpaperType.IMAGE -> WallpaperServiceType.IMAGE
    WallpaperType.NONE, WallpaperType.VIDEO -> WallpaperServiceType.VIDEO
}

object Util {

    fun getImageImportDirectory(context: Context): File {
        return File("${context.filesDir.path}/import/image").apply {
            if (!isDirectory) mkdirs()
        }
    }

    fun getVideoImportDirectory(context: Context): File {
        return File("${context.filesDir.path}/import/video").apply {
            if (!isDirectory) mkdirs()
        }
    }

    private fun importFile(inputStream: InputStream, directory: File, prefix: String): File {
        val time = System.currentTimeMillis()
        val file = File(directory, "${prefix}_$time")
        file.outputStream().use { output ->
            inputStream.use { input ->
                input.copyTo(output)
            }
        }
        return file
    }

    fun importImage(inputStream: InputStream, context: Context): File {
        return importFile(inputStream, getImageImportDirectory(context), "image")
    }

    fun importVideo(inputStream: InputStream, context: Context): File {
        return importFile(inputStream, getVideoImportDirectory(context), "video")
    }

    suspend fun cleanup(
        context: Context,
        userPreferencesRepository: UserPreferencesRepository,
        path: File
    ) = withContext(Dispatchers.IO) {
        val activePath = userPreferencesRepository.getWallpaperPath()
        val newPreviewPath = path.path

        // Clean up old files
        listOf(
            getImageImportDirectory(context),
            getVideoImportDirectory(context)
        ).forEach { dir ->
            dir.walk().filter { !it.isDirectory }.forEach { file ->
                val filePath = file.path
                if (filePath != activePath && filePath != newPreviewPath) {
                    file.delete()
                }
            }
        }
    }
}