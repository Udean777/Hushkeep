package com.ssajudn.hushkeep.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable

class NetworkMonitor(context: Context) : Closeable {
    private val connectivityManager = context.applicationContext
        .getSystemService(ConnectivityManager::class.java)
    private val _isOnline = MutableStateFlow(false)
    private var isRegistered = false

    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateFromActiveNetwork()
        }

        override fun onLost(network: Network) {
            updateFromActiveNetwork()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            _isOnline.value = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED,
            )
        }
    }

    fun start() {
        if (isRegistered) return
        updateFromActiveNetwork()
        connectivityManager.registerDefaultNetworkCallback(callback)
        isRegistered = true
    }

    fun stop() {
        if (!isRegistered) return
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        isRegistered = false
        _isOnline.value = false
    }

    private fun updateFromActiveNetwork() {
        val capabilities = connectivityManager
            .activeNetwork
            ?.let(connectivityManager::getNetworkCapabilities)

        _isOnline.value = capabilities?.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
        ) == true
    }

    override fun close() = stop()
}
