package com.fleeksoft.connectsdk.core

import com.fleeksoft.connectsdk.ported.DeviceInfo
import com.fleeksoft.connectsdk.service.capability.listeners.ErrorListener
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import kotlinx.coroutines.*
import kotlinx.datetime.Clock

object Util {
    var T: String = "Connect SDK"
    private val scope = CoroutineScope(Dispatchers.Default)

    private var job: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    // TODO: set limit to coroutines
    private const val NUM_OF_THREADS = 20

    suspend fun runOnUI(runnable: suspend () -> Unit) {
        withContext(Dispatchers.Main) {
            runnable()

        }
    }

    suspend fun runInBackground(runnable: suspend () -> Unit) {
        withContext(Dispatchers.IO) { runnable() }
    }

    fun <T> postSuccess(listener: ResponseListener<T>?, data: T) {
        if (listener == null) return

        scope.launch { runOnUI { listener.onSuccess(data) } }
    }

    fun postError(listener: ErrorListener?, error: ServiceCommandError) {
        if (listener == null) return

        scope.launch { runOnUI { listener.onError(error) } }
    }

    fun convertIpAddress(ip: Int): ByteArray {
        return byteArrayOf(
            (ip and 0xFF).toByte(),
            ((ip shr 8) and 0xFF).toByte(),
            ((ip shr 16) and 0xFF).toByte(),
            ((ip shr 24) and 0xFF).toByte()
        )
    }

    fun isIPv4Address(ipAddress: String): Boolean {
        TODO("not implemented yet")
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