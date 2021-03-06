package me.sedlar.calibreviewer

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.sedlar.calibre.opds.local.OPDSLibrary
import me.sedlar.calibre.opds.local.OPDSSeries
import me.sedlar.calibre.opds.model.OPDSSeriesEntry
import me.sedlar.calibreviewer.adapter.SeriesListRecyclerViewAdapter
import me.sedlar.calibreviewer.holder.SeriesHolder
import me.sedlar.calibreviewer.task.AcquisitionDownloadTask
import me.sedlar.calibreviewer.task.SeriesParseTask
import me.sedlar.calibreviewer.util.isComicOrManga
import kotlin.math.ceil


class EntryListActivity : AppCompatActivity() {

    private var library: OPDSLibrary? = null
    private var series: OPDSSeries? = null
    private var libGrid: RecyclerView? = null
    private var libGridAdapter: SeriesListRecyclerViewAdapter? = null

    private var refreshedThumbs = false

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        intent?.extras?.get("lib")?.let { library = it as OPDSLibrary }
        intent?.extras?.get("series")?.let { bundledSeries ->
            series = bundledSeries as OPDSSeries
        }

        intent?.extras?.get("refreshedThumbs")?.let { refreshedThumbs = it as Boolean }

        supportActionBar?.title = series?.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        registerReceiver(
            AcquisitionDownloadTask.RECEIVER,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )

        setContentView(R.layout.activity_series_entries)

        SeriesParseTask(this, onFinish = {
            setupSeriesList()
        }).execute(SeriesHolder(library!!, series!!))
    }

    override fun onDestroy() {
        unregisterReceiver(AcquisitionDownloadTask.RECEIVER)
        super.onDestroy()
    }

    override fun onBackPressed() {
        var override = false

        libGridAdapter?.let { adapter ->
            override = adapter.onBack()
        }

        if (!override) {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (libGridAdapter == null || !libGridAdapter!!.createMenu(menu)) {
            menuInflater.inflate(R.menu.toolbar_series, menu)
        }

        // Add sync cover handler
        menu?.findItem(R.id.action_sync_covers)?.let { menuItem ->
            menuItem.setOnMenuItemClickListener {
                handleCoverRefresh()
                true
            }
        }

        // Add change cover handler
        menu?.findItem(R.id.action_change_series_cover)?.let { menuItem ->
            menuItem.setOnMenuItemClickListener {
                println("TODO: change cover image picker")
                true
            }
        }

        // Add show titles handler
        menu?.findItem(R.id.action_show_titles)?.let { menuItem ->
            menuItem.isChecked = isShowingTitles()
            menuItem.setOnMenuItemClickListener {
                it.isChecked = !it.isChecked
                setTitleVisibility(it.isChecked)
                true
            }
        }

        // Add show read handler
        menu?.findItem(R.id.action_show_read)?.let { menuItem ->
            menuItem.isChecked = isShowingRead()
            menuItem.setOnMenuItemClickListener {
                it.isChecked = !it.isChecked
                setShowRead(it.isChecked)
                libGrid?.redraw()
                true
            }
        }

        // Add grid handler
        menu?.findItem(R.id.action_use_grid)?.let { menuItem ->
            menuItem.isChecked = isGridAlways()
            menuItem.setOnMenuItemClickListener { gridMenuItem ->
                gridMenuItem.isChecked = !gridMenuItem.isChecked
                if (gridMenuItem.isChecked) {
                    menu.findItem(R.id.action_use_list)?.isChecked = false
                    setListAlways(false)
                }
                setGridAlways(gridMenuItem.isChecked)
                restartActivity()
                true
            }
        }

        // Add grid handler
        menu?.findItem(R.id.action_use_list)?.let { menuItem ->
            menuItem.isChecked = isListAlways()
            menuItem.setOnMenuItemClickListener { listMenuItem ->
                listMenuItem.isChecked = !listMenuItem.isChecked
                if (listMenuItem.isChecked) {
                    menu.findItem(R.id.action_use_grid)?.isChecked = false
                    setGridAlways(false)
                }
                setListAlways(listMenuItem.isChecked)
                restartActivity()
                true
            }
        }

        // Add external reader handler
        menu?.findItem(R.id.action_external_reader)?.let { menuItem ->
            menuItem.isChecked = isExternalReader()
            menuItem.setOnMenuItemClickListener {
                it.isChecked = !it.isChecked
                setExternalReader(it.isChecked)
                true
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (libGridAdapter != null && libGridAdapter!!.isSelectMode()) {
                onBackPressed()
            } else {
                finish() // Go back to MainActivity
                if (refreshedThumbs) {
                    restartMainActivity()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupSeriesList() {
        // Remove progress text
        findViewById<TextView>(R.id.lblProgressPercent)?.let {
            (it.parent as ViewGroup).removeView(it)
        }

        // Remove progress indicator
        findViewById<ProgressBar>(R.id.progressIndicator)?.let {
            (it.parent as ViewGroup).removeView(it)
        }

        // Remove progress label
        findViewById<TextView>(R.id.lblProgress)?.let {
            (it.parent as ViewGroup).removeView(it)
        }

        // Add grid view of library
        findViewById<RecyclerView>(R.id.gridSeriesEntries)?.let { grid ->
            libGrid = grid
            grid.visibility = View.VISIBLE

            var gridMode = series!!.entries.size <= MIN_LIST_VIEW_COUNT

            if (series!!.isComicOrManga()) {
                gridMode = false
            }

            if (isGridAlways()) {
                gridMode = true
            }

            if (isListAlways()) {
                gridMode = false
            }

            if (gridMode) {
                grid.layoutManager = GridLayoutManager(this, calculateNoOfColumns(130F))
            } else {
                grid.layoutManager = LinearLayoutManager(this)
            }

            libGridAdapter = SeriesListRecyclerViewAdapter(
                this,
                grid,
                SeriesHolder(library!!, series!!),
                gridView = gridMode
            )

            grid.adapter = libGridAdapter

            libGridAdapter?.scrollToUnread(false)
        }
    }

    private fun handleCoverRefresh() {
        if (hasNetworkConnection()) {
            println("Handling cover refresh...")

            // Delete thumbnail (covers the user is seeing)
            series!!.entries.forEach { entry ->
                library!!.getThumbFile(series!!, entry).let { coverFile ->
                    if (coverFile.exists()) {
                        println("Deleting cover @ ${series!!.name} - ${entry.title}")
                        coverFile.delete()
                    }
                }
            }

            restartActivity()
        } else {
            Toast.makeText(this, "No connection available...", Toast.LENGTH_LONG).show()
        }
    }

    fun isShowingTitles(): Boolean {
        return sharedPrefs.getBoolean(KEY_SHOW_TITLES, true)
    }

    private fun setTitleVisibility(visible: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_SHOW_TITLES, visible).apply()
        libGrid?.redraw()
    }

    fun isExternalReader(): Boolean {
        return sharedPrefs.getBoolean(KEY_EXTERNAL_READER, false)
    }

    private fun setExternalReader(external: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_EXTERNAL_READER, external).apply()
    }

    fun isShowingRead(): Boolean {
        return sharedPrefs.getBoolean(KEY_SHOW_READ, true)
    }

    private fun setShowRead(external: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_SHOW_READ, external).apply()
    }

    fun isGridAlways(): Boolean {
        return sharedPrefs.getBoolean(KEY_GRID_ALWAYS, false)
    }

    private fun setGridAlways(always: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_GRID_ALWAYS, always).apply()
    }

    fun isListAlways(): Boolean {
        return sharedPrefs.getBoolean(KEY_LIST_ALWAYS, false)
    }

    private fun setListAlways(always: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_LIST_ALWAYS, always).apply()
    }

    fun markRead(vararg entries: OPDSSeriesEntry) {
        val currEntries = HashSet<String>()
        if (sharedPrefs.contains(KEY_READ_ENTRIES)) {
            currEntries.addAll(
                sharedPrefs.getStringSet(KEY_READ_ENTRIES, emptySet())!!.toTypedArray()
            )
        }
        entries.forEach { currEntries.add(it.uuid) }
        sharedPrefs.edit().putStringSet(KEY_READ_ENTRIES, currEntries).apply()
    }

    fun markUnread(vararg entries: OPDSSeriesEntry) {
        val currEntries = HashSet<String>()
        if (sharedPrefs.contains(KEY_READ_ENTRIES)) {
            currEntries.addAll(
                sharedPrefs.getStringSet(KEY_READ_ENTRIES, emptySet())!!.toTypedArray()
            )
        }
        entries.forEach { currEntries.remove(it.uuid) }
        sharedPrefs.edit().putStringSet(KEY_READ_ENTRIES, currEntries).apply()
    }

    fun isEntryRead(entry: OPDSSeriesEntry): Boolean {
        if (sharedPrefs.contains(KEY_READ_ENTRIES)) {
            return sharedPrefs.getStringSet(KEY_READ_ENTRIES, emptySet())!!.contains(entry.uuid)
        }
        return false
    }

    fun getReadEntryIds(): Set<String> {
        return sharedPrefs.getStringSet(KEY_READ_ENTRIES, emptySet())!!
    }

    fun getReadEntries(series: OPDSSeries): List<OPDSSeriesEntry> {
        val readIds = getReadEntryIds()
        return series.entries.filter { readIds.contains(it.uuid) }
    }

    fun getUnreadEntries(series: OPDSSeries): List<OPDSSeriesEntry> {
        val readIds = getReadEntryIds()
        return series.entries.filter { !readIds.contains(it.uuid) }
    }

    fun setProgressLabel(text: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.lblProgress)?.let {
                it.text = text
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun setProgressPercent(percent: Double) {
        runOnUiThread {
            // Set progress text
            findViewById<TextView>(R.id.lblProgressPercent)?.let {
                it.text = "${ceil(percent).toInt()}%"
            }
            // Set bar progress
            findViewById<ProgressBar>(R.id.progressIndicator)?.setProgress(
                ceil(percent).toInt(),
                true
            )
        }
    }

    private fun restartActivity(refreshedThumbs: Boolean = true) {
        // Dispose of current activity
        finish()
        // Restart activity with 1x network
        val restartIntent = Intent(applicationContext, EntryListActivity::class.java)
        restartIntent.putExtra("lib", library)
        restartIntent.putExtra("series", series)
        restartIntent.putExtra("refreshedThumbs", refreshedThumbs)
        startActivity(restartIntent)
    }
}