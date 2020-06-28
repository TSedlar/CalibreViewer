package me.sedlar.calibreviewer

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.sedlar.calibre.opds.OPDSConnector
import me.sedlar.calibre.opds.local.OPDSLibrary
import me.sedlar.calibreviewer.adapter.SeriesRecyclerViewAdapter
import me.sedlar.calibreviewer.task.LibraryParseTask
import java.io.File


internal const val KEY_IP = "KEY_IP"
internal const val KEY_PORT = "KEY_PORT"
internal const val KEY_USER = "KEY_USER"
internal const val KEY_PASS = "KEY_PASS"
internal const val KEY_LAST_LIB = "KEY_LAST_LIB"
internal const val KEY_CACHE_MODE = "KEY_CACHE_MODE"
internal const val KEY_SHOW_TITLES = "KEY_HIDE_TITLES"


class MainActivity : AppCompatActivity() {

    private var dataDirectory: File? = null
    private val libraries = ArrayList<OPDSLibrary>()
    private var library: Int? = null

    private var libSpinnerItem: MenuItem? = null
    private var libSpinner: Spinner? = null
    private var libOptionsItem: MenuItem? = null

    private var libGrid: RecyclerView? = null

    private var libNameMap = HashMap<String, String>()

    private val currentLib: OPDSLibrary?
        get() = libraries[if (library == null) 0 else library!!]

    init {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataDirectory = File(applicationContext.applicationInfo.dataDir)
        requestWritePermissions()

        val forceNetwork = intent.getBooleanExtra("forceNetwork", false)

        if (checkSettings()) {
            setContentView(R.layout.activity_main)
            supportActionBar?.title = "Library"
            setDefaultLibrary()
            parseLibraries(forceNetwork)
        } else {
            supportActionBar?.title = "Server Settings"
            setContentView(R.layout.activity_server_settings)
            setupServerSettings()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_lib, menu)

        menu?.findItem(R.id.spinLibraries)?.let { spinnerItem ->
            libSpinnerItem = spinnerItem
            libSpinner = spinnerItem.actionView as Spinner
            libSpinner?.visibility = View.GONE
        }

        menu?.findItem(R.id.menuLibOptions)?.let { libOptions ->
            libOptionsItem = libOptions

            // Add sync handler
            menu.findItem(R.id.action_sync)?.let { menuItem ->
                menuItem.setOnMenuItemClickListener {
                    restartApp(true) // force network on for syncing
                    true
                }
            }

            // Add show title handler
            menu.findItem(R.id.action_show_titles)?.let { menuItem ->
                menuItem.isChecked = isShowingTitles()
                menuItem.setOnMenuItemClickListener {
                    it.isChecked = !it.isChecked
                    setTitleVisibility(it.isChecked)
                    true
                }
            }

            // Add cache mode handler
            menu.findItem(R.id.action_cache_mode)?.let { menuItem ->
                menuItem.isChecked = isCacheModeViaPrefs()
                menuItem.setOnMenuItemClickListener {
                    it.isChecked = !it.isChecked
                    setCacheMode(it.isChecked)
                    true
                }
            }

            // Add change server handler
            menu.findItem(R.id.action_change_server)?.let { menuItem ->
                menuItem.setOnMenuItemClickListener {
                    clearServerPreferences()
                    restartApp()
                    true
                }
            }
        }
        return true
    }

    private fun parseLibraries(forceNetwork: Boolean = false) {
        println("Parsing library...")
        OPDSConnector.checker = { forceNetwork || (!isCacheMode() && hasNetworkConnection()) }

        val address = getServerAddress()
        val port = getServerPort()
        val dataDir = File(dataDirectory!!, "$address.$port/".replace(".", "_"))

        LibraryParseTask(this, onFinish = { libs ->
            println("Library parsed!")
            libraries.clear()
            libraries.addAll(libs)
            libNameMap.clear()

            libs.forEach { lib ->
                libNameMap[toReadableName(lib.name)] = lib.name
            }

            if (library == null) {
                library = 0
            }

            setupLibrary()
        })
            .execute(
                "http://$address:$port",
                getServerUser(),
                getServerPass(),
                dataDir.absolutePath
            )
    }

    private fun setDefaultLibrary() {
        if (sharedPrefs.contains(KEY_LAST_LIB)) {
            library = try {
                sharedPrefs.getInt(KEY_LAST_LIB, 0)
            } catch (cast: ClassCastException) {
                try {
                    sharedPrefs.getString(KEY_LAST_LIB, null)!!.toInt()
                } catch (err: java.lang.NumberFormatException) {
                    0
                }
            }
            println("Previous Library: $library")
        }
    }

    private fun setupLibrary() {
        supportActionBar?.title = "Library"

        libOptionsItem?.isVisible = true

        if (libraries.isEmpty()) {
            return
        }

        // Setup Library dropdown
        libSpinner?.let { spinner ->
            if (spinner.adapter == null) {
                libSpinnerItem?.isVisible = true

                spinner.adapter = ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_list_item_1,
                    libraries.map { toReadableName(it.name) }
                )

                if (library != null) {
                    println("Setting initial library: ${libraries[library!!].name} [$library]")
                    spinner.setSelection(library!!)
                }

                spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        library = position

                        println("Selected Library: ${libraries[library!!].name} [$library]")
                        sharedPrefs.edit()
                            .putInt(KEY_LAST_LIB, position)
                            .apply()

                        setupLibrary()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
            }
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
        findViewById<RecyclerView>(R.id.gridSeries)?.let { grid ->
            libGrid = grid
            grid.visibility = View.VISIBLE
            grid.layoutManager = GridLayoutManager(this, calculateNoOfColumns(130F))

            val recyclerViewAdapter = SeriesRecyclerViewAdapter(this, currentLib!!)
            grid.adapter = recyclerViewAdapter
        }
    }

    private fun setupServerSettings() {
        findViewById<Button>(R.id.btnSave)?.setOnClickListener {
            val ip = findViewById<EditText>(R.id.txtAddress).text.toString()
            val port = findViewById<EditText>(R.id.txtPort).text.toString()
            val user = findViewById<EditText>(R.id.txtUser).text.toString()
            val pass = findViewById<EditText>(R.id.txtPass).text.toString()

            var isDataValid = true

            val portNumber = try {
                port.toInt()
            } catch (err: NumberFormatException) {
                Toast.makeText(applicationContext, "", Toast.LENGTH_SHORT).show()
                isDataValid = false
                8080
            }

            if (ip.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                isDataValid = false
            }

            if (isDataValid) {
                sharedPrefs.edit()
                    .putString(KEY_IP, ip)
                    .putInt(KEY_PORT, portNumber)
                    .putString(KEY_USER, user)
                    .putString(KEY_PASS, pass)
                    .apply()
                setContentView(R.layout.activity_main)
                parseLibraries()
            }
        }
    }

    private fun getServerAddress(): String {
        return sharedPrefs.getString(KEY_IP, null)!!
    }

    private fun getServerPort(): Int {
        return sharedPrefs.getInt(KEY_PORT, 8080)
    }

    private fun getServerUser(): String {
        return sharedPrefs.getString(KEY_USER, null)!!
    }

    private fun getServerPass(): String {
        return sharedPrefs.getString(KEY_PASS, null)!!
    }

    fun setProgressLabel(text: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.lblProgress)?.let {
                it.text = text
            }
        }
    }

    private fun checkSettings(): Boolean {
        val prefs = sharedPrefs
        return prefs.contains(KEY_IP) && prefs.contains(KEY_PORT) && prefs.contains(KEY_USER) && prefs.contains(KEY_PASS)
    }

    fun isShowingTitles(): Boolean {
        return sharedPrefs.getBoolean(KEY_SHOW_TITLES, true)
    }

    private fun setTitleVisibility(visible: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_SHOW_TITLES, visible).apply()
        libGrid?.adapter = libGrid?.adapter // redraws the items in the grid
    }

    private fun isCacheMode(): Boolean {
        return if (!sharedPrefs.contains(KEY_CACHE_MODE)) { // first run
            sharedPrefs.edit().putBoolean(KEY_CACHE_MODE, true).apply()
            false // So that everything is downloaded on first connection
        } else {
            sharedPrefs.getBoolean(KEY_CACHE_MODE, true)
        }
    }

    private fun isCacheModeViaPrefs(): Boolean {
        return sharedPrefs.getBoolean(KEY_CACHE_MODE, true)
    }

    private fun setCacheMode(on: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_CACHE_MODE, on).apply()
    }

    private fun clearServerPreferences() {
        sharedPrefs.edit()
            .remove(KEY_IP)
            .remove(KEY_PORT)
            .remove(KEY_USER)
            .remove(KEY_PASS)
            .remove(KEY_CACHE_MODE)
            .remove(KEY_LAST_LIB)
            .apply()
    }

    private fun restartApp(forceNetwork: Boolean = false) {
        // Dispose of current activity
        finish()
        // Restart activity with 1x network
        val restartIntent = Intent(applicationContext, MainActivity::class.java)
        restartIntent.putExtra("forceNetwork", forceNetwork)
        startActivity(restartIntent)
    }

    private fun toReadableName(libName: String): String {
        var name = libName.replace('_', ' ')
        name = name[0].toUpperCase() + name.substring(1)
        return name
    }
}