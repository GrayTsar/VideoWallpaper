package com.graytsar.livewallpaper.util

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.graytsar.livewallpaper.R
import com.graytsar.livewallpaper.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream
import kotlin.io.path.pathString
import kotlin.io.path.walk

enum class WallpaperFlag {
    SYSTEM,
    LOCK;
}

enum class WallpaperType {
    NONE,
    IMAGE,
    VIDEO;
}

enum class ImageScaling(val value: Int) {
    FIT_TO_SCREEN(0),
    CENTER(1),
    ORIGINAL(2);

    @StringRes
    fun toTranslation(): Int {
        return when (this) {
            FIT_TO_SCREEN -> R.string.fit_to_screen
            CENTER -> R.string.center
            ORIGINAL -> R.string.original
        }
    }

    companion object {
        fun getTranslations() = listOf(
            FIT_TO_SCREEN.toTranslation(),
            CENTER.toTranslation(),
            ORIGINAL.toTranslation()
        )

        fun from(index: Int) =
            entries.getOrNull(index) ?: throw IllegalArgumentException("Invalid image scale type index $index")
    }
}

enum class VideoScaling(val value: Int) {
    FIT_CROP(0),
    FIT_TO_SCREEN(1),
    ORIGINAL(2);

    @StringRes
    fun toTranslation(): Int {
        return when (this) {
            FIT_CROP -> R.string.fit_crop
            FIT_TO_SCREEN -> R.string.fit_to_screen
            ORIGINAL -> R.string.original
        }
    }

    companion object {
        fun getTranslations() = listOf(
            FIT_CROP.toTranslation(),
            FIT_TO_SCREEN.toTranslation(),
            ORIGINAL.toTranslation()
        )

        fun from(index: Int) =
            entries.getOrNull(index) ?: throw IllegalArgumentException("Invalid video scale type index $index")
    }
}

object Util {
    fun applyWindowInsetsForTopAppBar(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }

            WindowInsetsCompat.CONSUMED
        }
    }

    fun getImageImportDirectory(context: Context): Path {
        val importPathString = "${context.filesDir.path}/import/image"
        val path = Path(importPathString)
        if (!path.isDirectory()) {
            path.createDirectories()
        }
        return path
    }

    fun getVideoImportDirectory(context: Context): Path {
        val importPathString = "${context.filesDir.path}/import/video"
        val path = Path(importPathString)
        if (!path.isDirectory()) {
            path.createDirectories()
        }
        return path
    }

    fun importImage(inputStream: InputStream, context: Context): Path {
        // Create a unique file for preview
        val time = System.currentTimeMillis()
        return Path("${getImageImportDirectory(context).pathString}/image_$time").also { it ->
            it.deleteIfExists()
        }.apply {
            outputStream().use { outputStream ->
                inputStream.use {
                    it.copyTo(outputStream)
                }
            }
        }
    }

    fun importVideo(inputStream: InputStream, context: Context): Path {
        // Create a unique file for preview
        val time = System.currentTimeMillis()
        return Path("${getVideoImportDirectory(context).pathString}/video_$time").also { it ->
            it.deleteIfExists()
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
        path: Path
    ) = withContext(Dispatchers.IO) {
        val activePath = userPreferencesRepository.getWallpaperPath()
        val newPreviewPath = path.pathString

        // Clean up old files
        listOf(
            getImageImportDirectory(context),
            getVideoImportDirectory(context)
        ).forEach { dir ->
            dir.walk().filter { !it.isDirectory() }.forEach { file ->
                val filePath = file.pathString
                if (filePath != activePath && filePath != newPreviewPath) {
                    file.deleteIfExists()
                }
            }
        }
    }
}