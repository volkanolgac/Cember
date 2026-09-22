package com.example.volkan.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.volkan.util.VolkanLogger

enum class ConnectionType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER
}

enum class InternetStatus {
    NO_NETWORK,
    NETWORK_AVAILABLE,
    INTERNET_VALIDATED
}

data class NetworkState(
    val isConnected: Boolean,
    val isValidated: Boolean,
    val status: InternetStatus,
    val type: ConnectionType,
    val isWifi: Boolean,
    val isCellular: Boolean
)

class NetworkMonitor(
    private val context: Context,
    private val onNetworkChanged: (NetworkState) -> Unit = {}
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isMonitoring = false

    fun startMonitoring() {
        if (connectivityManager == null || isMonitoring) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                VolkanLogger.d(VolkanLogger.TAG_NETWORK, "Network available")
                onNetworkChanged(getCurrentNetworkState())
            }

            override fun onLost(network: Network) {
                VolkanLogger.d(VolkanLogger.TAG_NETWORK, "Network lost")
                onNetworkChanged(getCurrentNetworkState())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                VolkanLogger.d(
                    VolkanLogger.TAG_NETWORK,
                    "Network capabilities changed (validated=${capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)})"
                )
                onNetworkChanged(getCurrentNetworkState())
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
            isMonitoring = true
        } catch (e: Exception) {
            VolkanLogger.w(VolkanLogger.TAG_NETWORK, "Failed to register network callback: ${e.message}")
        }
    }

    fun stopMonitoring() {
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                VolkanLogger.w(VolkanLogger.TAG_NETWORK, "Failed to unregister network callback: ${e.message}")
            }
        }
        networkCallback = null
        isMonitoring = false
    }

    fun getCurrentNetworkState(): NetworkState {
        val cm = connectivityManager ?: return NetworkState(
            isConnected = false,
            isValidated = false,
            status = InternetStatus.NO_NETWORK,
            type = ConnectionType.NONE,
            isWifi = false,
            isCellular = false
        )
        val activeNetwork = cm.activeNetwork ?: return NetworkState(
            isConnected = false,
            isValidated = false,
            status = InternetStatus.NO_NETWORK,
            type = ConnectionType.NONE,
            isWifi = false,
            isCellular = false
        )
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkState(
            isConnected = false,
            isValidated = false,
            status = InternetStatus.NO_NETWORK,
            type = ConnectionType.NONE,
            isWifi = false,
            isCellular = false
        )

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val isCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val isEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        val type = when {
            isWifi -> ConnectionType.WIFI
            isCellular -> ConnectionType.CELLULAR
            isEthernet -> ConnectionType.ETHERNET
            hasInternet -> ConnectionType.OTHER
            else -> ConnectionType.NONE
        }

        val status = when {
            !hasInternet -> InternetStatus.NO_NETWORK
            isValidated -> InternetStatus.INTERNET_VALIDATED
            else -> InternetStatus.NETWORK_AVAILABLE
        }

        return NetworkState(
            isConnected = hasInternet,
            isValidated = isValidated,
            status = status,
            type = type,
            isWifi = isWifi,
            isCellular = isCellular
        )
    }
}
