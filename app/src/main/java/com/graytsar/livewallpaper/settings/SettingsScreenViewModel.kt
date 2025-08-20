package com.graytsar.livewallpaper.settings

import androidx.datastore.core.DataStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.graytsar.livewallpaper.datastore.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val userPreferencesRepository: DataStore<UserPreferences>
) : ViewModel() {
}