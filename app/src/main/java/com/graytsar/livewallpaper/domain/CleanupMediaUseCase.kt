package com.graytsar.livewallpaper.domain

import android.content.Context
import com.graytsar.livewallpaper.core.repository.UserPreferencesRepository
import com.graytsar.livewallpaper.util.Util
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CleanupMediaUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(newPreviewPath: String) = withContext(Dispatchers.IO) {
        val activePath = userPreferencesRepository.getWallpaperPath()

        listOf(
            Util.getImageImportDirectory(context),
            Util.getVideoImportDirectory(context)
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
