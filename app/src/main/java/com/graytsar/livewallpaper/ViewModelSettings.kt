package com.graytsar.livewallpaper

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.graytsar.livewallpaper.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ViewModelSettings @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
}