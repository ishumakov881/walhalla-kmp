package org.imaginativeworld.oopsnointernet.utils


import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.provider.Settings.ACTION_SETTINGS
import android.provider.Settings.ACTION_WIFI_SETTINGS
import android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY
import android.widget.Toast
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Открывает раздел "Беспроводные сети" (Wireless & Networks).
 * Используется для глобального сброса связи (переключение Авиарежима,
 * глубокие настройки SIM и Wi-Fi), когда локальные меры не помогают.
 *
 * Важно! не Context а Activity для коректной работы backStack
 */
fun Activity.openWirelessSettings() {
    try {
        val intent =
            Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        startActivity(intent)
    } catch (e: Exception) {
        // Фолбек на общие настройки, если специфичный раздел недоступен
        // (крайне редко для этого интента)
        try {
            startActivity(Intent(ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        } catch (inner: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Open the wifi settings.
 */
fun Context.turnOnWifi() {
    try {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Для Android 10+ открываем красивую плавающую панель
                // Там кнопка "Войти" будет на самом видном месте
                val intent = Intent(ACTION_INTERNET_CONNECTIVITY)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                this.startActivity(intent)
            } else {
                // Для старых версий открываем обычные настройки Wi-Fi
                val intent = Intent(ACTION_WIFI_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                this.startActivity(intent)
            }
        } catch (e: Exception) {
            // На случай совсем экзотических прошивок — открываем общие настройки
            val intent = Intent(ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            this.startActivity(intent)
        }
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "It cannot open settings!", Toast.LENGTH_LONG).show()
    }
}

fun Context.openCaptivePortalSettings() {
    turnOnWifi()
}

actual object NoInternetUtils {


    private val context
        get() = NoInternetActivityHolder.activity
            ?: error("ActivityHolder is not initialized")
    /**
     * Check if the device is connected with the Internet.
     */

    actual fun isConnectedToInternet(): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    /**
     * Check if the device is in airplane mode.
     */

    actual fun isAirplaneModeOn(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
    }

    /**
     * Check if the device has an active VPN connection.
     */

    actual fun isVpnActive(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_VPN)?.isConnectedOrConnecting == true
        }
    }

    /**
     * Ping google.com to check if the internet connection is active.
     * It must be called from a background thread.
     */

    actual fun hasActiveInternetConnection(): Boolean {
        try {
            val urlConnection =
                URL("https://www.google.com").openConnection() as HttpURLConnection

            urlConnection.setRequestProperty("User-Agent", "Test")
            urlConnection.setRequestProperty("Connection", "close")
            urlConnection.connectTimeout = 1500
            urlConnection.connect()

            return urlConnection.responseCode == 200
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * Open the system settings.
     */

    actual fun turnOnMobileData() {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "It cannot open settings!", Toast.LENGTH_LONG).show()
        }
    }


    /**
     * Open the airplane mode settings.
     */

    actual fun turnOffAirplaneMode() {
        try {
            context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "It cannot open settings!", Toast.LENGTH_LONG).show()
        }
    }

    actual fun turnOnWifi() {
        context.turnOnWifi()
    }
}