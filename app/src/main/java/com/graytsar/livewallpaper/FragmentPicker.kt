package com.graytsar.livewallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
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
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.graytsar.livewallpaper.databinding.FragmentPickerBinding
import com.graytsar.livewallpaper.util.Util
import com.graytsar.livewallpaper.util.WallpaperType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

@AndroidEntryPoint
class FragmentPicker : Fragment() {
    val viewModel: ViewModelPicker by viewModels()

    private val wallpaperLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                runBlocking(Dispatchers.IO) {
                    viewModel.userPreferencesRepository.promotePreviewSelectionToWallpaper()
                }
            } else {
                runBlocking(Dispatchers.IO) {
                    val previewPath = viewModel.userPreferencesRepository.getPreviewPath()
                    if (previewPath != null) {
                        File(previewPath).delete()
                    }
                    viewModel.userPreferencesRepository.clearPreviewData()
                }
            }
        }

    /**
     * Video picker launcher.
     */
    private val videoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
            val videoUri: Uri = result ?: return@registerForActivityResult
            val result = runCatching { viewModel.validateVideo(videoUri, requireContext()) }.getOrElse { false }
            if (result) {
                val path = saveVideo(videoUri)
                saveSelection(path!!, WallpaperType.VIDEO)
                launchVideoWallpaperService()
            } else {
                showError(R.string.error_image_open)
            }
        }

    /**
     * Image picker launcher.
     */
    private val imageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { result ->
            val imageUri: Uri = result ?: return@registerForActivityResult
            val result = runCatching { viewModel.validateImage(imageUri) }.getOrElse { false }
            if (result) {
                val path = saveImage(imageUri)
                saveSelection(path!!, WallpaperType.IMAGE)
                launchVideoWallpaperService()
            } else {
                showError(R.string.error_image_open)
            }
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
    }

    private fun saveSelection(path: Path, type: WallpaperType) {
        runBlocking(Dispatchers.IO) {
            Util.cleanup(requireContext(), viewModel.userPreferencesRepository, path)
            viewModel.userPreferencesRepository.setPreviewWallpaperType(type)
            viewModel.userPreferencesRepository.setPreviewPath(path.pathString)
        }
    }

    /**
     * Show an error message.
     */
    private fun showError(@StringRes message: Int) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }

    private fun saveImage(uri: Uri): Path? {
        try {
            val stream = viewModel.openInputStreamForContentResolver(uri)
            return Util.importImage(stream, requireContext())
        } catch (e: Exception) {
            showError(R.string.error_image_open)
            return null
        }
    }

    private fun saveVideo(uri: Uri): Path? {
        try {
            val inputStream = viewModel.openInputStreamForContentResolver(uri)
            return Util.importVideo(inputStream, requireContext())
        } catch (e: Exception) {
            showError(R.string.error_image_open)
            return null
        }
    }

    private fun launchVideoWallpaperService() {
        try {
            viewModel.wallpaperManager.clear()
            wallpaperLauncher.launch(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(requireContext(), VideoWallpaperService::class.java)
                )
            })
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(requireView(), "could not set wallpaper", Snackbar.LENGTH_SHORT).show()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    /**
     * Launch the video picker.
     */
    private val onVideoClickListener = View.OnClickListener {
        //val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        //    addCategory(Intent.CATEGORY_OPENABLE)
        //    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        //    type = "video/*"
        //}
        //videoLauncher.launch(Intent.createChooser(intent, "Video"))

        //val intent = videoLauncher.contract.createIntent(requireContext())
        videoLauncher.launch("video/*")
    }

    /**
     * Launch the image picker.
     */
    private val onImageClickListener = View.OnClickListener {
        //val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
        //    addCategory(Intent.CATEGORY_OPENABLE)
        //    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        //    type = "image/*"
        //}
        //imageLauncher.launch(Intent.createChooser(intent, "Image"))
        imageLauncher.launch("image/*")
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menuSettings -> {
                    findNavController().navigate(R.id.fragmentSettings)
                }
            }
            return true
        }
    }
}