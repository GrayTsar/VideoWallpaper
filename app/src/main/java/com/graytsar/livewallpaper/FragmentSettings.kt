package com.graytsar.livewallpaper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat

class FragmentSettings: PreferenceFragmentCompat() {
    private lateinit var preferenceDarkMode:CheckBoxPreference


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        preferenceDarkMode =
            preferenceManager.findPreference<CheckBoxPreference>(getString(R.string.keyThemeDarkMode))!!

        preferenceDarkMode.setOnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean) {
                if (newValue) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }


                context?.let {context ->
                    if(context is MainActivity){
                        context.recreate()
                    }
                }

                /*
                if(newValue == true && SingletonStatic.isNightMode == false) {

                } else if (newValue == false && SingletonStatic.isNightMode == true) {

                }

                 */
            }

            true
        }


        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}