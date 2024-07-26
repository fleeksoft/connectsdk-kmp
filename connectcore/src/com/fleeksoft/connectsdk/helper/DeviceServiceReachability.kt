package com.fleeksoft.connectsdk.helper

import com.fleeksoft.connectsdk.ported.PortedUtil
import korlibs.io.lang.IOException
import kotlinx.coroutines.*
import kotlin.coroutines.cancellation.CancellationException

class DeviceServiceReachability {
    private lateinit var ipAddress: String

    var listener: DeviceServiceReachabilityListener? = null

    private var testThread: Job? = null

    constructor()

    constructor(ipAddress: String) {
        this.ipAddress = ipAddress
    }

    constructor(ipAddress: String, listener: DeviceServiceReachabilityListener?) {
        this.ipAddress = ipAddress
        this.listener = listener
    }

    fun isRunning(): Boolean = testThread?.isActive ?: false

    fun start() {
        if (isRunning()) return

        testThread = GlobalScope.async(Dispatchers.IO) { testReachability }
    }

    fun stop() {
        if (!isRunning()) return

        testThread?.cancel()
        testThread = null
    }

    private suspend fun unreachable() {
        stop()
        listener?.onLoseReachability(this)
    }

    private val testReachability: suspend () -> Unit = {
        try {
            while (true) {
                if (!PortedUtil.isReachable(ipAddress, TIMEOUT)) unreachable()
                delay(TIMEOUT.toLong())
            }
        } catch (e: IOException) {
            unreachable()
        } catch (e: CancellationException) {
        }
    }

    interface DeviceServiceReachabilityListener {
        suspend fun onLoseReachability(reachability: DeviceServiceReachability?)
    }

    companion object {
        private const val TIMEOUT: Int = 10000
        fun getReachability(
            ipAddress: String,
            listener: DeviceServiceReachabilityListener?,
        ): DeviceServiceReachability {
            return DeviceServiceReachability(ipAddress, listener)
        }
    }
}
