package com.graytsar.livewallpaper

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.graytsar.livewallpaper.engine.Api28ImageRenderer
import com.graytsar.livewallpaper.engine.EngineSettings
import com.graytsar.livewallpaper.engine.LegacyImageRenderer
import com.graytsar.livewallpaper.engine.WallpaperRenderer
import com.graytsar.livewallpaper.repository.UserPreferencesRepository
import com.graytsar.livewallpaper.repository.WallpaperSelection
import com.graytsar.livewallpaper.util.VideoScaling
import com.graytsar.livewallpaper.util.WallpaperType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class VideoWallpaperService : WallpaperService() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreateEngine(): Engine = UnifiedWallpaperEngine()

    private fun createRenderer(
        wallpaperType: WallpaperType,
        holder: SurfaceHolder,
        file: File,
        settings: EngineSettings
    ): WallpaperRenderer {
        return when (wallpaperType) {
            WallpaperType.VIDEO -> VideoRenderer(holder, file, settings)
            WallpaperType.IMAGE, WallpaperType.NONE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Api28ImageRenderer(holder, file, settings)
                } else {
                    LegacyImageRenderer(holder, file, settings)
                }
            }
        }
    }

    private inner class UnifiedWallpaperEngine : Engine() {
        private var renderer: WallpaperRenderer? = null
        private var tapTimeBetween: Long = 0L
        private var isPaused: Boolean = false
        private var isVisibleToUser: Boolean = false

        private var engineSettings: EngineSettings? = null
        private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private var observeJob: Job? = null

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            observeJob = engineScope.launch {
                combine(
                    userPreferencesRepository.getEngineSettingsFlow(),
                    userPreferencesRepository.getWallpaperSelectionFlow(isPreview)
                ) { settings, selection ->
                    settings to selection
                }.collect { (settings, selection) ->
                    engineSettings = settings
                    handleUpdate(settings, selection)
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleToUser = visible
            renderer?.onVisibilityChanged(visible, isPaused)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            val doubleTapToPause = engineSettings?.general?.doubleTapToPause ?: false
            if (doubleTapToPause && event?.actionMasked == MotionEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - tapTimeBetween <= 500L) {
                    isPaused = !isPaused
                    renderer?.onPauseStateChanged(isPaused, isVisibleToUser)
                }
                tapTimeBetween = currentTime
            }

            super.onTouchEvent(event)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            releaseRenderer()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            observeJob?.cancel()
            engineScope.cancel()
            releaseRenderer()
            super.onDestroy()
        }

        private fun handleUpdate(settings: EngineSettings, selection: WallpaperSelection?) {
            val surfaceHolder = surfaceHolder ?: return
            if (selection == null) {
                //data store was cleared
                releaseRenderer()
                return
            }

            val file = File(selection.path)
            if (!file.exists()) {
                //app data has been cleared
                releaseRenderer()
                return
            }

            releaseRenderer()
            renderer = createRenderer(
                wallpaperType = selection.type,
                holder = surfaceHolder,
                file = file,
                settings = settings
            ).also { renderer ->
                renderer.onVisibilityChanged(isVisibleToUser, isPaused)
                renderer.onSurfaceReady()
            }
        }

        /**
         * Releases renderer resources
         */
        private fun releaseRenderer() {
            renderer?.release()
            renderer = null
        }
    }

    private inner class VideoRenderer(
        private val holder: SurfaceHolder,
        private val file: File,
        private val settings: EngineSettings
    ) : WallpaperRenderer {
        private var mediaPlayer: MediaPlayer? = null
        private var isVisible = false
        private var isPaused = false


        override fun onSurfaceReady() {
            try {
                createMediaPlayer()
            } catch (e: Exception) {
                release()
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean, isPaused: Boolean) {
            updatePlaybackState(isVisible, isPaused)
        }

        override fun onPauseStateChanged(isPaused: Boolean, isVisible: Boolean) {
            updatePlaybackState(isVisible, isPaused)
        }

        override fun release() {
            val player = mediaPlayer ?: return

            runCatching {
                //release() internally stops the player, so we don't need to call stop() before it
                player.release()
            }.onFailure { error ->
                FirebaseCrashlytics.getInstance().recordException(error)
            }
            mediaPlayer = null
        }

        private fun createMediaPlayer() {
            if (mediaPlayer != null) {
                mediaPlayer!!.setSurface(holder.surface)
                return
            }

            val videoScalingMode = when (settings.video.videoScaling) {
                VideoScaling.FIT_CROP -> MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                VideoScaling.FIT_TO_SCREEN -> MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                VideoScaling.ORIGINAL -> null
            }
            val volume = if (settings.video.audio) 1.0f else 0.0f

            mediaPlayer = MediaPlayer().apply {
                setOnPreparedListener { mp ->
                    videoScalingMode?.let {
                        mp.setVideoScalingMode(videoScalingMode)
                    }
                    mediaPlayer = mp
                    applyPendingPlaybackState()
                }
                setOnErrorListener { mp, what, extra ->
                    FirebaseCrashlytics.getInstance().log("MediaPlayer Error: what=$what extra=$extra")
                    release()
                    true
                }
                setOnCompletionListener { mp ->
                    mediaPlayer = mp
                    release()
                }
                setSurface(holder.surface)
                isLooping = true
                setDataSource(this@VideoWallpaperService, Uri.fromFile(file))
                setVolume(volume, volume)
                prepare()
            }
        }

        private fun updatePlaybackState(visible: Boolean, isPaused: Boolean) {
            this.isVisible = visible
            this.isPaused = isPaused
            applyPendingPlaybackState()
        }

        private fun applyPendingPlaybackState() {
            val player = mediaPlayer ?: return

            val shouldPlay = when {
                isPaused -> false
                settings.general.playOffscreen -> true
                else -> isVisible
            }
            runCatching {
                if (shouldPlay) {
                    if (!player.isPlaying) {
                        player.start()
                    }
                } else if (player.isPlaying) {
                    player.pause()
                }
            }.onFailure { error ->
                FirebaseCrashlytics.getInstance().recordException(error)
            }
        }
    }
}