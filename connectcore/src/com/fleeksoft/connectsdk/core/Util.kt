package com.fleeksoft.connectsdk.core

import com.fleeksoft.connectsdk.ported.DeviceInfo
import com.fleeksoft.connectsdk.service.capability.listeners.ErrorListener
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import kotlinx.coroutines.*
import kotlinx.datetime.Clock

object Util {
    var T: String = "Connect SDK"

    private var job: Job? = null

    // TODO: set limit to coroutines
    private const val NUM_OF_THREADS = 20

    suspend fun runOnUI(runnable: suspend () -> Unit) {
        withContext(Dispatchers.Main) { runnable() }
    }

    suspend fun runInBackground(runnable: suspend () -> Unit) {
        withContext(Dispatchers.IO) { runnable() }
    }

    suspend fun <T> postSuccess(listener: ResponseListener<T>, data: T) {
        withContext(Dispatchers.Main) {
            listener.onSuccess(data)
        }
    }

    suspend fun postError(listener: ErrorListener, error: ServiceCommandError) {
        withContext(Dispatchers.Main) {
            listener.onError(error)
        }
    }

    fun convertIpAddress(ip: Int): ByteArray {
        return byteArrayOf(
            (ip and 0xFF).toByte(),
            ((ip shr 8) and 0xFF).toByte(),
            ((ip shr 16) and 0xFF).toByte(),
            ((ip shr 24) and 0xFF).toByte()
        )
    }

    fun isIPv4Address(ip: String): Boolean {
        val ipv4Pattern = Regex(
            "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}" +
                "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
        )
        return ipv4Pattern.matches(ip)
    }

    fun getIpAddress(): String {
        // TODO: fix it for test 
        return "0.0.0.0"
        return DeviceInfo().getIpAddress()
    }

    fun getHostAddress(): String {
//        Util.getIpAddress().getHostAddress()
        TODO("not implemented yet")
    }

    fun getTime(): Long {
        return Clock.System.now().epochSeconds
    }
}