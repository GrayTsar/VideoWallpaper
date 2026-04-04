package com.graytsar.livewallpaper.service

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.graytsar.livewallpaper.core.common.model.VideoEngineSettings
import com.graytsar.livewallpaper.core.common.model.VideoScaling
import com.graytsar.livewallpaper.core.common.model.WallpaperFlag
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.repository.UserPreferencesRepository
import com.graytsar.livewallpaper.core.repository.WallpaperSelection
import com.graytsar.livewallpaper.engine.WallpaperRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

//because onApplyWallpaper is not called
var currentFlag = WallpaperFlag.SYSTEM

@AndroidEntryPoint
class VideoWallpaperService : WallpaperService() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreateEngine(): Engine = VideoWallpaperEngine()

    private inner class VideoWallpaperEngine : Engine() {
        private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private var observeJob: Job? = null
        private var renderer: WallpaperRenderer? = null
        private var engineSettings: VideoEngineSettings? = null

        private var isPaused: Boolean = false
        private var isVisibleToUser: Boolean = true

        private val gestureDetector = GestureDetector(
            this@VideoWallpaperService,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    if (engineSettings?.general?.doubleTapToPause == true) {
                        isPaused = !isPaused
                        renderer?.onPauseChanged(isPaused)
                        return true
                    }
                    return false
                }
            })

        @OptIn(FlowPreview::class)
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                currentFlag = WallpaperFlag.from(wallpaperFlags)
            }

            observeJob = engineScope.launch {
                combine(
                    userPreferencesRepository.getVideoEngineSettingsFlow(),
                    userPreferencesRepository.getWallpaperSelectionFlow(
                        isPreview = isPreview,
                        serviceType = WallpaperServiceType.VIDEO,
                        wallpaperFlag = currentFlag
                    )
                ) { settings, selection ->
                    settings to selection
                }.debounce(300L).collect { (settings, selection) ->
                    engineSettings = settings
                    val shouldUpdate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        selection?.flag == WallpaperFlag.from(wallpaperFlags)
                    } else true
                    if (shouldUpdate) {
                        handleUpdate(settings, selection)
                    }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleToUser = visible
            renderer?.onVisibilityChanged(visible)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            if (event != null) gestureDetector.onTouchEvent(event)
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

        private fun handleUpdate(settings: VideoEngineSettings, selection: WallpaperSelection?) {
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
            renderer = VideoRenderer(
                holder = surfaceHolder,
                file = file,
                settings = settings
            ).also { renderer ->
                renderer.onVisibilityChanged(isVisibleToUser)
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
        private val settings: VideoEngineSettings
    ) : WallpaperRenderer {
        private var mediaPlayer: MediaPlayer? = null
        private var isMediaPlayerPrepared: Boolean = false

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

        override fun onVisibilityChanged(isVisible: Boolean) {
            this.isVisible = isVisible
            updatePlayState()
        }

        override fun onPauseChanged(isPaused: Boolean) {
            this.isPaused = isPaused
            updatePlayState()
        }

        override fun release() {
            isMediaPlayerPrepared = false
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
            val videoScalingMode = when (settings.video.videoScaling) {
                VideoScaling.FIT_CROP -> MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                VideoScaling.FIT_TO_SCREEN -> MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                VideoScaling.ORIGINAL -> null
            }
            val volume = if (settings.video.audio) 1.0f else 0.0f

            mediaPlayer = MediaPlayer().apply {
                setOnPreparedListener { mp ->
                    mediaPlayer = mp
                    isMediaPlayerPrepared = true
                    videoScalingMode?.let {
                        mp.setVideoScalingMode(videoScalingMode)
                    }
                    updatePlayState()
                }
                setOnErrorListener { mp, what, extra ->
                    FirebaseCrashlytics.getInstance().log("MediaPlayer Error: what=$what extra=$extra")
                    mediaPlayer = mp
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
                prepareAsync()
            }
        }

        private fun updatePlayState() {
            if (!isMediaPlayerPrepared) return
            val player = mediaPlayer ?: return

            val shouldPlay = when {
                isPaused -> false
                isVisible -> true
                else -> settings.general.playOffscreen
            }

            runCatching {
                val isActuallyPlaying = player.isPlaying

                if (shouldPlay && !isActuallyPlaying) {
                    player.start()
                } else if (player.isPlaying) {
                    player.pause()
                }
            }.onFailure { error ->
                FirebaseCrashlytics.getInstance().recordException(error)

                //If the player is in an invalid state, release it so it can be recreated
                if (error is IllegalStateException) {
                    release()
                }
            }
        }
    }
}