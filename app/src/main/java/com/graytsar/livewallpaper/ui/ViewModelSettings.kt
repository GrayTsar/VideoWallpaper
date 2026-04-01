package com.graytsar.livewallpaper.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.graytsar.livewallpaper.core.common.model.ImageScaling
import com.graytsar.livewallpaper.core.common.model.VideoScaling
import com.graytsar.livewallpaper.core.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewModelSettings @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val imageScalingTypes = ImageScaling.getTranslations()
    val videoScalingTypes = VideoScaling.getTranslations()

}