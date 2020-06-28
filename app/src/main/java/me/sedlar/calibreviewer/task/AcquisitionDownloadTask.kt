package me.sedlar.calibreviewer.task

import android.os.AsyncTask
import me.sedlar.calibre.opds.model.OPDSAcquisition
import me.sedlar.calibre.opds.model.OPDSSeriesEntry
import me.sedlar.calibreviewer.holder.SeriesHolder

typealias AcquisitionCallback = (Boolean) -> Unit

class AcquisitionDownloadTask(
    private val holder: SeriesHolder,
    private val entry: OPDSSeriesEntry,
    private val onFinish: AcquisitionCallback? = null
) :
    AsyncTask<OPDSAcquisition, Void, Boolean>() {

    override fun doInBackground(vararg params: OPDSAcquisition?): Boolean {
        params.firstOrNull()?.let { acquisition ->
            return holder.lib.downloadAcquisition(holder.series, entry, acquisition)
        }
        return false
    }

    override fun onPostExecute(result: Boolean) {
        super.onPostExecute(result)
        onFinish?.let { it(result) }
    }
}