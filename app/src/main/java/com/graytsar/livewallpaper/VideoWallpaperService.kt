@file:Suppress("DEPRECATION")

package com.graytsar.livewallpaper

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.Surface
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

    private fun getActiveFilePath(isPreview: Boolean): String? {
        return runBlocking {
            if (isPreview) {
                userPreferencesRepository.getPreviewPath() ?: userPreferencesRepository.getWallpaperPath()
            } else {
                val previewPath = userPreferencesRepository.getPreviewPath()
                val previewType = userPreferencesRepository.getPreviewWallpaperType()
                if (previewPath != null) {
                    userPreferencesRepository.setWallpaperPath(previewPath)
                    userPreferencesRepository.setWallpaperType(previewType)
                    userPreferencesRepository.setPreviewPath(null)
                    previewPath
                } else {
                    userPreferencesRepository.getWallpaperPath()
                }
            }
        }
    }

    override fun onCreateEngine(): Engine {
        return UnifiedWallpaperEngine(
            settings = loadSettings()
        )
    }

    private fun loadSettings(): EngineSettings {
        return runBlocking {
            EngineSettings(
                audio = userPreferencesRepository.getVideoAudio(),
                videoCrop = userPreferencesRepository.getVideoCrop(),
                scaleType = userPreferencesRepository.getGifScaleType(),
                doubleTapToPause = userPreferencesRepository.getDoubleTapToPause(),
                playOffscreen = userPreferencesRepository.getPlayOffscreen()
            )
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
            val filePath = getActiveFilePath(isPreview) ?: return
            val wallpaperType = runBlocking {
                if (isPreview) {
                    userPreferencesRepository.getPreviewWallpaperType()
                } else {
                    userPreferencesRepository.getWallpaperType()
                }
            }
            val file = File(filePath)
            if (!file.exists()) {
                return
            }

            clearRenderer()
            renderer = createRenderer(wallpaperType!!, surfaceHolder, file, settings)
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

        override fun onSurfaceCreated() {
            try {
                mediaPlayer = MediaPlayer.create(
                    this@VideoWallpaperService,
                    Uri.fromFile(file),
                    VideoWallpaperSurfaceHolder(holder)
                )
                mediaPlayer?.isLooping = true

                if (settings.videoCrop) {
                    mediaPlayer?.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                }

                if (!settings.audio) {
                    mediaPlayer?.setVolume(0.0f, 0.0f)
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }

        override fun onVisibilityChanged(isVisible: Boolean, isPaused: Boolean) {
            applyPlaybackState(isVisible, isPaused)
        }

        override fun onPauseStateChanged(isPaused: Boolean, isVisible: Boolean) {
            applyPlaybackState(isVisible, isPaused)
        }

        override fun release() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }

        private fun applyPlaybackState(visible: Boolean, isPaused: Boolean) {
            if (!settings.playOffscreen) {
                if (!isPaused && visible) {
                    mediaPlayer?.start()
                } else {
                    mediaPlayer?.pause()
                }
            } else {
                if (isPaused) {
                    mediaPlayer?.pause()
                } else {
                    mediaPlayer?.start()
                }
            }
        }
    }
}

class VideoWallpaperSurfaceHolder(private val holder: SurfaceHolder) : SurfaceHolder {
    @Suppress("DEPRECATION")
    @SuppressLint("ObsoleteSdkInt")
    @Deprecated("Deprecated in Java")
    override fun setType(type: Int) {
        if (Build.VERSION.SDK_INT <= 11) {
            holder.setType(type)
        }
    }

    override fun getSurface(): Surface = holder.surface
    override fun setSizeFromLayout() {
        holder.setSizeFromLayout()
    }

    override fun lockCanvas(): Canvas = holder.lockCanvas()
    override fun lockCanvas(dirty: Rect?): Canvas = holder.lockCanvas(dirty)
    override fun getSurfaceFrame(): Rect = holder.surfaceFrame
    override fun setFixedSize(width: Int, height: Int) {
        holder.setFixedSize(width, height)
    }

    override fun removeCallback(callback: SurfaceHolder.Callback?) {
        holder.removeCallback(callback)
    }

    override fun isCreating(): Boolean = holder.isCreating
    override fun addCallback(callback: SurfaceHolder.Callback?) {
        holder.addCallback(callback)
    }

    override fun setFormat(format: Int) {
        holder.setFormat(format)
    }

    override fun setKeepScreenOn(screenOn: Boolean) {}
    override fun unlockCanvasAndPost(canvas: Canvas?) {
        holder.unlockCanvasAndPost(canvas)
    }
}