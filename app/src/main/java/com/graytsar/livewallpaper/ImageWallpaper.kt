package com.graytsar.livewallpaper

import android.content.Context
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ImageWallpaper: WallpaperService() {
    override fun onCreateEngine(): Engine {
        return object: WallpaperService.Engine(){
            var d: Drawable? = null
            var fUri:String? = null

            init{
                val sharedPref = getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
                fUri = sharedPref.getString(keyVideo, null)

                if(fUri != null){
                    val source = ImageDecoder.createSource(contentResolver, Uri.parse(fUri))
                    d = ImageDecoder.decodeDrawable(source)

                    if(d is AnimatedImageDrawable){
                        val anim = d as AnimatedImageDrawable
                        anim.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                        anim.start()

                    }
                }
            }

            override fun onCreate(surfaceHolder: SurfaceHolder?) {
                Log.d("DBG:", "onCreate Engine")

                //super.onCreate(surfaceHolder)
            }

            override fun onSurfaceCreated(holder: SurfaceHolder?) {
                Log.d("DBG:", "onSurfaceCreated")

                GlobalScope.launch {
                    val h = holder!!
                    while(true){
                        Log.d("DBG:", "draw")
                        val canvas = h.lockCanvas()
                        d?.draw(canvas!!)
                        h.unlockCanvasAndPost(canvas)
                    }
                }

                super.onSurfaceCreated(holder)
            }

            override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                Log.d("DBG:", "onSurfaceChanged")

                super.onSurfaceChanged(holder, format, width, height)
            }

            override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
                Log.d("DBG:", "onSurfaceDestroyed")

                super.onSurfaceDestroyed(holder)
            }

            override fun onVisibilityChanged(visible: Boolean) {
                Log.d("DBG:", "onVisibilityChanged")



                super.onVisibilityChanged(visible)
            }
        }


    }

    class mSurfaceHolder(private val holder: SurfaceHolder): SurfaceHolder {
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