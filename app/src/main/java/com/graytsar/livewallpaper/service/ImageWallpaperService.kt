package com.graytsar.livewallpaper.service

import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.graytsar.livewallpaper.core.common.model.ImageEngineSettings
import com.graytsar.livewallpaper.core.common.model.WallpaperFlag
import com.graytsar.livewallpaper.core.common.model.WallpaperServiceType
import com.graytsar.livewallpaper.core.repository.UserPreferencesRepository
import com.graytsar.livewallpaper.core.repository.WallpaperSelection
import com.graytsar.livewallpaper.engine.Api28ImageRenderer
import com.graytsar.livewallpaper.engine.LegacyImageRenderer
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

@AndroidEntryPoint
class ImageWallpaperService : WallpaperService() {
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreateEngine(): Engine = ImageWallpaperEngine()

    private fun createRenderer(
        holder: SurfaceHolder,
        file: File,
        settings: ImageEngineSettings
    ): WallpaperRenderer {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Api28ImageRenderer(holder, file, settings)
        } else {
            LegacyImageRenderer(holder, file, settings)
        }
    }

    @OptIn(FlowPreview::class)
    private inner class ImageWallpaperEngine : Engine() {
        private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private var observeJob: Job? = null
        private var renderer: WallpaperRenderer? = null
        private var engineSettings: ImageEngineSettings? = null

        private var tapTimeBetween: Long = 0L
        private var isPaused: Boolean = false

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                currentFlag = WallpaperFlag.from(wallpaperFlags)
            }

            observeJob = engineScope.launch {
                combine(
                    userPreferencesRepository.getImageEngineSettingsFlow(),
                    userPreferencesRepository.getWallpaperSelectionFlow(
                        isPreview = isPreview,
                        serviceType = WallpaperServiceType.IMAGE,
                        wallpaperFlag = currentFlag
                    )
                ) { settings, selection ->
                    settings to selection
                }.debounce(300L).collect { (settings, selection) ->
                    engineSettings = settings
                    val shouldUpdate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        selection?.flag == WallpaperFlag.from(wallpaperFlags)
                    } else {
                        true
                    }
                    if (shouldUpdate) {
                        handleUpdate(settings, selection)
                    }
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            renderer?.onVisibilityChanged(visible)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            val doubleTapToPause = engineSettings?.general?.doubleTapToPause ?: false
            if (doubleTapToPause && event?.actionMasked == MotionEvent.ACTION_DOWN) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - tapTimeBetween <= 500L) {
                    isPaused = !isPaused
                    renderer?.onPauseChanged(isPaused)
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

        private fun handleUpdate(settings: ImageEngineSettings, selection: WallpaperSelection?) {
            val surfaceHolder = surfaceHolder ?: return
            if (selection == null) {
                releaseRenderer()
                return
            }

            val file = File(selection.path)
            if (!file.exists()) {
                releaseRenderer()
                return
            }

            releaseRenderer()
            renderer = createRenderer(
                holder = surfaceHolder,
                file = file,
                settings = settings
            ).also { renderer ->
                renderer.onSurfaceReady()
            }
        }

        private fun releaseRenderer() {
            renderer?.release()
            renderer = null
        }
    }
}