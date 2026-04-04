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

    @Volatile
    private var shouldDraw: Boolean = true
    @Volatile
    private var isVisible: Boolean = false
    @Volatile
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

    /**
     * Draws the content scaled to fill the entire screen dimensions.
     * With cropping off the edges that do not fit the screens aspect ratio
     *
     * @param canvas The [Canvas] on which the content should be drawn.
     */
    protected abstract fun drawFitCrop(canvas: Canvas)

    /**
     * Draws the content scaled to fit the entire screen dimensions.
     *
     * @param canvas The [Canvas] on which the content should be drawn.
     */
    protected abstract fun drawFitToScreen(canvas: Canvas)

    /**
     * Draws the content centered on the provided [canvas].
     *
     * @param canvas The [Canvas] on which the content should be drawn.
     */
    protected abstract fun drawCenter(canvas: Canvas)

    /**
     * Draws the content at its original size and position (top-left) onto the provided [canvas].
     *
     * @param canvas The canvas on which the image drawable will be rendered.
     */
    protected abstract fun drawOriginal(canvas: Canvas)

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
        val canvas = try {
            holder.lockCanvas()
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            return
        }
        //reset canvas state
        canvas.drawColor(Color.BLACK)
        try {
            when (settings.image.scaleType) {
                ImageScaling.FIT_CROP -> drawFitCrop(canvas)
                ImageScaling.FIT_TO_SCREEN -> drawFitToScreen(canvas)
                ImageScaling.CENTER -> drawCenter(canvas)
                ImageScaling.ORIGINAL -> drawOriginal(canvas)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas)
        }
    }
}