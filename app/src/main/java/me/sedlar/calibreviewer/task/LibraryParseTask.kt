package me.sedlar.calibreviewer.task

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import me.sedlar.calibre.opds.OPDSConnector
import me.sedlar.calibre.opds.OPDSParser
import me.sedlar.calibre.opds.local.OPDSLibrary
import me.sedlar.calibreviewer.MainActivity
import me.sedlar.calibreviewer.hasNetworkConnection
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

typealias LibTaskCallback = (List<OPDSLibrary>) -> Unit

class LibraryParseTask(private val activity: MainActivity, private val onFinish: LibTaskCallback? = null) :
    AsyncTask<String, Void, List<OPDSLibrary>>() {

    override fun doInBackground(vararg params: String): List<OPDSLibrary> {
        val parser = OPDSParser(
            baseURL = params[0],
            username = params[1],
            password = params[2],
            dataDir = File(params[3])
        )

        val xmlFile = File(File(params[3]), "calibre_opds.xml")
        val forceNetwork = !xmlFile.exists()
        val checker = OPDSConnector.checker

        if (forceNetwork) {
            println("Forcing checker to non-cache")
            OPDSConnector.checker = { activity.hasNetworkConnection() }
        } else {
            println("Library has already been parsed, using normal checker.")
            println(xmlFile.absolutePath + " - " + xmlFile.exists())
            println(xmlFile.length())
        }

        val libs = parser.parse()

        libs.forEach { lib ->
            activity.setProgressLabel("Cleaning Cache...")
            lib.cleanCache()
            activity.setProgressLabel("Cache Cleaned!")

            lib.seriesList.forEach { series ->
                series.entries.firstOrNull()?.let { firstEntry ->
                    // Download and resize thumbnail for series view
                    val thumbFile = lib.getThumbFile(series, firstEntry)
                    if (!thumbFile.exists()) {
                        activity.setProgressLabel("Downloading thumbnail... \n\n ${series.name}")
                        val thumb = lib.getThumbURL(firstEntry.cover)
                        lib.downloadAsBytes(thumb)?.let { thumbData ->
                            activity.setProgressLabel("Resizing thumbnail... \n\n ${series.name}")
                            ByteArrayInputStream(thumbData).use { input ->
                                val bitmap = BitmapFactory.decodeStream(input)
                                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 400, 600, false)
                                thumbFile.parentFile?.mkdirs()
                                FileOutputStream(thumbFile).use { output ->
                                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (forceNetwork) {
            OPDSConnector.checker = checker
        }

        return libs
    }

    override fun onPostExecute(result: List<OPDSLibrary>) {
        super.onPostExecute(result)
        onFinish?.let { it(result) }
    }
}