package me.sedlar.calibreviewer.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import me.sedlar.calibre.opds.OPDSConnector
import me.sedlar.calibre.opds.model.OPDSAcquisition
import me.sedlar.calibre.opds.model.OPDSSeriesEntry
import me.sedlar.calibreviewer.BuildConfig
import me.sedlar.calibreviewer.EntryListActivity
import me.sedlar.calibreviewer.R
import me.sedlar.calibreviewer.hasNetworkConnection
import me.sedlar.calibreviewer.holder.SeriesHolder
import me.sedlar.calibreviewer.task.AcquisitionDownloadTask
import me.sedlar.calibreviewer.task.ImageDecodeTask
import java.io.File


class SeriesListRecyclerViewAdapter(private val activity: EntryListActivity, private val holder: SeriesHolder) :
    RecyclerView.Adapter<SeriesListRecyclerViewAdapter.ItemViewHolder>() {

    init {
        println("SeriesListRecyclerViewAdapter[size=${itemCount}]")
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ItemViewHolder {
        val view: View = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_series_entry, viewGroup, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        val entry = holder.series.entries[position]
        val thumbnail = holder.lib.getThumbFile(holder.series, entry)
        val userDefinedThumbnail = File(thumbnail, "../cover.jpg")

        ImageDecodeTask(thumbnail, userDefinedThumbnail).execute(viewHolder.imgCover)

        viewHolder.lblEntryTitle?.text = toTitleName(entry.title)
        viewHolder.lblEntryTitle?.visibility = if (activity.isShowingTitles()) View.VISIBLE else View.GONE

        viewHolder.itemView.setOnClickListener { view ->
            val bookDialog: AlertDialog.Builder = AlertDialog.Builder(view.context)
            bookDialog.setTitle(entry.title)

            val actions = generateActionList(entry)

            val arrayAdapter = ArrayAdapter<String>(
                view.context, android.R.layout.select_dialog_item, actions.toTypedArray()
            )

            bookDialog.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

            bookDialog.setAdapter(arrayAdapter) { dialog, which ->
                arrayAdapter.getItem(which)?.let { selection ->
                    when (selection.toLowerCase()) {
                        "download epub" -> handleAcquisitionDownload(entry, "epub")
                        "open epub" -> handleAcquisitionOpen(entry, "epub")
                        "delete epub" -> handleAcquisitionDelete(entry, "epub")

                        "download zip" -> handleAcquisitionDownload(entry, "zip")
                        "open zip" -> handleAcquisitionOpen(entry, "zip")
                        "delete zip" -> handleAcquisitionDelete(entry, "zip")


                        else -> {
                            Toast.makeText(view.context, "Unsupported action: $selection", Toast.LENGTH_LONG)
                        }
                    }
                }

                dialog.dismiss()
            }

            bookDialog.show()
        }
    }

    override fun getItemCount(): Int {
        return holder.series.entries.size
    }

    private fun generateActionList(entry: OPDSSeriesEntry): List<String> {
        val actions = ArrayList<String>()

        entry.acquisitions.forEach { acquisition ->
            val targetFile = holder.lib.getAcquisitionFile(holder.series, entry, acquisition)
            if (targetFile.exists()) {
                actions.add("Open ${acquisition.fileExtension}")
                actions.add("Delete ${acquisition.fileExtension}")
            } else {
                actions.add("Download ${acquisition.fileExtension}")
            }
        }

        return actions
    }

    private fun doToast(text: String, duration: Int) {
        Toast.makeText(activity.applicationContext, text, duration).show()
    }

    private fun handleAcquisitionDownload(entry: OPDSSeriesEntry, fileExtension: String) {
        entry.acquisitions.firstOrNull { it.fileExtension == fileExtension }?.let { acquisition ->
            downloadAcquisition(entry, acquisition)
        }
    }

    private fun handleAcquisitionOpen(entry: OPDSSeriesEntry, fileExtension: String) {
        entry.acquisitions.firstOrNull { it.fileExtension == fileExtension }?.let { acquisition ->
            holder.lib.getAcquisitionFile(holder.series, entry, acquisition).let { file ->
                val fileURI = FileProvider.getUriForFile(
                    activity.applicationContext,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
                )

                val shareIntent = Intent()

                shareIntent.action = Intent.ACTION_VIEW
                shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                shareIntent.setDataAndType(fileURI, acquisition.type)
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileURI)

                activity.startActivity(Intent.createChooser(shareIntent, "Open eBook"))
            }
        }
    }

    private fun handleAcquisitionDelete(entry: OPDSSeriesEntry, fileExtension: String) {
        entry.acquisitions.firstOrNull { it.fileExtension == fileExtension }?.let { acquisition ->
            holder.lib.getAcquisitionFile(holder.series, entry, acquisition).let { file ->
                if (file.exists()) {
                    doToast("Deleting $fileExtension...", Toast.LENGTH_LONG)
                    if (file.delete()) {
                        doToast("Deleted $fileExtension!", Toast.LENGTH_SHORT)
                    } else {
                        doToast("Failed to delete $fileExtension...", Toast.LENGTH_SHORT)
                    }
                }
            }
        }
    }

    private fun downloadAcquisition(entry: OPDSSeriesEntry, acquisition: OPDSAcquisition) {
        doToast("Downloading epub...", Toast.LENGTH_LONG)

        val checker = OPDSConnector.checker

        OPDSConnector.checker = { activity.hasNetworkConnection() }

        AcquisitionDownloadTask(holder, entry, onFinish = { successful ->
            OPDSConnector.checker = checker
            if (successful) {
                doToast("Downloaded epub!", Toast.LENGTH_SHORT)
            } else {
                if (activity.hasNetworkConnection()) {
                    doToast("Failed to download...", Toast.LENGTH_SHORT)
                } else {
                    doToast("Connection unavailable...", Toast.LENGTH_SHORT)
                }
            }
        }).execute(acquisition)
    }

    inner class ItemViewHolder(itemView: View) : ViewHolder(itemView) {

        val imgCover: ImageView? = itemView.findViewById(R.id.imgCover)
        val lblEntryTitle: TextView? = itemView.findViewById(R.id.lblEntryTitle)
    }
}