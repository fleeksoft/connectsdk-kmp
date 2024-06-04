package com.fleeksoft.connectsdk.ported.mdns

import com.fleeksoft.connectsdk.ported.ServiceListener

expect class KmDNS(address: String, name: String) : KmDNSInterface

interface KmDNSInterface {
    fun requestServiceInfo(type: String, name: String, timeout: Long)
    fun addServiceListener(type: String, serviceListener: ServiceListener)
    fun removeServiceListener(type: String)
    fun close()
}

data class ServiceEvent(val name: String, val type: String, val info: ServiceInfo)
data class ServiceInfo(val type: String, val name: String, val port: Int, val hostAddresses: List<String>)