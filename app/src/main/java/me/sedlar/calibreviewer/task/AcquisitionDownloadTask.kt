package me.sedlar.calibreviewer.task

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.util.LongSparseArray
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.containsKey
import androidx.core.util.set
import me.sedlar.calibre.opds.model.OPDSAcquisition
import me.sedlar.calibre.opds.model.OPDSSeriesEntry
import me.sedlar.calibreviewer.holder.SeriesHolder

typealias AcquisitionCallback = (Boolean) -> Unit

class AcquisitionDownloadTask(
    private val activity: AppCompatActivity,
    private val holder: SeriesHolder,
    private val entry: OPDSSeriesEntry,
    private val onFinish: AcquisitionCallback? = null
) : AsyncTask<OPDSAcquisition, Void, Boolean>() {

    companion object {
        internal val DOWNLOAD_MAP = LongSparseArray<AcquisitionDownloadTask>()

        internal val RECEIVER = object : BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)?.let { downloadId ->
                    if (DOWNLOAD_MAP.containsKey(downloadId)) {
                        val task = DOWNLOAD_MAP[downloadId]
                        val query = DownloadManager.Query()
                        query.setFilterById(downloadId)
                        val cursor = task.downloadManager.query(query)
                        if (cursor.moveToFirst()) {
                            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                            val status = cursor.getInt(columnIndex)
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                task.onFinish?.let { it(true) }
                                DOWNLOAD_MAP.delete(downloadId)
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                task.onFinish?.let { it(false) }
                                DOWNLOAD_MAP.delete(downloadId)
                            }
                        }
                    }
                }
            }
        }
    }

    private val downloadManager = activity.getSystemService(DOWNLOAD_SERVICE) as DownloadManager

    override fun doInBackground(vararg params: OPDSAcquisition?): Boolean {
        params.firstOrNull()?.let { acquisition ->
            val uri = holder.lib.getAcquisitionURL(acquisition)
            val downloadRequest = DownloadManager.Request(Uri.parse(uri))

            downloadRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            downloadRequest.setAllowedOverRoaming(true)

            downloadRequest.setTitle(holder.series.name + " - " + entry.title)
//            downloadRequest.setDescription(holder.series.name + " - " + entry.title)

            holder.lib.createAuthHeader(uri)?.let { authHeader ->
                downloadRequest.addRequestHeader("Authorization", authHeader)
            }

            downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            val targetFile = holder.lib.getAcquisitionFile(holder.series, entry, acquisition)
            targetFile.parentFile?.mkdirs()
            val subPath = targetFile.absolutePath.replace(holder.lib.dataDir.parentFile?.absolutePath ?: "", "")

            downloadRequest.setDestinationInExternalFilesDir(activity, null, subPath)
            DOWNLOAD_MAP[downloadManager.enqueue(downloadRequest)] = this

            true
        }
        return false
    }
}