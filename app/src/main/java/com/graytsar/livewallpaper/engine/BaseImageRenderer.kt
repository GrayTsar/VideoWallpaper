package com.graytsar.livewallpaper.engine

import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import android.view.SurfaceHolder
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.graytsar.livewallpaper.core.common.model.ImageEngineSettings
import com.graytsar.livewallpaper.core.common.model.ImageScaling
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

abstract class BaseImageRenderer(
    private val holder: SurfaceHolder,
    private val file: File,
    private val settings: ImageEngineSettings
) : WallpaperRenderer {
    private val rendererScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var drawJob: Job? = null
    private var shouldDraw: Boolean = true

    private var isVisible: Boolean = false
    private var isPaused: Boolean = false

    final override fun onSurfaceReady() {
        loadContent(file)
        restartDrawing()
    }

    final override fun onVisibilityChanged(isVisible: Boolean) {
        this.isVisible = isVisible
        updateDrawState()
    }

    final override fun onPauseChanged(isPaused: Boolean) {
        this.isPaused = isPaused
        updateDrawState()
    }

    final override fun release() {
        cleanupContent()
        stopDrawing()
        rendererScope.cancel()
    }

    protected open fun onPlaybackStateChanged(isPlaying: Boolean) = Unit

    protected abstract fun loadContent(file: File)

    protected abstract fun drawOriginal(canvas: Canvas)

    protected abstract fun drawCenter(canvas: Canvas)

    protected abstract fun drawFit(canvas: Canvas)

    protected open fun cleanupContent() = Unit

    private fun updateDrawState() {
        //isPlaying = not paused AND (visible OR play offscreen)
        val shouldPlay = when {
            isPaused -> false
            isVisible -> true
            else -> settings.general.playOffscreen
        }
        onPlaybackStateChanged(shouldPlay)

        when (shouldPlay) {
            true -> startDrawing()
            false -> stopDrawing()
        }
    }

    private fun startDrawing() {
        shouldDraw = true
        /*
         * if draw job is null should start drawing
         * if draw job is not active should start drawing
         * if draw job is completed should start drawing
         */
        if (drawJob == null || drawJob?.isActive == false || drawJob?.isCompleted == true) {
            drawJob = startDrawJob()
        }
    }

    private fun restartDrawing() {
        stopDrawing()
        startDrawing()
    }

    private fun stopDrawing() {
        shouldDraw = false
        drawJob?.let { job ->
            runBlocking { job.cancelAndJoin() }
        }
        drawJob = null
    }

    private fun startDrawJob(): Job {
        return rendererScope.launch {
            try {
                while (holder.surface.isValid && shouldDraw) {
                    if (!isPaused) {
                        drawFrame()
                    }
                    delay(16)
                }
            } catch (e: CancellationException) {
                //expected when stopping the drawing loop, no need to log
            } catch (e: Exception) {
                Log.e("ERROR", e.message.toString(), e)
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }
    }

    private fun drawFrame() {
        //if (!holder.surface.isValid) return
        val canvas = holder.lockCanvas() ?: return
        //reset canvas state
        canvas.drawColor(Color.BLACK)
        try {
            when (settings.image.scaleType) {
                ImageScaling.FIT_TO_SCREEN -> drawFit(canvas)
                ImageScaling.CENTER -> drawCenter(canvas)
                ImageScaling.ORIGINAL -> drawOriginal(canvas)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
}