package com.fleeksoft.connectsdk.discovery.provider

import co.touchlab.kermit.Logger
import co.touchlab.stately.collections.ConcurrentMutableList
import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.discovery.DiscoveryFilter
import com.fleeksoft.connectsdk.discovery.DiscoveryProvider
import com.fleeksoft.connectsdk.discovery.DiscoveryProviderListener
import com.fleeksoft.connectsdk.ported.ServiceListener
import com.fleeksoft.connectsdk.ported.mdns.KmDNS
import com.fleeksoft.connectsdk.ported.mdns.ServiceEvent
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

open class ZeroconfDiscoveryProvider : DiscoveryProvider {
    private val HOSTNAME = "connectsdk"

    var kmdns: KmDNS? = null
    var srcAddress: String? = null

    private var scanTimerJob: Job? = null

    var serviceFilters: MutableList<DiscoveryFilter> = ConcurrentMutableList()
    var foundServices: HashMap<String, ServiceDescription> = HashMap()
    var serviceListeners: ConcurrentMutableList<DiscoveryProviderListener> = ConcurrentMutableList()

    var isRunning = false

    internal val kmdnsListener = object : ServiceListener {
        override suspend fun serviceResolved(event: ServiceEvent) {
            val ipAddress = event.info.hostAddresses[0]
            if (!Util.isIPv4Address(ipAddress)) {
                // Currently, we only support ipv4
                return
            }

            val friendlyName = event.info.name
            val port = event.info.port

            var foundService = foundServices[ipAddress]

            val isNew = foundService == null
            var listUpdateFlag = false

            if (isNew) {
                foundService = ServiceDescription().apply {
                    this.uuid = ipAddress
                    this.serviceFilter = event.info.type
                    this.ipAddress = ipAddress
                    this.serviceID = serviceIdForFilter(event.info.type)
                    this.port = port
                    this.friendlyName = friendlyName
                }
                listUpdateFlag = true
            } else {
                if (foundService!!.friendlyName != friendlyName) {
                    foundService.friendlyName = friendlyName
                    listUpdateFlag = true
                }
            }

            foundService.lastDetection = Util.getTime()

            foundServices[ipAddress] = foundService

            if (listUpdateFlag) {
                serviceListeners.forEach { listener ->
                    listener.onServiceAdded(this@ZeroconfDiscoveryProvider, foundService)
                }
            }
        }

        override suspend fun serviceRemoved(event: ServiceEvent) {
            val uuid = event.info.hostAddresses[0]
            val service = foundServices[uuid]

            service?.let {
                Util.runOnUI {
                    serviceListeners.forEach { listener ->
                        listener.onServiceRemoved(this@ZeroconfDiscoveryProvider, service)
                    }
                }
            }
        }

        override suspend fun serviceAdded(event: ServiceEvent) {
            kmdns?.requestServiceInfo(event.type, event.name, 1)
        }
    }

    init {
        runCatching { srcAddress = Util.getIpAddress() }.onFailure { it.printStackTrace() }
    }

    override suspend fun start() {
        if (isRunning) return

        isRunning = true

        scanTimerJob = GlobalScope.launch {
            delay(100)
            while (scanTimerJob?.isActive == true) {
                mdnsSearchTask()
                delay(DiscoveryProvider.RESCAN_INTERVAL.toLong())
            }
        }
    }

    open fun createJmDNS(): KmDNS? {
        return if (srcAddress != null) KmDNS(srcAddress!!, HOSTNAME) else null
    }

    suspend fun mdnsSearchTask() {
        val killKeys = mutableListOf<String>()
        val killPoint = Util.getTime() - DiscoveryProvider.TIMEOUT

        for (key in foundServices.keys) {
            val service = foundServices[key]
            if (service == null || service.lastDetection < killPoint) {
                killKeys.add(key)
            }
        }

        for (key in killKeys) {
            val service = foundServices[key]
            service?.let {
                Util.runOnUI {
                    serviceListeners.forEach { listener ->
                        listener.onServiceRemoved(this@ZeroconfDiscoveryProvider, service)
                    }
                }
            }

            foundServices.remove(key)
        }

        rescan()
    }

    override suspend fun stop() {
        isRunning = false

        scanTimerJob?.cancel()
        scanTimerJob = null

        kmdns?.let { jmdnsInstance ->
            serviceFilters.forEach { searchTarget ->
                jmdnsInstance.removeServiceListener(searchTarget.serviceFilter)
            }
        }
    }

    override suspend fun restart() {
        stop()
        start()
    }

    override suspend fun reset() {
        stop()
        foundServices.clear()
    }

    override fun rescan() {
        try {
            kmdns?.close()
            kmdns = null
            kmdns = createJmDNS()

            kmdns?.let { jmdnsInstance ->
                serviceFilters.forEach { searchTarget ->
                    jmdnsInstance.addServiceListener(searchTarget.serviceFilter, kmdnsListener)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun addListener(listener: DiscoveryProviderListener) {
        serviceListeners.add(listener)
    }

    override fun removeListener(listener: DiscoveryProviderListener) {
        serviceListeners.remove(listener)
    }

    override fun addDeviceFilter(filter: DiscoveryFilter) {
        filter.serviceFilter?.let { serviceFilters.add(filter) } ?: Logger.e(
            Util.T,
            message = { "This device filter does not have zeroconf filter info" }
        )
    }

    override fun removeDeviceFilter(filter: DiscoveryFilter) {
        serviceFilters.remove(filter)
    }

    override fun setFilters(filters: List<DiscoveryFilter>) {
        serviceFilters = filters.toMutableList()
    }

    override fun isEmpty(): Boolean {
        return serviceFilters.isEmpty()
    }

    fun serviceIdForFilter(filter: String): String {
        for (serviceFilter in serviceFilters) {
            if (serviceFilter.serviceFilter == filter) {
                return serviceFilter.serviceId
            }
        }
        return ""
    }
}
