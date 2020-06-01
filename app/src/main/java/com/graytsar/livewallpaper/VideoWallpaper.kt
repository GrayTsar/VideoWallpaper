package com.graytsar.livewallpaper

import android.content.Context
import android.graphics.*
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.Surface
import android.view.SurfaceHolder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.Exception

class VideoWallpaper:WallpaperService() {
    override fun onCreateEngine(): Engine {
        return object:WallpaperService.Engine(){
            var mediaPlayer:MediaPlayer? = null
            var fUri:String? = null
            var isVideo = false

            var holderInstance:SurfaceHolder? = null

            //api < 28
            var movie: Movie? = null

            //api >= 28
            var drawable:Drawable? = null
            var dJob:Job? = null

            init{
                val sharedPref = getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
                fUri = sharedPref.getString(keyVideo, null)
                isVideo = sharedPref.getBoolean(keyType, false)
            }

            override fun onSurfaceCreated(holder: SurfaceHolder?) {
                holderInstance = holder

                if (isVideo) {
                    clearImage()

                    mediaPlayer = MediaPlayer.create(this@VideoWallpaper, Uri.parse(fUri), mSurfaceHolder(holder!!)).apply {
                        isLooping = true
                        setVolume(0f,0f)
                    }
                } else if(!isVideo){
                    clearVideo()

                    if(Build.VERSION.SDK_INT >= 28){
                            val source = ImageDecoder.createSource(contentResolver, Uri.parse(fUri))
                            drawable = ImageDecoder.decodeDrawable(source)

                            if(drawable is AnimatedImageDrawable){
                                val anim = drawable as AnimatedImageDrawable
                                anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                                anim.start()
                            }
                    } else {
                            movie = Movie.decodeStream(contentResolver.openInputStream(Uri.parse(fUri)))
                    }
                    dJob = startJob()
                }
                super.onSurfaceCreated(holder)
            }

            override fun onVisibilityChanged(visible: Boolean) {
                if(visible){
                    mediaPlayer?.start()

                    if(Build.VERSION.SDK_INT >= 28){
                        if(drawable is AnimatedImageDrawable){
                            val anim = drawable as AnimatedImageDrawable
                            anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                            anim.start()
                            dJob = startJob()
                        }
                    }
                } else {
                    mediaPlayer?.pause()

                    if(Build.VERSION.SDK_INT >= 28){
                        if(drawable is AnimatedImageDrawable){
                            val anim = drawable as AnimatedImageDrawable
                            anim.stop()
                            dJob?.cancel()
                        }
                    }
                }

                super.onVisibilityChanged(visible)
            }

            override fun onDestroy() {
                super.onDestroy()

                clearVideo()
                clearImage()
            }

            private fun startJob(): Job {
                return GlobalScope.launch {
                    try{
                        while(holderInstance != null){
                            val canvas = holderInstance?.lockCanvas()

                            if(Build.VERSION.SDK_INT >= 28){
                                drawable?.draw(canvas!!)
                            } else {
                                movie?.let {
                                    it.draw(canvas!!, 0f, 0f)
                                    it.setTime((System.currentTimeMillis() % movie!!.duration()).toInt())
                                }
                            }
                            holderInstance?.unlockCanvasAndPost(canvas!!)
                        }
                    } catch (e:Exception){

                    }
                }
            }

            private fun clearImage(){
                dJob?.cancel()
                dJob = null

                if(Build.VERSION.SDK_INT >= 28){
                    if(drawable is AnimatedImageDrawable) {
                        (drawable as AnimatedImageDrawable).stop()
                    }
                }

                drawable = null
                movie = null
            }

            private fun clearVideo(){
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }

            /*
            private fun drawError(holder: SurfaceHolder){
                val c = holder.lockCanvas()
                val rect = c.clipBounds
                c.drawText("No permission", 50f, rect.centerY().toFloat()- 50f,
                    Paint().apply {
                        color = Color.WHITE
                        textSize = 52f
                    })
                c.drawText("Open App to get Permission", 50f, rect.centerY().toFloat(),
                    Paint().apply {
                        color = Color.WHITE
                        textSize = 52f
                    })
                c.drawText("And Select Video / Image", 50f, rect.centerY().toFloat() + 50,
                    Paint().apply {
                        color = Color.WHITE
                        textSize = 52f
                    })
                holder.unlockCanvasAndPost(c)
            }
            */
        }

    }

    class mSurfaceHolder(private val holder: SurfaceHolder):SurfaceHolder{
        override fun setType(type: Int) {
            holder.setType(type)
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