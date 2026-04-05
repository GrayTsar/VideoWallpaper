package com.graytsar.livewallpaper.ui

import com.graytsar.livewallpaper.core.common.model.WallpaperFlag
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.core.repository.UserPreferencesRepository
import com.graytsar.livewallpaper.util.toServiceType
import java.io.File
import javax.inject.Inject

class PickerSelectionStore @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend fun saveSelection(path: String, type: WallpaperType) {
        userPreferencesRepository.setPreviewWallpaperType(type)
        userPreferencesRepository.setPreviewWallpaperService(type.toServiceType())
        userPreferencesRepository.setPreviewPath(path)
    }

    suspend fun promotePreviewSelectionToWallpaper(flag: WallpaperFlag) {
        userPreferencesRepository.promotePreviewSelectionToWallpaper(flag)
    }

    suspend fun clearPreviewSelection() {
        userPreferencesRepository.getPreviewPath()?.let { File(it).delete() }
        userPreferencesRepository.clearPreviewData()
    }
}