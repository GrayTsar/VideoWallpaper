package com.graytsar.livewallpaper.ui

import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.graytsar.livewallpaper.R
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.common.model.WallpaperType
import com.graytsar.livewallpaper.databinding.FragmentPickerBinding
import com.graytsar.livewallpaper.service.ImageWallpaperService
import com.graytsar.livewallpaper.service.VideoWallpaperService
import com.graytsar.livewallpaper.service.currentFlag
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentPicker : Fragment() {
    private val viewModel: ViewModelPicker by viewModels()

    private val wallpaperLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.onWallpaperSetResult(result.resultCode == Activity.RESULT_OK, currentFlag)
        }

    private val videoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.onMediaSelected(it, WallpaperType.VIDEO) }
        }

    private val imageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.onMediaSelected(it, WallpaperType.IMAGE) }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentPickerBinding.inflate(inflater, container, false).apply {
            buttonImage.setOnClickListener(onImageClickListener)
            buttonVideo.setOnClickListener(onVideoClickListener)
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ViewModelPicker.PickerEvent.LaunchWallpaperService -> {
                            launchWallpaperService(event.serviceType)
                        }

                        is ViewModelPicker.PickerEvent.Error -> {
                            val errorMessage = when (event.error) {
                                ViewModelPicker.PickerUiError.InvalidImage -> R.string.error_image_open
                                ViewModelPicker.PickerUiError.InvalidVideo -> R.string.error_video_open
                                ViewModelPicker.PickerUiError.Import -> R.string.error_import
                            }
                            showError(errorMessage)
                        }
                    }
                }
            }
        }
    }

    private fun showError(@StringRes message: Int) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
    }

    private fun launchWallpaperService(serviceType: WallpaperServiceType) {
        val wallpaperService = when (serviceType) {
            WallpaperServiceType.IMAGE -> ImageWallpaperService::class.java
            WallpaperServiceType.VIDEO -> VideoWallpaperService::class.java
        }

        try {
            wallpaperLauncher.launch(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(requireContext(), wallpaperService)
                )
            })
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(requireView(), "could not set wallpaper", Snackbar.LENGTH_SHORT).show()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private val onVideoClickListener = View.OnClickListener {
        try {
            videoLauncher.launch("video/*")
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(requireView(), "No video picker app found on this device.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private val onImageClickListener = View.OnClickListener {
        try {
            imageLauncher.launch("image/*")
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(requireView(), "No image picker app found on this device.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menuSettings -> {
                    findNavController().navigate(R.id.fragmentSettings)
                    true
                }

                else -> false
            }
        }
    }
}