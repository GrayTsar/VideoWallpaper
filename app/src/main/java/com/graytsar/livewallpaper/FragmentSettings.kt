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
import com.graytsar.livewallpaper.util.GifScaleType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class FragmentSettings : Fragment() {
    val viewModel: ViewModelSettings by viewModels()

    lateinit var switchDarkMode: SwitchMaterial
    lateinit var layoutGifScale: TextInputLayout
    lateinit var autoCompleteGifScale: MaterialAutoCompleteTextView
    lateinit var switchVideoAudio: SwitchMaterial
    lateinit var switchVideoCrop: SwitchMaterial
    lateinit var switchDoubleTapToPause: SwitchMaterial
    lateinit var switchPlayOffscreen: SwitchMaterial

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentSettingsBinding.inflate(inflater, container, false).apply {
            this@FragmentSettings.switchDarkMode = this.switchDarkMode
            this@FragmentSettings.layoutGifScale = this.inputLayoutGifScaleType
            this@FragmentSettings.autoCompleteGifScale = this.autoCompleteGifScaleType
            this@FragmentSettings.switchVideoAudio = this.switchVideoAudio
            this@FragmentSettings.switchVideoCrop = this.switchCropVideo
            this@FragmentSettings.switchDoubleTapToPause = this.switchDoubleTapToPause
            this@FragmentSettings.switchPlayOffscreen = this.switchPlayOffscreen
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val gifScaleTypes = GifScaleType.getTranslations().map {
            requireContext().getString(it)
        }

        autoCompleteGifScale.setSimpleItems(gifScaleTypes.toTypedArray())

        runBlocking {
            viewLifecycleOwner.lifecycleScope.launch {
                val settings = withContext(Dispatchers.IO) {
                    SettingsUiState(
                        darkMode = viewModel.userPreferencesRepository.getForceDarkMode(),
                        gifScaleType = viewModel.userPreferencesRepository.getGifScaleType(),
                        videoAudio = viewModel.userPreferencesRepository.getVideoAudio(),
                        videoCrop = viewModel.userPreferencesRepository.getVideoCrop(),
                        doubleTapToPause = viewModel.userPreferencesRepository.getDoubleTapToPause(),
                        playOffscreen = viewModel.userPreferencesRepository.getPlayOffscreen()
                    )
                }

                switchDarkMode.isChecked = settings.darkMode
                switchVideoAudio.isChecked = settings.videoAudio
                switchVideoCrop.isChecked = settings.videoCrop
                switchDoubleTapToPause.isChecked = settings.doubleTapToPause
                switchPlayOffscreen.isChecked = settings.playOffscreen
                autoCompleteGifScale.setText(requireContext().getString(settings.gifScaleType.toTranslation()), false)
            }
        }

        switchDarkMode.apply {
            setOnCheckedChangeListener { _, isChecked ->
                viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        viewModel.userPreferencesRepository.setForceDarkMode(isChecked)
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

        autoCompleteGifScale.apply {
            setOnItemClickListener { _, _, position, _ ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.userPreferencesRepository.setGifScaleType(GifScaleType.from(position))
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

        switchVideoCrop.apply {
            setOnCheckedChangeListener { _, isChecked ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    viewModel.userPreferencesRepository.setVideoCrop(isChecked)
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
        val gifScaleType: GifScaleType = GifScaleType.FIT_TO_SCREEN,
        val videoAudio: Boolean = false,
        val videoCrop: Boolean = false,
        val doubleTapToPause: Boolean = false,
        val playOffscreen: Boolean = false
    )
}