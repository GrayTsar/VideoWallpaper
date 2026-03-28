package com.graytsar.livewallpaper.engine

import android.graphics.Canvas
import android.view.SurfaceHolder
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.graytsar.livewallpaper.util.valueDefaultScaleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

abstract class BaseImageRenderer(
    private val holder: SurfaceHolder,
    private val file: File,
    private val settings: EngineSettings
) : WallpaperRenderer {
    private var drawJob: Job? = null
    private var shouldDraw: Boolean = true
    private var isPaused: Boolean = false
    private val rendererScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    final override fun onSurfaceCreated() {
        loadContent(file)
        restartDrawing()
    }

    final override fun onVisibilityChanged(isVisible: Boolean, isPaused: Boolean) {
        this.isPaused = isPaused
        onImageVisibilityChanged(isVisible)

        if (isVisible) {
            if (settings.playOffscreen) {
                startDrawingIfNeeded()
            } else {
                restartDrawing()
            }
        } else if (!settings.playOffscreen) {
            stopDrawing()
        }
    }

    final override fun onPauseStateChanged(isPaused: Boolean, isVisible: Boolean) {
        this.isPaused = isPaused
    }

    final override fun release() {
        cleanupContent()
        stopDrawing()
        rendererScope.cancel()
    }

    protected open fun onImageVisibilityChanged(visible: Boolean) = Unit

    protected abstract fun loadContent(file: File)

    protected abstract fun drawOriginal(canvas: Canvas)

    protected abstract fun drawCenter(canvas: Canvas)

    protected abstract fun drawFit(canvas: Canvas)

    protected open fun cleanupContent() = Unit

    private fun startDrawingIfNeeded() {
        shouldDraw = true
        if (drawJob == null) {
            drawJob = startDrawJob()
        }
    }

    private fun restartDrawing() {
        stopDrawing()
        startDrawingIfNeeded()
    }

    private fun stopDrawing() {
        shouldDraw = false
        drawJob?.cancel()
        drawJob = null
    }

    private fun startDrawJob(): Job {
        return rendererScope.launch {
            try {
                while (holder.surface.isValid && shouldDraw) {
                    if (!isPaused) {
                        drawFrame()
                    }
                    delay(7)
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private fun drawFrame() {
        val canvas = holder.lockCanvas() ?: return
        try {
            when (settings.scaleType) {
                valueDefaultScaleType -> drawFit(canvas)
                "center" -> drawCenter(canvas)
                "original" -> drawOriginal(canvas)
                else -> drawFit(canvas)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
}