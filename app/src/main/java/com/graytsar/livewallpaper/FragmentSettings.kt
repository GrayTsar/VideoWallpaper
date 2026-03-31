package com.graytsar.livewallpaper

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.graytsar.livewallpaper.databinding.FragmentSettingsBinding
import com.graytsar.livewallpaper.util.ImageScaling
import com.graytsar.livewallpaper.util.VideoScaling
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class FragmentSettings : Fragment() {
    val viewModel: ViewModelSettings by viewModels<ViewModelSettings>()

    lateinit var switchDarkMode: SwitchMaterial
    lateinit var layoutImageScaling: TextInputLayout
    lateinit var autoCompleteImageScaling: MaterialAutoCompleteTextView
    lateinit var switchVideoAudio: SwitchMaterial
    lateinit var layoutVideoScaling: TextInputLayout
    lateinit var autoCompleteVideoScaling: MaterialAutoCompleteTextView
    lateinit var switchDoubleTapToPause: SwitchMaterial
    lateinit var switchPlayOffscreen: SwitchMaterial

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentSettingsBinding.inflate(inflater, container, false).apply {
            this@FragmentSettings.switchDarkMode = this.switchDarkMode
            this@FragmentSettings.layoutImageScaling = this.inputLayoutImageScaling
            this@FragmentSettings.autoCompleteImageScaling = this.autoCompleteImageScaling
            this@FragmentSettings.switchVideoAudio = this.switchVideoAudio
            this@FragmentSettings.layoutVideoScaling = this.inputLayoutVideoScaling
            this@FragmentSettings.autoCompleteVideoScaling = this.autoCompleteVideoScaling
            this@FragmentSettings.switchDoubleTapToPause = this.switchDoubleTapToPause
            this@FragmentSettings.switchPlayOffscreen = this.switchPlayOffscreen
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        autoCompleteImageScaling.setSimpleItems(viewModel.imageScalingTypes.map { requireContext().getString(it) }
            .toTypedArray())
        autoCompleteVideoScaling.setSimpleItems(viewModel.videoScalingTypes.map { requireContext().getString(it) }
            .toTypedArray())

        runBlocking {
            viewLifecycleOwner.lifecycleScope.launch {
                val settings = withContext(Dispatchers.IO) {
                    SettingsUiState(
                        darkMode = viewModel.userPreferencesRepository.getForceDarkMode(),
                        imageScaling = viewModel.userPreferencesRepository.getImageScaleType(),
                        videoAudio = viewModel.userPreferencesRepository.getVideoAudio(),
                        videoScaling = viewModel.userPreferencesRepository.getVideoScaling(),
                        doubleTapToPause = viewModel.userPreferencesRepository.getDoubleTapToPause(),
                        playOffscreen = viewModel.userPreferencesRepository.getPlayOffscreen()
                    )
                }

                switchDarkMode.isChecked = settings.darkMode
                autoCompleteImageScaling.setText(
                    requireContext().getString(settings.imageScaling.toTranslation()),
                    false
                )
                switchVideoAudio.isChecked = settings.videoAudio
                switchDoubleTapToPause.isChecked = settings.doubleTapToPause
                switchPlayOffscreen.isChecked = settings.playOffscreen
                autoCompleteVideoScaling.setText(
                    requireContext().getString(settings.videoScaling.toTranslation()),
                    false
                )
            }
        }

        switchDarkMode.apply {
            setOnCheckedChangeListener { _, isChecked ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    withContext(Dispatchers.IO) {
                        viewModel.userPreferencesRepository.setForceDarkMode(isChecked)
                    }
                    withContext(Dispatchers.Main) {
                        AppCompatDelegate.setDefaultNightMode(
                            if (isChecked) {
                                AppCompatDelegate.MODE_NIGHT_YES
                            } else {
                                AppCompatDelegate.MODE_NIGHT_NO
                            }
                        )
                    }
                }
            }
        }

        autoCompleteImageScaling.apply {
            setOnItemClickListener { _, _, position, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.userPreferencesRepository.setImageScaleType(ImageScaling.from(position))
                }
            }
        }

        switchVideoAudio.apply {
            setOnCheckedChangeListener { _, isChecked ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.userPreferencesRepository.setVideoAudio(isChecked)
                }
            }
        }


        autoCompleteVideoScaling.apply {
            setOnItemClickListener { _, _, position, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.userPreferencesRepository.setVideoScaling(VideoScaling.from(position))
                }
            }
        }

        switchDoubleTapToPause.apply {
            setOnCheckedChangeListener { _, isChecked ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.userPreferencesRepository.setDoubleTapToPause(isChecked)
                }
            }
        }

        switchPlayOffscreen.apply {
            setOnCheckedChangeListener { _, isChecked ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.userPreferencesRepository.setPlayOffscreen(isChecked)
                }
            }
        }
    }

    private data class SettingsUiState(
        val darkMode: Boolean = false,
        val imageScaling: ImageScaling = ImageScaling.FIT_TO_SCREEN,
        val videoAudio: Boolean = false,
        val videoScaling: VideoScaling = VideoScaling.FIT_CROP,
        val doubleTapToPause: Boolean = false,
        val playOffscreen: Boolean = false
    )
}