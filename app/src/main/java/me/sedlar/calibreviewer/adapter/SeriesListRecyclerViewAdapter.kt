package me.sedlar.calibreviewer.adapter

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.folioreader.Config
import com.folioreader.FolioReader
import com.folioreader.model.locators.ReadLocator
import com.folioreader.util.AppUtil
import me.sedlar.calibre.opds.OPDSConnector
import me.sedlar.calibre.opds.model.OPDSAcquisition
import me.sedlar.calibre.opds.model.OPDSSeriesEntry
import me.sedlar.calibreviewer.*
import me.sedlar.calibreviewer.holder.SeriesHolder
import me.sedlar.calibreviewer.task.AcquisitionDownloadTask
import me.sedlar.calibreviewer.task.ImageDecodeTask
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.collections.ArrayList


class SeriesListRecyclerViewAdapter(
    private val activity: EntryListActivity,
    private val parent: RecyclerView,
    private val holder: SeriesHolder
) : RecyclerView.Adapter<SeriesListRecyclerViewAdapter.ItemViewHolder>() {

    private var selectMode = false
    private val selected = ArrayList<OPDSSeriesEntry>()

    private val reader = FolioReader.get()

    private var lastSelectedEntry: OPDSSeriesEntry? = null

    private val filteredEntries: List<OPDSSeriesEntry>
        get() {
            return if (activity.isShowingRead()) {
                holder.series.entries
            } else {
                activity.getUnreadEntries(holder.series)
            }
        }

    private fun getEntryLocationFile(entry: OPDSSeriesEntry): File {
        return File(holder.lib.dataDir, "locations/${entry.uuid}.json")
    }

    init {
        println("SeriesListRecyclerViewAdapter[size=${itemCount}]")

        // Setup location saver
        reader.setReadLocatorListener { locator ->
            lastSelectedEntry?.let { entry ->
                val locFile = getEntryLocationFile(entry)
                val locatorJson = locator.toJson()!!
                locFile.parentFile?.mkdirs()
                Files.write(locFile.toPath(), locatorJson.toByteArray())
                println("saveReadLocator -> $locatorJson")
            }
        }
    }

    fun onBack(): Boolean {
        println("onBack called..")
        if (selectMode) {
            clearSelections()
            println("Canceled series entry multiselect")
            return true
        }
        return false
    }

    fun createMenu(menu: Menu?): Boolean {
        if (selectMode) {
            activity.menuInflater.inflate(R.menu.toolbar_series_multiselect, menu)

            // Add download handler
            menu?.findItem(R.id.action_download)?.let { menuItem ->
                menuItem.setOnMenuItemClickListener {
                    downloadSelections()

                    true
                }
            }

            // Add delete handler
            menu?.findItem(R.id.action_delete)?.let { menuItem ->
                menuItem.setOnMenuItemClickListener {
                    deleteSelections()

                    true
                }
            }

            // Add mark read handler
            menu?.findItem(R.id.action_mark_read)?.let { menuItem ->
                menuItem.setOnMenuItemClickListener {
                    markSelectionsRead()
                    true
                }
            }

            // Add mark unread handler
            menu?.findItem(R.id.action_mark_unread)?.let { menuItem ->
                menuItem.setOnMenuItemClickListener {
                    markSelectionsUnread()
                    true
                }
            }

            // Add select handler
            menu?.findItem(R.id.action_select_all)?.let { menuItem ->
                menuItem.setOnMenuItemClickListener {
                    selected.clear()
                    selected.addAll(filteredEntries)

                    parent.redraw {
                        scrollToUnread()
                    }

                    true
                }
            }

            // Add select before handler
            menu?.findItem(R.id.action_select_before)?.let { menuItem ->
                menuItem.setOnMenuItemClickListener {
                    selectBefore()
                    true
                }
            }

            // Add select after handler
            menu?.findItem(R.id.action_select_after)?.let { menuItem ->
                menuItem.setOnMenuItemClickListener {
                    selectAfter()
                    true
                }
            }

            // Add cancel handler
            menu?.findItem(R.id.action_cancel)?.let { menuItem ->
                menuItem.setOnMenuItemClickListener {
                    clearSelections(false)
                    true
                }
            }

            return true
        }
        return false
    }

    fun isSelectMode(): Boolean {
        return selectMode
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, type: Int): ItemViewHolder {
        val view: View = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_series_entry, viewGroup, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        val entry = filteredEntries[position]
        val thumbnail = holder.lib.getThumbFile(holder.series, entry)
        val userDefinedThumbnail = File(thumbnail, "../cover.jpg")

        ImageDecodeTask(thumbnail, userDefinedThumbnail).execute(viewHolder.imgCover)

        viewHolder.lblEntryTitle?.text = toTitleName(entry.title)
        viewHolder.lblEntryTitle?.visibility =
            if (activity.isShowingTitles()) View.VISIBLE else View.GONE

        if (selectMode && selected.contains(entry)) {
            viewHolder.selectBackground?.visibility = View.VISIBLE
            viewHolder.selectBackground?.setCardBackgroundColor(Color.parseColor("#4DFFFFFF"))
        } else if (activity.isEntryRead(entry)) {
            viewHolder.selectBackground?.visibility = View.VISIBLE
            viewHolder.selectBackground?.setCardBackgroundColor(Color.parseColor("#99131313"))
        } else {
            viewHolder.selectBackground?.visibility = View.GONE
        }

        viewHolder.itemView.setOnLongClickListener {
            println("Enabled multiselect mode")

            selectMode = true
            activity.invalidateOptionsMenu()

            handleSelection(entry, viewHolder)

            true
        }

        viewHolder.itemView.setOnClickListener { view ->
            if (selectMode) {
                handleSelection(entry, viewHolder)
            } else {
                val bookDialog: AlertDialog.Builder = AlertDialog.Builder(view.context)
                bookDialog.setTitle(entry.title)

                val actions = generateActionList(entry)

                val arrayAdapter = ArrayAdapter<String>(
                    view.context, android.R.layout.select_dialog_item, actions.toTypedArray()
                )

                bookDialog.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

                bookDialog.setAdapter(arrayAdapter) { dialog, which ->
                    arrayAdapter.getItem(which)?.let { selection ->
                        when (selection.toLowerCase(Locale.getDefault())) {
                            "download epub" -> handleAcquisitionDownload(entry, "epub")
                            "open epub" -> handleAcquisitionOpen(entry, "epub")
                            "delete epub" -> handleAcquisitionDelete(entry, "epub")

                            "download zip" -> handleAcquisitionDownload(entry, "zip")
                            "open zip" -> handleAcquisitionOpen(entry, "zip")
                            "delete zip" -> handleAcquisitionDelete(entry, "zip")


                            else -> {
                                Toast.makeText(
                                    view.context,
                                    "Unsupported action: $selection",
                                    Toast.LENGTH_LONG
                                )
                            }
                        }
                    }

                    dialog.dismiss()
                }

                bookDialog.show()
            }
        }
    }

    override fun getItemCount(): Int {
        return filteredEntries.size
    }

    private fun downloadSelections() {
        clearSelections(true)

        var dlCount = 0

        selected.forEach { entry ->
            entry.acquisitions.forEach { acquisition ->
                holder.lib.getAcquisitionFile(holder.series, entry, acquisition)
                    .let { acqFile ->
                        if (!acqFile.exists()) {
                            dlCount++
                            handleAcquisitionDownload(entry, acquisition.fileExtension)
                        }
                    }
            }
        }

        if (dlCount > 0) {
            doToast("Starting downloads...", Toast.LENGTH_LONG)
        } else {
            doToast("Files already downloaded!", Toast.LENGTH_SHORT)
        }

        selected.clear()
    }

    private fun deleteSelections() {
        doToast("Deleting selections...", Toast.LENGTH_LONG)

        clearSelections(true)

        selected.forEach { entry ->
            entry.acquisitions.forEach { acquisition ->
                holder.lib.getAcquisitionFile(holder.series, entry, acquisition)
                    .let { acqFile ->
                        if (acqFile.exists()) {
                            acqFile.delete()
                        }
                    }
            }
        }

        selected.clear()

        doToast("Selections deleted!", Toast.LENGTH_SHORT)
    }

    private fun markSelectionsRead() {
        activity.markRead(*selected.toTypedArray())
        clearSelections()
    }

    private fun markSelectionsUnread() {
        activity.markUnread(*selected.toTypedArray())
        clearSelections()
    }

    private fun clearSelections(visualOnly: Boolean = false) {
        selectMode = false
        if (!visualOnly) {
            selected.clear()
        }

        parent.redraw {
            scrollToUnread()
        }

        activity.invalidateOptionsMenu()
    }

    private fun handleSelection(entry: OPDSSeriesEntry, viewHolder: ItemViewHolder) {
        if (selected.contains(entry)) {
            selected.remove(entry)
        } else {
            selected.add(entry)
        }

        viewHolder.selectBackground?.visibility =
            if (selectMode && selected.contains(entry)) View.VISIBLE else View.GONE
        viewHolder.selectBackground?.setCardBackgroundColor(Color.parseColor("#4DFFFFFF"))
    }

    private fun selectBefore() {
        val lastSelected =
            selected.sortedBy { filteredEntries.indexOf(it) }.lastOrNull()
        if (lastSelected != null) {
            val lastSelectedIndex = filteredEntries.indexOf(lastSelected)
            selected.clear()
            selected.addAll(filteredEntries.subList(0, lastSelectedIndex + 1))

            parent.redraw {
                parent.scrollToPosition(lastSelectedIndex)
            }
        }
    }

    private fun selectAfter() {
        val lastSelected =
            selected.sortedBy { filteredEntries.indexOf(it) }.lastOrNull()
        if (lastSelected != null) {
            val lastSelectedIndex = filteredEntries.indexOf(lastSelected)
            selected.clear()
            selected.addAll(
                filteredEntries.subList(
                    lastSelectedIndex,
                    filteredEntries.size
                )
            )
            parent.redraw {
                parent.scrollToPosition(filteredEntries.size)
            }
        }
    }

    fun getUnreadPosition(): Int? {
        activity.getUnreadEntries(holder.series).firstOrNull()?.let { firstUnread ->
            return filteredEntries.indexOf(firstUnread)
        }
        return null
    }

    fun scrollToUnread(smooth: Boolean = true) {
        getUnreadPosition()?.let {
            if (smooth) {
                parent.smoothScrollToPosition(it)
            } else {
                parent.scrollToPosition(it)
            }
        }
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
                lastSelectedEntry = entry

                val internalButNotEpub = !activity.isExternalReader() && file.extension != "epub"

                if (internalButNotEpub) {
                    doToast("Internal reader supports epub only", Toast.LENGTH_SHORT)
                }

                if (activity.isExternalReader() || internalButNotEpub) {
                    println("Using external reader...")

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
                } else {
                    println("Using internal reader...")

                    val locFile = getEntryLocationFile(entry)

                    if (locFile.exists()) {
                        println("Using previous location...")
                        reader.setReadLocator(ReadLocator.fromJson(locFile.readText()))
                    } else {
                        println("Location marker does not exist: ${locFile.absolutePath}")
                    }

                    var config = AppUtil.getSavedConfig(activity.applicationContext)

                    if (config == null) {
                        config = Config()
                        config.isNightMode = true
                        config.fontSize = 2
                    }

                    config.setThemeColorInt(Color.parseColor("#448AFF"))
                    config.setNightThemeColorInt(Color.parseColor("#FFFFFF"))
                    config.allowedDirection = Config.AllowedDirection.VERTICAL_AND_HORIZONTAL
                    config.isShowRemainingIndicator = true

                    reader
                        .setConfig(config, true)
                        .openBook(file.absolutePath)
                }
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
        val title = "${holder.series.name} - ${entry.title} (${acquisition.fileExtension})"

        println("Downloading $title")
        doToast("Downloading $title...", Toast.LENGTH_LONG)

        val checker = OPDSConnector.checker

        OPDSConnector.checker = { activity.hasNetworkConnection() }

        AcquisitionDownloadTask(activity, holder, entry, onFinish = { successful ->
            OPDSConnector.checker = checker
            if (successful) {
                println("Downloaded $title!")
                doToast("Downloaded $title!", Toast.LENGTH_SHORT)
            } else {
                holder.lib.getAcquisitionFile(holder.series, entry, acquisition).let { acqFile ->
                    if (acqFile.exists()) {
                        acqFile.delete()
                    }
                }
                if (activity.hasNetworkConnection()) {
                    println("Failed to download $title...")
                    doToast("Failed to download $title...", Toast.LENGTH_SHORT)
                } else {
                    doToast("Connection unavailable...", Toast.LENGTH_SHORT)
                }
            }
        }).execute(acquisition)
    }

    inner class ItemViewHolder(itemView: View) : ViewHolder(itemView) {

        val selectBackground: CardView? = itemView.findViewById(R.id.selectBackground)
        val imgCover: ImageView? = itemView.findViewById(R.id.imgCover)
        val lblEntryTitle: TextView? = itemView.findViewById(R.id.lblEntryTitle)
    }
}