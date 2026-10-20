package com.sentinellab.agent

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val signalDbm: Int,
    val capabilities: String,
    val frequencyMhz: Int
)

class WifiScanner(private val context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun scan(): List<WifiNetwork> {
        val locationGranted =
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val nearbyGranted =
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.NEARBY_WIFI_DEVICES
                ) == PackageManager.PERMISSION_GRANTED

        if (!locationGranted || !nearbyGranted) {
            throw SecurityException("Faltan permisos para consultar redes Wi-Fi.")
        }

        if (!wifiManager.isWifiEnabled) {
            throw IllegalStateException("Activa el Wi-Fi antes de escanear.")
        }

        // Inicia una búsqueda; Android puede limitar su frecuencia.
        wifiManager.startScan()

        return wifiManager.scanResults.map { result ->
            WifiNetwork(
                ssid = result.SSID.orEmpty(),
                bssid = result.BSSID.orEmpty(),
                signalDbm = result.level,
                capabilities = result.capabilities.orEmpty(),
                frequencyMhz = result.frequency
            )
        }
    }
}