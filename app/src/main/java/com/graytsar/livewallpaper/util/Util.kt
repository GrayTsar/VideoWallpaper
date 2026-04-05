package com.graytsar.livewallpaper.util

import android.content.Context
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import java.io.File

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
}