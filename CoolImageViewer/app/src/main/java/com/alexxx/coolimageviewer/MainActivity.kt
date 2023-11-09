package com.alexxx.coolimageviewer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.image_item.view.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Reader
import java.io.Serializable
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap


class MainActivity : AppCompatActivity() {
    private lateinit var downloadIntent: Intent

    private val IMAGE_LIST_KEY = "IMAGE_LIST_KEY"

    enum class DownloadStatus {
        NOT_DOWNLOADED, DOWNLOADING, DOWNLOADED
    }

    val imageList = ArrayList<Image>()

    companion object {
        enum class ImageType {
            PREVIEW, HIGH_RES
        }

        enum class ResultType {
            SUCCESS, FAILED
        }

        class initImageList(context: MainActivity) : AsyncTask<Void, Void, String>() {
            private val activityReference: WeakReference<MainActivity> = WeakReference(context)
            private val accessKey = "2Xxvj52ToOBqDpDnJaLa01uLL5OdrGFzDbeLxeoFUSs"
            private val picsUrl =
                "https://api.unsplash.com/search/photos?page=1&per_page=50&query=cat&client_id=$accessKey"

            private fun readAll(rd: Reader): String {
                val sb = StringBuilder()
                var cp: Int
                while (rd.read().also { cp = it } != -1) {
                    sb.append(cp.toChar())
                }
                return sb.toString()
            }

            override fun doInBackground(vararg params: Void?): String {
                while (true) {
                    try {
                        val con = URL(picsUrl).openConnection() as HttpURLConnection
                        con.doInput = true
                        con.connect()
                        val responseCode = con.responseCode
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val iS = con.inputStream
                            val rd = BufferedReader(InputStreamReader(iS, StandardCharsets.UTF_8))
                            val jsonText = readAll(rd)
                            iS.close()
                            return jsonText
                        }
                    } catch (e: Exception) {
                        Log.d("asyncTask", "Error in downloading list $e")
                    }
                    Thread.sleep(5000)
                    Log.d("asyncTask", "Unsuccessful try to download list")
                }
            }

            override fun onPostExecute(jsonText: String?) {
                super.onPostExecute(jsonText)
                val parser = JsonParser()
                val parsed: JsonObject = parser.parse(jsonText).asJsonObject
                val imgList = ArrayList<Image>()
                for (rawResult in parsed.asJsonObject["results"].asJsonArray) {
                    val result = rawResult.asJsonObject
                    val title = if (result["description"].isJsonNull) {
                        "Undefined"
                    } else {
                        result["description"].asString
                    }
                    imgList.add(
                        Image(
                            result["id"].asString,
                            title,
                            DownloadStatus.NOT_DOWNLOADED,
                            result["urls"].asJsonObject["thumb"].asString,
                            result["urls"].asJsonObject["regular"].asString
                        )
                    )
                }
                activityReference.get()!!.updateImgList(imgList)
                Log.d("postExecute", "${imgList.size}")
            }
        }
    }

    private var mMyBroadcastReceiver = MyBroadcastReceiver()

    fun updateImgList(nImgList: ArrayList<Image>) {
        imageList.clear()
        imageList.addAll(nImgList)
        recyclerView.adapter!!.notifyDataSetChanged()
        for (i in 0 until imageList.size) startDownloading(imageList[i], ImageType.PREVIEW)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        downloadIntent = Intent(this, DownloadService::class.java)
        val viewManager = LinearLayoutManager(this)
        recyclerView.apply {
            layoutManager = viewManager
            adapter = ImageAdapter(imageList, fun(pos, it) {
                when (it.status) {
                    DownloadStatus.NOT_DOWNLOADED -> {
                        it.status = DownloadStatus.DOWNLOADING
                        adapter!!.notifyItemChanged(pos)
                        startDownloading(it, ImageType.HIGH_RES)
                    }
                    DownloadStatus.DOWNLOADING -> {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.downloadingInProgress),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    DownloadStatus.DOWNLOADED -> {
                        openHiRes(it)
                    }
                }
            })
        }

        hiResImageView.setOnClickListener {
            it.visibility = View.GONE
        }

        val intentFilter = IntentFilter(DownloadService.ACTION_MYINTENTSERVICE)
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)
        registerReceiver(mMyBroadcastReceiver, intentFilter)

        if (savedInstanceState != null && savedInstanceState.containsKey(IMAGE_LIST_KEY)) {
            updateImgList(savedInstanceState.getSerializable(IMAGE_LIST_KEY) as ArrayList<Image>)
        } else {
            Toast.makeText(this, getString(R.string.downloadingInProgress), Toast.LENGTH_LONG).show()
            initImageList(this).execute()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val lst = ArrayList<Image>()
        lst.addAll(imageList)
        outState.putSerializable(IMAGE_LIST_KEY, lst)
        super.onSaveInstanceState(outState)
    }

    private fun openHiRes(image: Image) {
        hiResImageView.visibility = View.VISIBLE
        hiResImageView.setImageBitmap(DownloadService.downloadedImages[image.urlHiRes])
    }

    private fun startDownloading(image: Image, imageType: ImageType) {
        Log.d("Sending download req.", image.id)
        val url = if (imageType == ImageType.PREVIEW) {
            image.urlPreview
        } else {
            image.urlHiRes
        }
        startService(
            downloadIntent.putExtra(DownloadService.ID_KEY, image.id)
                .putExtra(DownloadService.URL_KEY, url)
                .putExtra(DownloadService.IMAGE_TYPE_KEY, imageType)
        )
    }

    fun downloadEnumToString(status: DownloadStatus): String {
        return when (status) {
            DownloadStatus.NOT_DOWNLOADED -> getString(R.string.not_downloaded)
            DownloadStatus.DOWNLOADING -> getString(R.string.downloading)
            DownloadStatus.DOWNLOADED -> getString(R.string.downloaded)
        }
    }


    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val id = intent.getStringExtra(DownloadService.ID_KEY)
            var imageId : Int = -1
            for (im in 0 until imageList.size) {
                if (imageList[im].id == id) {
                    imageId = im
                }
            }
            if (imageId == -1) {
                Log.d("broadcastReceiver", "No card position for updating info")
                return
            }
            val result = intent
                .getSerializableExtra(DownloadService.RESULT_KEY) as ResultType
            val imageType = intent.getSerializableExtra(DownloadService.IMAGE_TYPE_KEY)

            if (result == ResultType.SUCCESS) {
                if (imageType == ImageType.HIGH_RES) {
                    imageList[imageId].status = DownloadStatus.DOWNLOADED
                }
            } else {
                val errorMessage = intent.getStringExtra(DownloadService.ERROR_KEY)
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_SHORT).show()
                if (imageType == ImageType.HIGH_RES) {
                    imageList[imageId].status = DownloadStatus.NOT_DOWNLOADED
                }
            }
            recyclerView.adapter!!.notifyItemChanged(imageId)
        }
    }

    data class Image(
        val id: String,
        val title: String,
        var status: DownloadStatus,
        val urlPreview: String,
        val urlHiRes: String
    ) : Serializable

    inner class ImageViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        fun bind(image: Image) {
            with(root) {
                imageTitle.text = image.title
                imageStatus.text = downloadEnumToString(image.status)
                if (DownloadService.downloadedImages.contains(key = image.urlPreview)) {
                    imagePreview.setImageBitmap(DownloadService.downloadedImages[image.urlPreview])
                }
            }
        }
    }

    inner class ImageAdapter(
        private val images: List<Image>,
        private val onClick: (Int, Image) -> Unit
    ) : RecyclerView.Adapter<ImageViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val holder = ImageViewHolder(
                LayoutInflater
                    .from(parent.context)
                    .inflate(R.layout.image_item, parent, false)
            )
            holder.root.setOnClickListener {
                onClick(holder.adapterPosition, images[holder.adapterPosition])
            }
            return holder
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) =
            holder.bind(images[position])

        override fun getItemCount() = images.size
    }
}