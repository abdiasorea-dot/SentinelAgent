package com.sentinellab.agent

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SentinelApi(
    private val baseUrl: String,
    private val token: String
) {
    fun sendScan(deviceName: String, networks: List<WifiNetwork>) {
        require(baseUrl.startsWith("https://")) {
            "Configura una URL HTTPS para enviar datos."
        }
        require(token.length >= 32) {
            "El token debe tener al menos 32 caracteres."
        }

        val networkArray = JSONArray()

        networks.forEach { network ->
            networkArray.put(
                JSONObject().apply {
                    put("ssid", network.ssid)
                    put("bssid", network.bssid)
                    put("signal_dbm", network.signalDbm)
                    put("capabilities", network.capabilities)
                    put("frequency_mhz", network.frequencyMhz)
                }
            )
        }

        val payload = JSONObject().apply {
            put("device_name", deviceName)
            put("networks", networkArray)
        }

        val connection = URL("$baseUrl/api/scans").openConnection()
            as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")

            connection.outputStream.use {
                it.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("La API respondió HTTP $status")
            }
        } finally {
            connection.disconnect()
        }
    }
}