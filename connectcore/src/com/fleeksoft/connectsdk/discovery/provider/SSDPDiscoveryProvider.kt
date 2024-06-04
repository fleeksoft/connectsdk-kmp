/*
 * SSDPDiscoveryProvider
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
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
package com.fleeksoft.connectsdk.discovery.provider

import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.discovery.DiscoveryFilter
import com.fleeksoft.connectsdk.discovery.DiscoveryProvider
import com.fleeksoft.connectsdk.discovery.DiscoveryProviderListener
import com.fleeksoft.connectsdk.discovery.provider.ssdp.SSDPClient
import com.fleeksoft.connectsdk.discovery.provider.ssdp.SSDPDevice
import com.fleeksoft.connectsdk.discovery.provider.ssdp.SSDPPacket
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import io.ktor.http.*
import io.ktor.network.sockets.*
import korlibs.io.lang.IOException
import kotlinx.coroutines.*
import kotlinx.datetime.Clock

open class SSDPDiscoveryProvider : DiscoveryProvider {
    var needToStartSearch: Boolean = false
    private val scope = CoroutineScope(Dispatchers.Default)

    private val serviceListeners: ConcurrentMutableList<DiscoveryProviderListener> =
        ConcurrentMutableList()

    var foundServices: ConcurrentMutableMap<String, ServiceDescription> = ConcurrentMutableMap()
    var discoveredServices: ConcurrentMutableMap<String, ServiceDescription> =
        ConcurrentMutableMap()

    var serviceFilters: MutableList<DiscoveryFilter>

    private var ssdpClient: SSDPClient? = null

    private var scanTimerJob: Job? = null
    private var responseThread: Job? = null
    private var notifyThread: Job? = null

    private val uuidReg: Regex = "(?<=uuid:)(.+?)(?=(::)|$)".toRegex()

    var isRunning: Boolean = false

    private suspend fun openSocket() {
        if (ssdpClient != null && ssdpClient!!.isConnected()) return

        try {
            val source: InetSocketAddress = InetSocketAddress(Util.getIpAddress(), 80)

            ssdpClient = createSocket(source)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    open fun createSocket(source: InetSocketAddress): SSDPClient {
        return SSDPClient(source)
    }

    override suspend fun start() {
        if (isRunning) return
        isRunning = true

        openSocket()
        scanTimerJob = scope.launch {
            delay(100)
            while (true) {
                sendSearch()
                delay(DiscoveryProvider.RESCAN_INTERVAL.toLong())
            }
        }

        responseThread = scope.launch {
            responseHandler()
        }

        notifyThread = scope.launch {
            respNotifyHandler()
        }
    }

    suspend fun sendSearch() {
        val killKeys: MutableList<String> = ArrayList()

        val killPoint: Long = Clock.System.now().toEpochMilliseconds() - DiscoveryProvider.TIMEOUT

        for (key: String in foundServices.keys) {
            val service: ServiceDescription? = foundServices[key]
            if (service == null || service.lastDetection < killPoint) {
                killKeys.add(key)
            }
        }

        for (key: String in killKeys) {
            val service: ServiceDescription? = foundServices[key]

            if (service != null) {
                notifyListenersOfLostService(service)
            }

            if (foundServices.containsKey(key)) foundServices.remove(key)
        }

        rescan()
    }

    override suspend fun stop() {
        isRunning = false
        scanTimerJob?.cancel()
        scanTimerJob = null

        ssdpClient?.close()
        ssdpClient = null

        responseThread?.cancel()
        responseThread = null

        notifyThread?.cancel()
        notifyThread = null

        ssdpClient?.close()
        ssdpClient = null

    }

    override suspend fun restart() {
        stop()
        start()
    }

    override suspend fun reset() {
        stop()
        foundServices.clear()
        discoveredServices.clear()
    }

    override fun rescan() {
        for (searchTarget: DiscoveryFilter in serviceFilters) {
            val message: String =
                SSDPClient.getSSDPSearchMessage(searchTarget.serviceFilter)

            for (i in 0..2) {
                scope.launch {
                    delay((i * 1000).toLong())
                    try {
                        if (ssdpClient != null) ssdpClient!!.send(message)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun addDeviceFilter(filter: DiscoveryFilter) {
        serviceFilters.add(filter)
    }

    override fun removeDeviceFilter(filter: DiscoveryFilter) {
        serviceFilters.remove(filter)
    }

    override fun setFilters(filters: List<DiscoveryFilter>) {
        serviceFilters = filters.toMutableList()
    }

    override fun isEmpty(): Boolean {
        return serviceFilters.size == 0
    }

    private suspend fun responseHandler() {
        while (ssdpClient != null) {
            try {
                handleSSDPPacket(SSDPPacket(ssdpClient!!.responseReceive()))
            } catch (e: IOException) {
                e.printStackTrace()
                break
            } catch (e: RuntimeException) {
                e.printStackTrace()
                break
            }
        }
    }

    private suspend fun respNotifyHandler() {
        while (ssdpClient != null) {
            try {
                handleSSDPPacket(SSDPPacket(ssdpClient!!.multicastReceive()))
            } catch (e: IOException) {
                e.printStackTrace()
                break
            } catch (e: RuntimeException) {
                e.printStackTrace()
                break
            }
        }
    }

    init {
        serviceFilters = ConcurrentMutableList()
    }

    private suspend fun handleSSDPPacket(ssdpPacket: SSDPPacket?) {
        // Debugging stuff
//        Util.runOnUI(new Runnable() {
//
//            @Override
//            public void run() {
//                Log.d("Connect SDK Socket", "Packet received | type = " + ssdpPacket.type);
//
//                for (String key : ssdpPacket.data.keySet()) {
//                    Log.d("Connect SDK Socket", "    " + key + " = " + ssdpPacket.data.get(key));
//                }
//                Log.d("Connect SDK Socket", "__________________________________________");
//            }
//        });
        // End Debugging stuff

        if ((ssdpPacket == null) || (ssdpPacket.getData().isEmpty())) return

        val serviceFilter: String? =
            ssdpPacket.getData()[if ((ssdpPacket.getType() == SSDPClient.NOTIFY)) "NT" else "ST"]

        if ((serviceFilter == null) || (SSDPClient.MSEARCH == ssdpPacket.getType()) || !isSearchingForFilter(
                serviceFilter
            )
        ) return

        val usnKey: String? = ssdpPacket.getData()["USN"]

        if (usnKey.isNullOrBlank()) return

        val m: MatchResult = uuidReg.find(usnKey) ?: return

        val uuid: String = m.groupValues[1]

        if ((SSDPClient.BYEBYE == ssdpPacket.getData()["NTS"])) {
            val service: ServiceDescription? = foundServices[uuid]

            if (service != null) {
                foundServices.remove(uuid)

                notifyListenersOfLostService(service)
            }
        } else {
            val location: String? = ssdpPacket.getData()["LOCATION"]

            if (location.isNullOrEmpty()) return

            var foundService: ServiceDescription? = foundServices[uuid]
            val discoverdService: ServiceDescription? = discoveredServices[uuid]

            val isNew: Boolean = foundService == null && discoverdService == null

            if (isNew) {
                foundService = ServiceDescription()
                foundService.uuid = uuid
                foundService.serviceFilter = serviceFilter
                foundService.ipAddress =
                    (ssdpPacket.datagramPacket.address as InetSocketAddress).hostname
                foundService.port = 3001

                discoveredServices[uuid] = foundService

                getLocationData(location, uuid, serviceFilter)
            }

            foundService?.lastDetection = Clock.System.now().toEpochMilliseconds()
        }
    }

    suspend fun getLocationData(location: String, uuid: String, serviceFilter: String?) {
        try {
            getLocationData(Url(location), uuid, serviceFilter)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun getLocationData(location: Url, uuid: String, serviceFilter: String?) {
        Util.runInBackground {

            var device: SSDPDevice? = null
            try {
                device = SSDPDevice.parse(location, serviceFilter)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (device != null) {
                device.UUID = uuid
                val hasServices: Boolean = containsServicesWithFilter(device, serviceFilter)

                if (hasServices) {
                    val service: ServiceDescription? = discoveredServices[uuid]

                    if (service != null) {
                        service.serviceFilter = serviceFilter
                        service.friendlyName = device.friendlyName
                        service.modelName = device.modelName
                        service.modelNumber = device.modelNumber
                        service.modelDescription = device.modelDescription
                        service.manufacturer = device.manufacturer
                        service.applicationURL = device.applicationURL
                        service.serviceList = device.serviceList
                        service.responseHeaders = device.headers
                        service.locationXML = device.locationXML
                        service.serviceURI = device.serviceURI
                        service.port = device.port

                        foundServices[uuid] = service

                        notifyListenersOfNewService(service)
                    }
                }
            }

            discoveredServices.remove(uuid)
        }
    }

    private suspend fun notifyListenersOfNewService(service: ServiceDescription) {
        val serviceIds: List<String> = serviceIdsForFilter(service.serviceFilter)

        for (serviceId: String in serviceIds) {
            val _newService: ServiceDescription = service.clone()
            _newService.serviceID = serviceId

            val newService: ServiceDescription = _newService

            Util.runOnUI {
                for (listener: DiscoveryProviderListener in serviceListeners) {
                    listener.onServiceAdded(this@SSDPDiscoveryProvider, newService)
                }
            }
        }
    }

    private suspend fun notifyListenersOfLostService(service: ServiceDescription) {
        val serviceIds: List<String> = serviceIdsForFilter(service.serviceFilter)

        for (serviceId: String in serviceIds) {
            val _newService: ServiceDescription = service.clone()
            _newService.serviceID = serviceId

            val newService: ServiceDescription = _newService

            Util.runOnUI {
                for (listener: DiscoveryProviderListener in serviceListeners) {
                    listener.onServiceRemoved(this@SSDPDiscoveryProvider, newService)
                }
            }
        }
    }

    fun serviceIdsForFilter(filter: String?): List<String> {
        val serviceIds: ArrayList<String> = ArrayList()

        for (serviceFilter: DiscoveryFilter in serviceFilters) {
            val ssdpFilter: String = serviceFilter.serviceFilter

            if ((ssdpFilter == filter)) {
                val serviceId: String = serviceFilter.serviceId
                serviceIds.add(serviceId)
            }
        }

        return serviceIds
    }

    fun isSearchingForFilter(filter: String): Boolean {
        for (serviceFilter: DiscoveryFilter in serviceFilters) {
            val ssdpFilter: String = serviceFilter.serviceFilter

            if ((ssdpFilter == filter)) return true
        }

        return false
    }

    fun containsServicesWithFilter(device: SSDPDevice?, filter: String?): Boolean {
//        List<String> servicesRequired = new ArrayList<String>();
//
//        for (JsonObject serviceFilter : serviceFilters) {
//        }

        //  TODO  Implement this method.  Not sure why needs to happen since there are now required services.

        return true
    }

    override fun addListener(listener: DiscoveryProviderListener) {
        serviceListeners.add(listener)
    }

    override fun removeListener(listener: DiscoveryProviderListener) {
        serviceListeners.remove(listener)
    }

    companion object {
        val instance by lazy { SSDPDiscoveryProvider() }
    }
}
