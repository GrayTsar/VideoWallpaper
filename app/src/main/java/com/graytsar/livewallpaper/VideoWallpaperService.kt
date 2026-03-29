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
import com.graytsar.livewallpaper.util.WallpaperType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class VideoWallpaperService : WallpaperService() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreateEngine(): Engine {
        return UnifiedWallpaperEngine(
            settings = loadSettings()
        )
    }

    private fun loadSettings(): EngineSettings {
        return runBlocking {
            userPreferencesRepository.getEngineSettings()
        }
    }

    private fun createRenderer(
        wallpaperType: WallpaperType,
        holder: SurfaceHolder,
        file: File,
        settings: EngineSettings
    ): WallpaperRenderer {
        return when (wallpaperType) {
            WallpaperType.VIDEO -> VideoRenderer(holder, file, settings)
            WallpaperType.IMAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Api28ImageRenderer(holder, file, settings)
                } else {
                    LegacyImageRenderer(holder, file, settings)
                }
            }
        }
    }

    private inner class UnifiedWallpaperEngine(
        private val settings: EngineSettings
    ) : Engine() {
        private var renderer: WallpaperRenderer? = null
        private var tapTimeBetween: Long = 0L
        private var isPaused: Boolean = false
        private var isVisibleToUser: Boolean = false
        private var hasVisibilityState: Boolean = false

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)

            val surfaceHolder = holder ?: return
            val selection = runBlocking {
                userPreferencesRepository.resolveSelectionForEngine(isPreview)
            } ?: return
            val file = File(selection.path)
            if (!file.exists()) {
                return
            }

            clearRenderer()
            renderer = createRenderer(selection.wallpaperType, surfaceHolder, file, settings)
            renderer?.onSurfaceCreated()
            if (hasVisibilityState) {
                renderer?.onVisibilityChanged(isVisibleToUser, isPaused)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            hasVisibilityState = true
            isVisibleToUser = visible
            renderer?.onVisibilityChanged(visible, isPaused)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            if (settings.doubleTapToPause && event?.actionMasked == MotionEvent.ACTION_DOWN) {
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
            clearRenderer()
            super.onSurfaceDestroyed(holder)
        }

        override fun onDestroy() {
            clearRenderer()
            super.onDestroy()
        }

        private fun clearRenderer() {
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
        private var isPrepared: Boolean = false
        private var isReleased: Boolean = false
        private var hasPendingPlaybackState: Boolean = false
        private var pendingVisibility: Boolean = false
        private var pendingPauseState: Boolean = false

        override fun onSurfaceCreated() {
            try {
                isReleased = false
                isPrepared = false
                hasPendingPlaybackState = false
                val videoScalingMode = if (settings.videoCrop)
                    MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                else
                    MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT
                val volume = if (settings.audio) 1.0f else 0.0f

                mediaPlayer = MediaPlayer().apply {
                    setOnPreparedListener { preparedPlayer ->
                        if (isReleased || mediaPlayer !== preparedPlayer) {
                            return@setOnPreparedListener
                        }

                        isPrepared = true
                        applyPendingPlaybackState()
                    }
                    setOnErrorListener { mp, what, extra ->
                        FirebaseCrashlytics.getInstance().log("MediaPlayer Error: what=$what extra=$extra")
                        release()
                        true
                    }
                    setDataSource(this@VideoWallpaperService, Uri.fromFile(file))
                    setSurface(holder.surface)
                    isLooping = true
                    setVideoScalingMode(videoScalingMode)
                    setVolume(volume, volume)
                    prepareAsync()
                }
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
            isReleased = true
            hasPendingPlaybackState = false

            val player = mediaPlayer ?: return
            val wasPrepared = isPrepared

            mediaPlayer = null
            isPrepared = false

            if (wasPrepared) {
                runCatching {
                    player.stop()
                }.onFailure { error ->
                    FirebaseCrashlytics.getInstance().recordException(error)
                }
            }

            runCatching {
                player.release()
            }.onFailure { error ->
                FirebaseCrashlytics.getInstance().recordException(error)
            }
        }

        private fun updatePlaybackState(visible: Boolean, isPaused: Boolean) {
            pendingVisibility = visible
            pendingPauseState = isPaused
            hasPendingPlaybackState = true
            applyPendingPlaybackState()
        }

        private fun applyPendingPlaybackState() {
            val player = mediaPlayer ?: return
            if (!isPrepared || isReleased || !hasPendingPlaybackState) {
                return
            }

            val isUserPaused = pendingPauseState
            val isHidden = !pendingVisibility

            val shouldPlay = when {
                isUserPaused -> false
                settings.playOffscreen -> true
                else -> !isHidden
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