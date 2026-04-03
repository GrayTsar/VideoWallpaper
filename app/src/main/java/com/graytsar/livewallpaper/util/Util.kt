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
        val importPathString = "${context.filesDir.path}/import/image"
        val file = File(importPathString)
        if (!file.isDirectory) {
            file.mkdirs()
        }
        return file
    }

    fun getVideoImportDirectory(context: Context): File {
        val importPathString = "${context.filesDir.path}/import/video"
        val file = File(importPathString)
        if (!file.isDirectory) {
            file.mkdirs()
        }
        return file
    }

    fun importImage(inputStream: InputStream, context: Context): File {
        // Create a unique file for preview
        val time = System.currentTimeMillis()
        return File(getImageImportDirectory(context), "image_$time").also { it ->
            if (it.exists()) {
                it.delete()
            }
        }.apply {
            outputStream().use { outputStream ->
                inputStream.use {
                    it.copyTo(outputStream)
                }
            }
        }
    }

    fun importVideo(inputStream: InputStream, context: Context): File {
        // Create a unique file for preview
        val time = System.currentTimeMillis()
        return File(getVideoImportDirectory(context), "video_$time").also { it ->
            if (it.exists()) {
                it.delete()
            }
        }.apply {
            outputStream().use { outputStream ->
                inputStream.use {
                    it.copyTo(outputStream)
                }
            }
        }
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