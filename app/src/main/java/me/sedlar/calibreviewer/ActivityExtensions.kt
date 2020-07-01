package me.sedlar.calibreviewer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.sedlar.calibreviewer.util.SeriesFilter

fun AppCompatActivity.hasNetworkConnection(): Boolean {
    var result = false
    val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw =
            connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        result = when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    } else {
        connectivityManager.run {
            connectivityManager.activeNetworkInfo?.run {
                result = when (type) {
                    ConnectivityManager.TYPE_WIFI -> true
                    ConnectivityManager.TYPE_MOBILE -> true
                    ConnectivityManager.TYPE_ETHERNET -> true
                    else -> false
                }

            }
        }
    }
    return result
}

fun AppCompatActivity.requestWritePermissions() {
    // Request write permissions for writing OPDS XML files locally

    val permissionCheck =
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            9999
        )
    }
}

fun AppCompatActivity.calculateNoOfColumns(columnWidthDp: Float): Int {
    val displayMetrics = applicationContext.resources.displayMetrics
    val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
    return (screenWidthDp / columnWidthDp + 0.5).toInt()
}

val AppCompatActivity.sharedPrefs: SharedPreferences
    get() = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE)

fun AppCompatActivity.restartMainActivity(forceNetwork: Boolean = false, filter: SeriesFilter? = null) {
    // Dispose of current activity
    finish()
    // Restart activity with 1x network
    val restartIntent = Intent(applicationContext, MainActivity::class.java)
    restartIntent.putExtra("forceNetwork", forceNetwork)
    if (filter != null) {
        restartIntent.putExtra("filter", filter)
    }
    startActivity(restartIntent)
}

fun AppCompatActivity.dp2px(dp: Float): Int {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, applicationContext.resources.displayMetrics)
        .toInt()
}