package me.sedlar.calibreviewer.task

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import me.sedlar.calibre.opds.OPDSConnector
import me.sedlar.calibre.opds.local.OPDSLibrary
import me.sedlar.calibre.opds.local.OPDSSeries
import me.sedlar.calibre.opds.model.OPDSSeriesEntry
import me.sedlar.calibreviewer.EntryListActivity
import me.sedlar.calibreviewer.hasNetworkConnection
import me.sedlar.calibreviewer.holder.SeriesHolder
import me.sedlar.calibreviewer.util.await
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.util.concurrent.Callable

typealias SeriesTaskCallback = () -> Unit

class SeriesParseTask(private val activity: EntryListActivity, private val onFinish: SeriesTaskCallback? = null) :
    AsyncTask<SeriesHolder, Void, Unit>() {

    override fun doInBackground(vararg params: SeriesHolder) {
        params.firstOrNull()?.let { holder ->
            val progressMax = holder.series.entries.size
            var progress = 0

            holder.series.entries.map { entry ->
                Callable {
                    downloadThumbnail(holder.lib, holder.series, entry)
                    progress++
                    activity.setProgressPercent((progress.toDouble() / progressMax.toDouble()) * 100.0)
                }
            }.await(4)
        }
    }

    override fun onPostExecute(result: Unit?) {
        super.onPostExecute(result)
        onFinish?.let { it() }
    }

    private fun downloadThumbnail(lib: OPDSLibrary, series: OPDSSeries, entry: OPDSSeriesEntry) {
        val thumbFile = lib.getThumbFile(series, entry)
        if (!thumbFile.exists()) {
            val checker = OPDSConnector.checker

            OPDSConnector.checker = { activity.hasNetworkConnection() }

            activity.setProgressLabel("Downloading thumbnail... \n\n ${series.name} \n\n ${entry.title}")

            val thumb = lib.getThumbURL(entry.cover)

            lib.downloadAsBytes(thumb)?.let { thumbData ->
                activity.setProgressLabel("Resizing thumbnail... \n\n ${series.name} \n\n ${entry.title}")

                ByteArrayInputStream(thumbData).use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 400, 600, false)

                    thumbFile.parentFile?.mkdirs()

                    FileOutputStream(thumbFile).use { output ->
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                    }
                }
            }

            OPDSConnector.checker = checker
        }
    }
}