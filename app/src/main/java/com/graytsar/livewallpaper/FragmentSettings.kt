package com.graytsar.livewallpaper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.graytsar.livewallpaper.databinding.FragmentSettingsBinding
import com.graytsar.livewallpaper.repository.UserPreferencesRepository
import com.graytsar.livewallpaper.util.GifScaleType
import com.graytsar.livewallpaper.util.valueDefaultDarkMode
import com.graytsar.livewallpaper.util.valueDefaultDoubleTapToPause
import com.graytsar.livewallpaper.util.valueDefaultPlayOffscreen
import com.graytsar.livewallpaper.util.valueDefaultScaleType
import com.graytsar.livewallpaper.util.valueDefaultVideoAudio
import com.graytsar.livewallpaper.util.valueDefaultVideoCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class FragmentSettings : Fragment() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    lateinit var darkModeSwitch: SwitchMaterial
    lateinit var gifScaleLayout: TextInputLayout
    lateinit var gifScaleAutoComplete: MaterialAutoCompleteTextView
    lateinit var videoAudioSwitch: SwitchMaterial
    lateinit var videoCropSwitch: SwitchMaterial
    lateinit var doubleTapToPauseSwitch: SwitchMaterial
    lateinit var playOffscreenSwitch: SwitchMaterial

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentSettingsBinding.inflate(inflater, container, false).apply {
            darkModeSwitch = this.switchDarkMode
            gifScaleLayout = this.inputLayoutGifScaleType
            gifScaleAutoComplete = this.autoCompleteGifScaleType
            videoAudioSwitch = this.switchVideoAudio
            videoCropSwitch = this.switchCropVideo
            doubleTapToPauseSwitch = this.switchDoubleTapToPause
            playOffscreenSwitch = this.switchPlayOffscreen
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val keyThemeDarkMode = getString(R.string.keyThemeDarkMode)
        val keyGifScaleType = getString(R.string.keyGifScaleType)
        val keyVideoAudio = getString(R.string.keyVideoAudio)
        val keyCropVideo = getString(R.string.keyCropVideo)
        val keyDoubleTapToPause = getString(R.string.keyDoubleTapToPause)
        val keyPlayOffscreen = getString(R.string.keyPlayOffscreen)

        darkModeSwitch.apply {
            isChecked = sharedPreferences.getBoolean(
                keyThemeDarkMode,
                valueDefaultDarkMode
            )
            setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit { putBoolean(keyThemeDarkMode, isChecked) }
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        userPreferencesRepository.setForceDarkMode(isChecked)
                    }

                    AppCompatDelegate.setDefaultNightMode(
                        if (isChecked) {
                            AppCompatDelegate.MODE_NIGHT_YES
                        } else {
                            AppCompatDelegate.MODE_NIGHT_NO
                        }
                    )

                    (activity as? ReibuActivity)?.recreate()
                }
            }
        }

        gifScaleAutoComplete.apply {
            val gifScaleTypes = GifScaleType.getTranslations()

            val selectedScaleTypeValue = sharedPreferences.getString(
                keyGifScaleType,
                valueDefaultScaleType
            ) ?: valueDefaultScaleType
            val selectedScaleTypeIndex = gifScaleTypes.indexOf(selectedScaleTypeValue)
                .takeIf { it >= 0 }
                ?: 0

            setSimpleItems(gifScaleTypes.toTypedArray())
            setText(gifScaleTypes[selectedScaleTypeIndex], false)
            setOnItemClickListener { _, _, position, _ ->
                val selectedValue = gifScaleTypes.getOrElse(position) { valueDefaultScaleType }
                sharedPreferences.edit { putString(keyGifScaleType, selectedValue) }
            }
        }

        videoAudioSwitch.apply {
            isChecked = sharedPreferences.getBoolean(
                keyVideoAudio,
                valueDefaultVideoAudio
            )
            setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit { putBoolean(keyVideoAudio, isChecked) }
            }
        }

        videoCropSwitch.apply {
            isChecked = sharedPreferences.getBoolean(
                keyCropVideo,
                valueDefaultVideoCrop
            )
            setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit { putBoolean(keyCropVideo, isChecked) }
            }
        }

        doubleTapToPauseSwitch.apply {
            isChecked = sharedPreferences.getBoolean(
                keyDoubleTapToPause,
                valueDefaultDoubleTapToPause
            )
            setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit { putBoolean(keyDoubleTapToPause, isChecked) }
            }
        }

        playOffscreenSwitch.apply {
            isChecked = sharedPreferences.getBoolean(
                keyPlayOffscreen,
                valueDefaultPlayOffscreen
            )
            setOnCheckedChangeListener { _, isChecked ->
                sharedPreferences.edit { putBoolean(keyPlayOffscreen, isChecked) }
            }
        }
    }
}