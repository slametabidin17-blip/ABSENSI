package com.example.util

import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtil {

    data class NetworkInfo(
        val ipAddress: String,
        val interfaceName: String,
        val isEthernet: Boolean
    )

    fun getLocalIpAddress(): NetworkInfo {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            var wifiIp: NetworkInfo? = null
            var otherIp: NetworkInfo? = null

            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val name = networkInterface.name.lowercase()
                val isEthernet = name.contains("eth") || name.contains("en")

                for (inetAddress in Collections.list(networkInterface.inetAddresses)) {
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        val hostAddress = inetAddress.hostAddress ?: continue
                        val info = NetworkInfo(
                            ipAddress = hostAddress,
                            interfaceName = networkInterface.displayName ?: networkInterface.name,
                            isEthernet = isEthernet
                        )

                        // If ethernet interface found, immediately prioritize it
                        if (isEthernet) {
                            return info
                        } else if (name.contains("wlan")) {
                            wifiIp = info
                        } else {
                            otherIp = info
                        }
                    }
                }
            }

            return wifiIp ?: otherIp ?: NetworkInfo("127.0.0.1", "Localhost", false)
        } catch (e: Exception) {
            return NetworkInfo("127.0.0.1", "Default", false)
        }
    }
}
