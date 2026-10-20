package com.sentinellab.agent

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var results: TextView
    private lateinit var scanner: WifiScanner

    private val permissions: Array<String>
        get() = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scanner = WifiScanner(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 48, 28, 28)
            setBackgroundColor(Color.rgb(10, 17, 31))
        }

        fun text(value: String, size: Float) =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(Color.rgb(220, 235, 250))
                setPadding(0, 12, 0, 12)
            }

        root.addView(text("SENTINEL AGENT", 26f))
        root.addView(text("Monitor de redes Wi-Fi", 16f))

        status = text("Estado: listo para escanear", 15f)
        root.addView(status)

        val scanButton = Button(this).apply {
            text = "Solicitar permisos y escanear"
            setOnClickListener {
                val missing = permissions.filter {
                    checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                }

                if (missing.isNotEmpty()) {
                    requestPermissions(missing.toTypedArray(), 100)
                } else {
                    scanNetworks()
                }
            }
        }
        root.addView(scanButton)

        results = text("Los resultados aparecerán aquí.", 14f)
        root.addView(results)

        setContentView(root)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            scanNetworks()
        } else {
            status.text = "Estado: faltan permisos"
        }
    }

    private fun scanNetworks() {
        status.text = "Estado: consultando redes..."

        Thread {
            try {
                val networks = scanner.scan()

                val report = if (networks.isEmpty()) {
                    "No hay resultados disponibles. Intenta escanear de nuevo."
                } else {
                    networks.joinToString("\n\n") { network ->
                        val ssid = network.ssid.ifBlank { "(SSID oculto)" }
                        val security = when {
                            network.capabilities.contains("WEP") -> "WEP"
                            network.capabilities.contains("PSK") -> "WPA/WPA2-PSK"
                            network.capabilities.contains("SAE") -> "WPA3-Personal"
                            network.capabilities.contains("EAP") -> "Enterprise"
                            network.capabilities.contains("OWE") -> "OWE"
                            else -> "Seguridad no identificada"
                        }

                        "$ssid\nSeñal: ${network.signalDbm} dBm\n" +
                            "Seguridad anunciada: $security\n" +
                            "Frecuencia: ${network.frequencyMhz} MHz"
                    }
                }

                runOnUiThread {
                    status.text = "Estado: consulta completada"
                    results.text = report
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Error: ${e.message ?: "no se pudo escanear"}"
                }
            }
        }.start()
    }
}