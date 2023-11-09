package com.alexxx.coolimageviewer

import android.app.IntentService
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap


class DownloadService : IntentService("Downloader") {
    companion object{
        const val ACTION_MYINTENTSERVICE = "com.alexxx.downloadService"
        const val ID_KEY = "ID"
        const val URL_KEY = "URL"
        const val RESULT_KEY = "RESULT"
        const val ERROR_KEY = "ERROR"
        const val IMAGE_TYPE_KEY = "IMAGE_TYPE"

        val downloadedImages = ConcurrentHashMap<String, Bitmap>()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("DS", "onCreate")
    }

    override fun onHandleIntent(intent: Intent?) {
        try{
            Log.d("DS", "handled intent")
            if (intent == null) return
            val id = intent.getStringExtra(ID_KEY)
            val url = intent.getStringExtra(URL_KEY)!!

            val responseIntent = Intent()
            responseIntent.action = ACTION_MYINTENTSERVICE
            responseIntent.addCategory(Intent.CATEGORY_DEFAULT)
            responseIntent.putExtra(ID_KEY, id)
            responseIntent.putExtra(URL_KEY, url)
            responseIntent.putExtra(IMAGE_TYPE_KEY, intent.getSerializableExtra(IMAGE_TYPE_KEY))

            try {
                if (downloadedImages.contains(key=url)) {
                    responseIntent.putExtra(RESULT_KEY, MainActivity.Companion.ResultType.SUCCESS)
                } else {
                    Log.d("DS", "starting real downloading")
                    val con: HttpURLConnection = URL(url).openConnection() as HttpURLConnection
                    con.doInput = true
                    con.connect()
                    val responseCode = con.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val iS = con.inputStream
                        downloadedImages.put(url, BitmapFactory.decodeStream(iS))
                        iS.close()
                        responseIntent.putExtra(
                            RESULT_KEY,
                            MainActivity.Companion.ResultType.SUCCESS
                        )
                    } else {
                        responseIntent.putExtra(
                            RESULT_KEY,
                            MainActivity.Companion.ResultType.FAILED
                        )
                        responseIntent.putExtra(ERROR_KEY, "Server response code isn't 200")
                    }
                }
            } catch (exc: IOException) {
                responseIntent.putExtra(RESULT_KEY, MainActivity.Companion.ResultType.FAILED)
                responseIntent.putExtra(ERROR_KEY, exc.message)
            }
            Log.d("DS", "${responseIntent.getSerializableExtra(RESULT_KEY)}")
            sendBroadcast(responseIntent)
        } catch (exc: Exception) {
            Log.d("DS", "Error in download serv. $exc")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DS", "onDestroy")
    }
}