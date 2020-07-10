package me.sedlar.calibreviewer

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
import androidx.recyclerview.widget.RecyclerView
import me.sedlar.calibre.opds.local.OPDSLibrary
import me.sedlar.calibre.opds.local.OPDSSeries
import me.sedlar.calibreviewer.adapter.SeriesListRecyclerViewAdapter
import me.sedlar.calibreviewer.holder.SeriesHolder
import me.sedlar.calibreviewer.task.AcquisitionDownloadTask
import me.sedlar.calibreviewer.task.SeriesParseTask


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

        registerReceiver(AcquisitionDownloadTask.RECEIVER, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

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
            grid.layoutManager = GridLayoutManager(this, calculateNoOfColumns(130F))

            libGridAdapter = SeriesListRecyclerViewAdapter(this, grid, SeriesHolder(library!!, series!!))
            grid.adapter = libGridAdapter
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

    fun setProgressLabel(text: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.lblProgress)?.let {
                it.text = text
            }
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