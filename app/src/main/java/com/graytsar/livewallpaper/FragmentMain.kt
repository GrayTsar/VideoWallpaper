package com.graytsar.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.graytsar.livewallpaper.databinding.FragmentMainBinding

class FragmentMain: Fragment() {
    private lateinit var binding:FragmentMainBinding

    private var isVideo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)

        binding.buttonImage.setOnClickListener {
            onClickImage(it)
        }

        binding.buttonVideo.setOnClickListener {
            onClickVideo(it)
        }

        return binding.root
    }


    private fun onClickVideo(view: View){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "video/*"
        isVideo = true

        try {
            startActivityForResult(intent, 1)
        } catch (e: Exception) {
            showAlertError("No File Manager found. Please install \"Files by Google\" or a similar File Manager")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun onClickImage(view: View){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        isVideo = false

        try {
            startActivityForResult(intent, 1)
        } catch (e: Exception) {
            showAlertError("No File Manager found. Please install \"Files by Google\" or a similar File Manager")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    override fun onActivityResult(request:Int, result:Int, resultData: Intent?){
        super.onActivityResult(request, result, resultData)

        val fUri: Uri? = resultData?.data
        if(fUri == null) {
            return
        }

        context?.let { context ->
            if(context is MainActivity) {
                try{
                    WallpaperManager.getInstance(context.applicationContext).clear()
                } catch (e:Exception){
                    FirebaseCrashlytics.getInstance().recordException(e)
                }

                val extension = context.contentResolver.getType(fUri)

                try {
                    context.contentResolver.takePersistableUriPermission(fUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e:Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }

                //problem with selecting multiple mime types. Limits how the SAF "Files" App shows stuff
                //simply add support for non animated images like this
                if(!isVideo && extension != "image/gif" && extension != "image/webp"){
                    try {
                        val manager: WallpaperManager = WallpaperManager.getInstance(context.applicationContext)
                        manager.setStream(context.contentResolver.openInputStream(fUri))
                        return
                    } catch (e:Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        showAlertError("Something went wrong.")
                        return
                    }
                }

                if(isVideo && !checkVideo(fUri)) {
                    showAlertError("Unable to load video.")
                    return
                }

                if(!isVideo && !checkImage(fUri)) {
                    showAlertError("Unable to load image.")
                    return
                }

                if(!checkInputStream(fUri)) {
                    showAlertError("Unable to load.")
                    return
                }

                saveSettings(fUri, isVideo)

                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, VideoWallpaper::class.java))

                try {
                    startActivity(intent)
                } catch (e1:Exception) {
                    FirebaseCrashlytics.getInstance().recordException(e1)
                    try {
                        startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e2:Exception) {
                        FirebaseCrashlytics.getInstance().recordException(e2)
                        showAlertError("Can not show Live Wallpaper on this device.")
                    }
                }
            }
        }
    }

    private fun saveSettings(fUri: Uri, isVideo: Boolean) {
        val sharedPref = requireContext().getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(keyVideo, fUri.toString())
        editor.putBoolean(keyType, isVideo)
        editor.apply()
    }

    private fun checkVideo(fUri: Uri):Boolean {
        try {
            var mediaPlayer = MediaPlayer.create(context, fUri).apply {
                isLooping = true
                setVolume(0f,0f)
            }
            mediaPlayer.release()
            mediaPlayer = null
        } catch (e:Exception) {
            logAnalyticsEvent(context, "error", "video", fUri.toString())
            FirebaseCrashlytics.getInstance().recordException(e)
            return false
        }
        return true
    }

    private fun checkImage(fUri: Uri): Boolean {
        if(context is MainActivity && context != null) {
            try {
                val cr: ContentResolver = requireContext().contentResolver

                if(Build.VERSION.SDK_INT >= 28){
                    var source: ImageDecoder.Source? = ImageDecoder.createSource(cr, fUri)
                    var animatedImageDrawable: Drawable? = ImageDecoder.decodeDrawable(source!!)

                    animatedImageDrawable = null
                    source = null
                } else {
                    var inputStream = cr.openInputStream(fUri)
                    inputStream?.read()
                    var movie = Movie.decodeStream(inputStream)
                    movie = null
                    inputStream?.close()
                    inputStream = null
                }
            } catch (e: Exception) {
                logAnalyticsEvent(context, "error", "image", fUri.toString())
                FirebaseCrashlytics.getInstance().recordException(e)
                return false
            }
        }
        return true
    }

    private fun checkInputStream(fUri: Uri):Boolean {
        try {
            val cr: ContentResolver = requireContext().contentResolver
            val input = cr.openInputStream(fUri)
            input?.close()
        } catch (e:Exception) {
            logAnalyticsEvent(context, "error", "stream", fUri.toString())
            FirebaseCrashlytics.getInstance().recordException(e)
            return false
        }
        return true
    }

    private fun showAlertError(message:String){
        AlertDialog.Builder(ContextThemeWrapper(context, R.style.AlertDialog))
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->

            }
            .show()
    }

    private fun logAnalyticsEvent(context: Context?, event:String, param:String, value:String){
        /*
        context?.let {
            val bundle = Bundle()
            bundle.putString(param, value)
            FirebaseAnalytics.getInstance(it).logEvent(event , bundle)
        }

         */
    }
}