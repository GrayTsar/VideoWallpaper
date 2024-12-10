package com.graytsar.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.graytsar.livewallpaper.compose.AppTheme

class FragmentPicker : Fragment() {

    /**
     * Video picker launcher.
     */
    private val videoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val videoUri: Uri = result.data?.data ?: return@registerForActivityResult
            if (!checkVideo(videoUri)) {
                showError(R.string.error_video_open)
                return@registerForActivityResult
            }

            WallpaperManager.getInstance(requireContext().applicationContext).clear()
            saveSettings(videoUri, true)

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(requireContext(), VideoWallpaper::class.java)
                )
            }
            startActivity(intent)
        }

    /**
     * Image picker launcher.
     */
    private val imageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val imageUri: Uri = result.data?.data ?: return@registerForActivityResult
            if (!checkImage(imageUri)) {
                showError(R.string.error_image_open)
                return@registerForActivityResult
            }

            val wallpaperManager = WallpaperManager.getInstance(requireContext().applicationContext)
            wallpaperManager.clear()

            //problem with selecting multiple mime types. Limits how the SAF "Files" App shows stuff
            //simply add support for non animated images like this
            val extension = requireContext().contentResolver.getType(imageUri)
            if (extension != "image/gif" && extension != "image/webp") {
                wallpaperManager.setStream(requireContext().contentResolver.openInputStream(imageUri))
                return@registerForActivityResult

            }

            saveSettings(imageUri, false)

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(requireContext(), VideoWallpaper::class.java)
                )
            }

            startActivity(intent)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WallpaperPicketScreen()
            }
        }
    }

    @Preview(
        name = "Light Mode",
        showBackground = true
    )
    @Preview(
        uiMode = Configuration.UI_MODE_NIGHT_YES,
        name = "Dark Mode",
        showBackground = true
    )
    @Composable
    fun WallpaperPicketScreen() {
        AppTheme {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SelectionOutlinedButton(
                        onClick = onVideoClickListener,
                        text = R.string.video,
                    )
                    SelectionOutlinedButton(
                        onClick = onImageClickListener,
                        text = R.string.image
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Check if the image is valid.
     *
     * @param fUri the uri of the image.
     *
     * @return true if the image is valid, false otherwise.
     */
    private fun checkImage(fUri: Uri) = runCatching {
        val cr: ContentResolver = requireContext().contentResolver
        if (Build.VERSION.SDK_INT >= 28) {
            var source: ImageDecoder.Source? = ImageDecoder.createSource(cr, fUri)
            var animatedImageDrawable: Drawable? = ImageDecoder.decodeDrawable(source!!)

            animatedImageDrawable = null
            source = null
        } else {
            var inputStream = cr.openInputStream(fUri)
            inputStream?.read()
            var movie = Movie.decodeStream(inputStream)
            movie = null
            inputStream?.close()
            inputStream = null
        }
    }.isSuccess

    /**
     * Check if the video is valid.
     *
     * @param fUri the uri of the video.
     *
     * @return true if the video is valid, false otherwise.
     */
    private fun checkVideo(fUri: Uri) = runCatching {
        val mediaPlayer: MediaPlayer? = MediaPlayer.create(context, fUri).apply {
            isLooping = true
            setVolume(0f, 0f)
        }
        mediaPlayer?.release()
    }.isSuccess

    /**
     * Save the file uri for a video or image in shared preferences.
     *
     * @param fUri the uri of the video or image.
     * @param isVideo whether the file is a video or not.
     */
    private fun saveSettings(fUri: Uri, isVideo: Boolean) {
        val sharedPref =
            requireContext().getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(keyVideo, fUri.toString())
        editor.putBoolean(keyType, isVideo)
        editor.apply()
    }

    /**
     * Show an error message.
     */
    private fun showError(@StringRes message: Int) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }

    /**
     * Launch the video picker.
     */
    private val onVideoClickListener = {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "video/*"
        }
        videoLauncher.launch(Intent.createChooser(intent, "Video"))
    }

    /**
     * Launch the image picker.
     */
    private val onImageClickListener = {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "image/*"
        }
        imageLauncher.launch(Intent.createChooser(intent, "Image"))
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menuSettings -> {
                    findNavController().navigate(R.id.fragmentContainerSettings)
                }
            }
            return true
        }
    }
}

@Composable
private fun SelectionOutlinedButton(
    onClick: () -> Unit,
    @StringRes
    text: Int
) {
    AppTheme { }.apply {
        lightColorScheme()
    }

    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        border = BorderStroke(
            width = 3.dp,
            color = MaterialTheme.colorScheme.primary
            //colorResource(id = R.color.accent)
        )
    ) {
        Text(
            text = stringResource(text),
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary
            //colorResource(id = R.color.accent)
        )
    }
}