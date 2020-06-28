package me.sedlar.calibreviewer

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.sedlar.calibre.opds.local.OPDSLibrary
import me.sedlar.calibre.opds.local.OPDSSeries
import me.sedlar.calibreviewer.adapter.SeriesListRecyclerViewAdapter
import me.sedlar.calibreviewer.holder.SeriesHolder
import me.sedlar.calibreviewer.task.SeriesParseTask


class EntryListActivity : AppCompatActivity() {

    private var library: OPDSLibrary? = null
    private var series: OPDSSeries? = null
    private var libGrid: RecyclerView? = null

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)

        intent?.extras?.get("lib")?.let { library = it as OPDSLibrary }
        intent?.extras?.get("series")?.let { bundledSeries ->
            series = bundledSeries as OPDSSeries
        }

        supportActionBar?.title = series?.name
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        setContentView(R.layout.activity_series_entries)

        SeriesParseTask(this, onFinish = {
            setupSeriesList()
        }).execute(SeriesHolder(library!!, series!!))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_series, menu)

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

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish() // Go back to MainActivity
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

            val recyclerViewAdapter = SeriesListRecyclerViewAdapter(this, SeriesHolder(library!!, series!!))
            grid.adapter = recyclerViewAdapter
        }
    }

    fun isShowingTitles(): Boolean {
        return sharedPrefs.getBoolean(KEY_SHOW_TITLES, true)
    }

    private fun setTitleVisibility(visible: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_SHOW_TITLES, visible).apply()
        libGrid?.adapter = libGrid?.adapter // redraws the items in the grid
    }

    fun setProgressLabel(text: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.lblProgress)?.let {
                it.text = text
            }
        }
    }
}