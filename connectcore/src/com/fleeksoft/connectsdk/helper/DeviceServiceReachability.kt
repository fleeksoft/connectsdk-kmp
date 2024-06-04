/*
 * DeviceServiceReachability
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Jeffrey Glenn on 16 Apr 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fleeksoft.connectsdk.etc.helper

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

    val isRunning: Boolean
        get() = testThread?.isActive ?: false

    fun start() {
        if (isRunning) return

        testThread = GlobalScope.async(Dispatchers.IO) { testReachability }
    }

    fun stop() {
        if (!isRunning) return

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
