package com.graytsar.livewallpaper.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.graytsar.livewallpaper.core.common.model.WallpaperFlag
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.core.repository.UserPreferencesRepository
import com.graytsar.livewallpaper.domain.CleanupMediaUseCase
import com.graytsar.livewallpaper.domain.ImportMediaUseCase
import com.graytsar.livewallpaper.domain.ValidateMediaUseCase
import com.graytsar.livewallpaper.util.toServiceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ViewModelPicker @Inject constructor(
    private val validateMediaUseCase: ValidateMediaUseCase,
    private val importMediaUseCase: ImportMediaUseCase,
    private val cleanupMediaUseCase: CleanupMediaUseCase,
    val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<PickerEvent>()
    val events = _events.asSharedFlow()

    fun onMediaSelected(uri: Uri, type: WallpaperType) {
        viewModelScope.launch {
            val isValid = when (type) {
                WallpaperType.IMAGE -> validateMediaUseCase.validateImage(uri)
                WallpaperType.VIDEO -> validateMediaUseCase.validateVideo(uri)
                else -> false
            }

            if (isValid) {
                val file = runCatching { importMediaUseCase(uri, type) }.getOrElse { null }
                if (file != null) {
                    cleanupMediaUseCase(file.path)
                    saveSelection(file.path, type)
                    _events.emit(PickerEvent.LaunchWallpaperService(type.toServiceType()))
                } else {
                    _events.emit(PickerEvent.Error(error = PickerUiError.Import))
                }
            } else {
                when (type) {
                    WallpaperType.NONE -> _events.emit(PickerEvent.Error(error = PickerUiError.Import))
                    WallpaperType.IMAGE -> _events.emit(PickerEvent.Error(error = PickerUiError.InvalidImage))
                    WallpaperType.VIDEO -> _events.emit(PickerEvent.Error(error = PickerUiError.InvalidVideo))
                }
            }
        }
    }

    private suspend fun saveSelection(path: String, type: WallpaperType) {
        userPreferencesRepository.setPreviewWallpaperType(type)
        userPreferencesRepository.setPreviewWallpaperService(type.toServiceType())
        userPreferencesRepository.setPreviewPath(path)
    }

    fun onWallpaperSetResult(success: Boolean, flag: WallpaperFlag) {
        viewModelScope.launch() {
            if (success) {
                userPreferencesRepository.promotePreviewSelectionToWallpaper(flag)
            } else {
                val previewPath = userPreferencesRepository.getPreviewPath()
                if (previewPath != null) {
                    File(previewPath).delete()
                }
                userPreferencesRepository.clearPreviewData()
            }
        }
    }

    sealed class PickerEvent {
        data class LaunchWallpaperService(val serviceType: WallpaperServiceType) : PickerEvent()
        data class Error(val error: PickerUiError) : PickerEvent()
    }

    enum class PickerUiError {
        InvalidImage,
        InvalidVideo,
        Import;
    }
}
