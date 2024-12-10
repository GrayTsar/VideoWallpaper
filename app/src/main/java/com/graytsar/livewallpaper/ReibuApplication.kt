package com.graytsar.livewallpaper

import android.app.Application
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.graytsar.livewallpaper.datastore.UserPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class ReibuApplication : Application() {
    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate() {
        super.onCreate()

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> {
            }

            Configuration.UI_MODE_NIGHT_NO -> {
                GlobalScope.launch(Dispatchers.IO) {
                    val isDarkMode = userPreferences.readForceDarkMode().first()
                    if (isDarkMode) {
                        withContext(Dispatchers.Main) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        }
                    }
                }
            }

            Configuration.UI_MODE_NIGHT_UNDEFINED -> {
            }
        }
    }
}