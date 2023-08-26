package com.graytsar.livewallpaper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

@DelicateCoroutinesApi
class VideoWallpaper : WallpaperService() {
    private var settingsAudio: Boolean = false
    private var settingsVideoCrop: Boolean = false
    private var settingsScaleType: String = ""
    private var settingsDoubleTapToPause: Boolean = false
    private var settingsPlayOffscreen: Boolean = false

    override fun onCreateEngine(): Engine {
        val sharedPref = getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
        val isVideo = sharedPref.getBoolean(keyType, false)

        val settingsSP = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        settingsScaleType =
            settingsSP.getString(getString(R.string.keyGifScaleType), valueDefaultScaleType)!!
        settingsAudio =
            settingsSP.getBoolean(getString(R.string.keyVideoAudio), valueDefaultVideoAudio)
        settingsDoubleTapToPause = settingsSP.getBoolean(
            getString(R.string.keyDoubleTapToPause),
            valueDefaultDoubleTapToPause
        )
        settingsPlayOffscreen =
            settingsSP.getBoolean(getString(R.string.keyPlayOffscreen), valueDefaultPlayOffscreen)
        settingsVideoCrop =
            settingsSP.getBoolean(getString(R.string.keyCropVideo), valueDefaultVideoCrop)

        return if (isVideo) {
            VideoWallpaperEngine()
        } else {
            if (Build.VERSION.SDK_INT >= 28) {
                ImageWallpaperEngineApi28()
            } else {
                ImageWallpaperEngineApi19()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    inner class ImageWallpaperEngineApi28 : Engine() {
        private var animatedImageDrawable: Drawable? = null

        private var holderInstance: SurfaceHolder? = null
        private var drawJob: Job? = null

        private var shouldDraw: Boolean = true
        private var tapTimeBetween: Long = 0L
        private var isPaused: Boolean = false


        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            holderInstance = holder

            val sharedPref = getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
            var fUri: Uri = runCatching {
                Uri.parse(sharedPref.getString(keyVideo, null))
            }.getOrNull() ?: return

            if (isPreview) {
                onPreview(fUri)
            } else {
                onPlay(fUri)
            }

            drawJob = getDrawJob()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                if (animatedImageDrawable is AnimatedImageDrawable) {
                    val anim = animatedImageDrawable as AnimatedImageDrawable
                    anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                    anim.start()
                }

                if (settingsPlayOffscreen) {
                    if (drawJob == null) {
                        shouldDraw = true
                        drawJob = getDrawJob()
                    }
                } else {
                    shouldDraw = true
                    drawJob = getDrawJob()
                }
            } else {
                if (animatedImageDrawable is AnimatedImageDrawable) {
                    val anim = animatedImageDrawable as AnimatedImageDrawable
                    anim.stop()
                }

                if (!settingsPlayOffscreen) {
                    shouldDraw = false
                    drawJob?.cancel()
                    drawJob = null
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            clearImage()
            super.onSurfaceDestroyed(holder)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            if (settingsDoubleTapToPause) {
                when (event?.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - tapTimeBetween <= doubleTapTimeout) {
                            isPaused = !isPaused
                        }
                        tapTimeBetween = currentTime
                    }
                }
            }

            super.onTouchEvent(event)
        }

        override fun onDestroy() {
            clearImage()
            super.onDestroy()
        }

        private fun onPreview(fUri: Uri) {
            val source = ImageDecoder.createSource(contentResolver, fUri)
            animatedImageDrawable = ImageDecoder.decodeDrawable(source)

            if (animatedImageDrawable is AnimatedImageDrawable) {
                val anim = animatedImageDrawable as AnimatedImageDrawable
                anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                anim.start()
            }
        }

        private fun onPlay(fUri: Uri) {
            val basePath = applicationContext.filesDir.path
            val folder = imageFolder

            //create directories if they do not exist
            val directory = File("$basePath/$folder")
            if (!directory.isDirectory) {
                directory.mkdirs()
            }

            try {
                val inputStream = contentResolver.openInputStream(fUri!!)!!
                val fileOutputStream = FileOutputStream("$basePath/$folder/$imageName", false)

                inputStream.copyTo(fileOutputStream)

                fileOutputStream.close()
                inputStream.close()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            val source = ImageDecoder.createSource(File("$basePath/$folder/$imageName"))
            animatedImageDrawable = ImageDecoder.decodeDrawable(source)

            if (animatedImageDrawable is AnimatedImageDrawable) {
                val anim = animatedImageDrawable as AnimatedImageDrawable
                anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                anim.start()
            }
        }

        private fun startDrawOriginal(): Job {
            return GlobalScope.launch {
                try {
                    while (holderInstance != null && holderInstance!!.surface.isValid && shouldDraw) {
                        if (!isPaused) {
                            val canvas = holderInstance?.lockCanvas()
                            animatedImageDrawable?.draw(canvas!!)
                            holderInstance?.unlockCanvasAndPost(canvas!!)
                        }
                        delay(7)
                    }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        private fun startDrawCenter(): Job {
            return GlobalScope.launch {
                try {
                    while (holderInstance != null && holderInstance!!.surface.isValid && shouldDraw) {
                        if (!isPaused) {
                            val canvas = holderInstance?.lockCanvas()

                            animatedImageDrawable?.let { animatedImageDrawable ->
                                val sx: Float =
                                    (canvas!!.width.toFloat() - animatedImageDrawable.intrinsicWidth.toFloat()) / 2
                                val sy: Float =
                                    (canvas.height.toFloat() - animatedImageDrawable.intrinsicHeight.toFloat()) / 2

                                canvas.translate(sx, sy)
                                animatedImageDrawable.draw(canvas)
                            }
                            holderInstance?.unlockCanvasAndPost(canvas!!)
                        }
                        delay(7)
                    }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        private fun startDrawFit(): Job {
            return GlobalScope.launch {
                try {
                    while (holderInstance != null && holderInstance!!.surface.isValid && shouldDraw) {
                        if (!isPaused) {
                            val canvas = holderInstance?.lockCanvas()

                            animatedImageDrawable?.let { animatedImageDrawable ->
                                var ax: Float = animatedImageDrawable.intrinsicWidth.toFloat()
                                var ay: Float = animatedImageDrawable.intrinsicHeight.toFloat()

                                if (ax <= 0) {
                                    ax = 1.0f
                                }
                                if (ay <= 0) {
                                    ay = 1.0f
                                }

                                val sx = canvas!!.width.toFloat() / ax
                                val sy = canvas.height.toFloat() / ay

                                canvas.scale(sx, sy)
                                animatedImageDrawable.draw(canvas)
                            }
                            holderInstance?.unlockCanvasAndPost(canvas!!)
                        }
                        delay(7)
                    }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        private fun getDrawJob(): Job {
            return when (settingsScaleType) {
                valueDefaultScaleType -> startDrawFit()
                "center" -> startDrawCenter()
                "original" -> startDrawOriginal()
                else -> startDrawFit()
            }
        }

        private fun clearImage() {
            if (animatedImageDrawable is AnimatedImageDrawable) {
                (animatedImageDrawable as AnimatedImageDrawable).stop()
            }

            shouldDraw = false
            drawJob?.cancel()
            drawJob = null
        }
    }

    inner class ImageWallpaperEngineApi19 : Engine() {
        private var movie: Movie? = null

        private var holderInstance: SurfaceHolder? = null
        private var drawJob: Job? = null

        private var shouldDraw: Boolean = true
        private var tapTimeBetween: Long = 0L
        private var isPaused: Boolean = false

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)
            holderInstance = holder

            val sharedPref = getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
            val fUri: Uri = runCatching {
                Uri.parse(sharedPref.getString(keyVideo, null))
            }.getOrNull() ?: return

            if (isPreview) onPreview(fUri) else onPlay(fUri)
            drawJob = getDrawJob()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                if (settingsPlayOffscreen) {
                    if (drawJob == null) {
                        shouldDraw = true
                        drawJob = getDrawJob()
                    }
                } else {
                    shouldDraw = true
                    drawJob = getDrawJob()
                }
            } else {
                if (!settingsPlayOffscreen) {
                    shouldDraw = false
                    drawJob?.cancel()
                    drawJob = null
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            clearImage()
            super.onSurfaceDestroyed(holder)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            if (settingsDoubleTapToPause) {
                when (event?.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - tapTimeBetween <= doubleTapTimeout) {
                            isPaused = !isPaused
                        }
                        tapTimeBetween = currentTime
                    }
                }
            }

            super.onTouchEvent(event)
        }

        override fun onDestroy() {
            clearImage()
            super.onDestroy()
        }

        private fun onPreview(fUri: Uri) {
            movie = Movie.decodeStream(contentResolver.openInputStream(fUri))
        }

        private fun onPlay(fUri: Uri) {
            val basePath = applicationContext.filesDir.path
            val folder = imageFolder

            //create directories if they do not exist
            val directory = File("$basePath/$folder")
            if (!directory.isDirectory) {
                directory.mkdirs()
            }

            try {
                val inputStream = contentResolver.openInputStream(fUri)!!
                val fileOutputStream = FileOutputStream("$basePath/$folder/$imageName", false)

                inputStream.copyTo(fileOutputStream)

                fileOutputStream.close()
                inputStream.close()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }

            movie = Movie.decodeStream(File("$basePath/$folder/$imageName").inputStream())
        }

        private fun startDrawOriginal(): Job {
            return GlobalScope.launch {
                try {
                    while (holderInstance != null && holderInstance!!.surface.isValid && shouldDraw) {
                        if (!isPaused) {
                            val canvas = holderInstance?.lockCanvas()

                            movie?.let { movie ->
                                movie.draw(canvas!!, 0f, 0f)
                                movie.setTime((System.currentTimeMillis() % movie.duration()).toInt())
                            }

                            holderInstance?.unlockCanvasAndPost(canvas!!)
                        }
                        delay(7)
                    }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        private fun startDrawCenter(): Job {
            return GlobalScope.launch {
                try {
                    while (holderInstance != null && holderInstance!!.surface.isValid && shouldDraw) {
                        if (!isPaused) {
                            val canvas = holderInstance?.lockCanvas()

                            movie?.let { movie ->
                                val sx: Float =
                                    (canvas!!.width.toFloat() / movie.width().toFloat()) / 2
                                val sy: Float =
                                    (canvas.height.toFloat() / movie.height().toFloat()) / 2

                                movie.draw(canvas, sx, sy)
                                movie.setTime((System.currentTimeMillis() % movie.duration()).toInt())
                            }
                            holderInstance?.unlockCanvasAndPost(canvas!!)
                        }
                        delay(7)
                    }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        private fun startDrawFit(): Job {
            return GlobalScope.launch {
                try {
                    while (holderInstance != null && holderInstance!!.surface.isValid && shouldDraw) {
                        if (!isPaused) {
                            val canvas = holderInstance?.lockCanvas()

                            movie?.let { movie ->
                                val sx = canvas!!.width.toFloat() / movie.width().toFloat()
                                val sy = canvas.height.toFloat() / movie.height().toFloat()
                                canvas.scale(sx, sy)

                                movie.draw(canvas, 0f, 0f)
                                movie.setTime((System.currentTimeMillis() % movie.duration()).toInt())

                            }
                            holderInstance?.unlockCanvasAndPost(canvas!!)
                        }
                        delay(7)
                    }
                } catch (e: Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
        }

        private fun getDrawJob(): Job {
            return when (settingsScaleType) {
                valueDefaultScaleType -> startDrawFit()
                "center" -> startDrawCenter()
                "original" -> startDrawOriginal()
                else -> startDrawFit()
            }
        }

        private fun clearImage() {
            shouldDraw = false
            drawJob?.cancel()
            drawJob = null
        }
    }


    inner class VideoWallpaperEngine : Engine() {
        private var mediaPlayer: MediaPlayer? = null

        private var tapTimeBetween: Long = 0L
        private var isPaused: Boolean = false

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
        }

        override fun onSurfaceCreated(holder: SurfaceHolder?) {
            super.onSurfaceCreated(holder)

            val sharedPref = getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
            val fUri: Uri = runCatching {
                Uri.parse(sharedPref.getString(keyVideo, null))
            }.getOrNull() ?: return

            if (isPreview) onPreview(fUri, holder) else onPlay(fUri, holder)
        }

        private fun onPreview(fUri: Uri, holder: SurfaceHolder?) {
            try {
                mediaPlayer = MediaPlayer.create(
                    this@VideoWallpaper,
                    fUri,
                    VideoWallpaperSurfaceHolder(holder!!)
                )
                mediaPlayer?.isLooping = true

                if (settingsVideoCrop) {
                    mediaPlayer?.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                }

                if (!settingsAudio) {
                    mediaPlayer?.setVolume(0.0f, 0.0f)
                }
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }
        }

        private fun onPlay(fUri: Uri, holder: SurfaceHolder?) {
            val basePath = applicationContext.filesDir.path
            val folder = videoFolder

            //create directories if they do not exist
            val directory = File("$basePath/$folder")
            if (!directory.isDirectory) {
                directory.mkdirs()
            }

            try {
                val inputStream = contentResolver.openInputStream(fUri)!!
                val fileOutputStream = FileOutputStream("$basePath/$folder/$videoName", false)

                inputStream.copyTo(fileOutputStream)

                fileOutputStream.close()
                inputStream.close()
            } catch (e: Exception) {
                FirebaseCrashlytics.getInstance().recordException(e)
            }


            val file = File("$basePath/$videoFolder").listFiles()
            if (file != null && file.isNotEmpty() && file[0].exists()) {
                mediaPlayer = MediaPlayer.create(
                    this@VideoWallpaper,
                    file[0].toUri(),
                    VideoWallpaperSurfaceHolder(holder!!)
                )
                mediaPlayer?.isLooping = true

                if (settingsVideoCrop) {
                    mediaPlayer?.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                }

                if (!settingsAudio) {
                    mediaPlayer?.setVolume(0.0f, 0.0f)
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (!settingsPlayOffscreen) {
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

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            clearVideo()
            super.onSurfaceDestroyed(holder)
        }

        override fun onTouchEvent(event: MotionEvent?) {
            if (settingsDoubleTapToPause) {
                when (event?.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - tapTimeBetween <= doubleTapTimeout) {
                            isPaused = !isPaused
                            if (isPaused) {
                                mediaPlayer?.pause()
                            } else {
                                mediaPlayer?.start()
                            }
                        }
                        tapTimeBetween = currentTime
                    }
                }
            }
            super.onTouchEvent(event)
        }

        override fun onDestroy() {
            clearVideo()
            super.onDestroy()
        }

        private fun clearVideo() {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private class VideoWallpaperSurfaceHolder(private val holder: SurfaceHolder) : SurfaceHolder {
        @SuppressLint("ObsoleteSdkInt")
        override fun setType(type: Int) {
            if (Build.VERSION.SDK_INT <= 11) {
                holder.setType(type)
            }
        }

        override fun getSurface(): Surface {
            return holder.surface
        }

        override fun setSizeFromLayout() {
            holder.setSizeFromLayout()
        }

        override fun lockCanvas(): Canvas {
            return holder.lockCanvas()
        }

        override fun lockCanvas(dirty: Rect?): Canvas {
            return holder.lockCanvas(dirty)
        }

        override fun getSurfaceFrame(): Rect {
            return holder.surfaceFrame
        }

        override fun setFixedSize(width: Int, height: Int) {
            holder.setFixedSize(width, height)
        }

        override fun removeCallback(callback: SurfaceHolder.Callback?) {
            holder.removeCallback(callback)
        }

        override fun isCreating(): Boolean {
            return holder.isCreating
        }

        override fun addCallback(callback: SurfaceHolder.Callback?) {
            holder.addCallback(callback)
        }

        override fun setFormat(format: Int) {
            holder.setFormat(format)
        }

        override fun setKeepScreenOn(screenOn: Boolean) {

        }

        override fun unlockCanvasAndPost(canvas: Canvas?) {
            holder.unlockCanvasAndPost(canvas)
        }
    }
}