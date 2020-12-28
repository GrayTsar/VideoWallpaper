package com.graytsar.livewallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.graytsar.livewallpaper.databinding.ActivityMainBinding

const val keySharedPrefVideo = "video"
const val keyVideo = "key"
const val keyType = "type"

const val videoFolder = "video"
const val imageFolder = "image"

const val videoName = "video"
const val imageName = "image"

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    private var isVideo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonImage.setOnClickListener {
            onClickImage(it)
        }

        binding.buttonVideo.setOnClickListener {
            onClickVideo(it)
        }
    }

    private fun onClickVideo(view: View){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "video/*"
        isVideo = true
        startActivityForResult(intent, 1)
    }

    private fun onClickImage(view: View){
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "image/*"
        isVideo = false
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(request:Int, result:Int, resultData: Intent?){
        super.onActivityResult(request, result, resultData)

        val fUri:Uri? = resultData?.data
        if(fUri == null) {
            showAlertError("Something went wrong.")
            return
        }

        try{
            WallpaperManager.getInstance(applicationContext).clear()
        } catch (e:Exception){
            FirebaseCrashlytics.getInstance().recordException(e)
        }

        val extension = contentResolver.getType(fUri)
        contentResolver.takePersistableUriPermission(fUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        //problem with selecting multiple mime types. Limits how the SAF "Files" App shows stuff
        //simply add support for non animated images like this
        if(!isVideo && extension != "image/gif" && extension != "image/webp"){
            try {
                val manager:WallpaperManager = WallpaperManager.getInstance(applicationContext)
                manager.setStream(contentResolver.openInputStream(fUri))
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

        if(!checkInputStream(fUri)) {
            showAlertError("Unable to load.")
            return
        }

        saveSettings(fUri, isVideo)

        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, VideoWallpaper::class.java))
        startActivity(intent)
    }

    private fun saveSettings(fUri:Uri, isVideo: Boolean) {
        val sharedPref = this.getSharedPreferences(keySharedPrefVideo, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString(keyVideo, fUri.toString())
        editor.putBoolean(keyType, isVideo)
        editor.apply()
    }

    private fun checkVideo(fUri:Uri):Boolean {
        try {
            var mediaPlayer = MediaPlayer.create(this, fUri).apply {
                isLooping = true
                setVolume(0f,0f)
            }
            mediaPlayer.release()
            mediaPlayer = null

            //val input = contentResolver.openInputStream(fUri)
            //input?.close()
        } catch (e:Exception) {
            logAnalyticsEvent(this, "error", "video", fUri.toString())
            FirebaseCrashlytics.getInstance().recordException(e)
            return false
        }
        return true
    }

    private fun checkInputStream(fUri: Uri):Boolean {
        try {
            val input = contentResolver.openInputStream(fUri)
            input?.close()
        } catch (e:Exception) {
            logAnalyticsEvent(this, "error", "stream", fUri.toString())
            FirebaseCrashlytics.getInstance().recordException(e)
            return false
        }
        return true
    }

    private fun showAlertError(message:String){
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialog))
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->

            }
            .show()
    }

    private fun logAnalyticsEvent(context: Context?, event:String, param:String, value:String){
        context?.let {
            val bundle = Bundle()
            bundle.putString(param, value)
            FirebaseAnalytics.getInstance(it).logEvent(event , bundle)
        }
    }
}
