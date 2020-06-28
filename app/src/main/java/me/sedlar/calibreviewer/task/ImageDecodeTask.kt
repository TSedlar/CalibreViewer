package me.sedlar.calibreviewer.task

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.ImageView
import java.io.File

class ImageDecodeTask(private val imgFile: File, private val priorityImgFile: File? = null) :
    AsyncTask<ImageView, Void, Bitmap>() {

    private var view: ImageView? = null

    override fun doInBackground(vararg params: ImageView?): Bitmap? {
        view = params.firstOrNull()

        if (priorityImgFile != null && priorityImgFile.exists()) {
            return BitmapFactory.decodeFile(priorityImgFile.absolutePath)
        }

        if (!imgFile.exists()) {
            return null
        }

        return BitmapFactory.decodeFile(imgFile.absolutePath)
    }

    override fun onPostExecute(result: Bitmap?) {
        view?.setImageBitmap(result)
    }
}