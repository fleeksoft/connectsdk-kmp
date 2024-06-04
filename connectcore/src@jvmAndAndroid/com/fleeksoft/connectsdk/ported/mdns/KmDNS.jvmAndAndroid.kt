package com.fleeksoft.connectsdk.ported.mdns

import com.fleeksoft.connectsdk.ported.ServiceListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.InetAddress
import javax.jmdns.JmDNS

actual class KmDNS : KmDNSInterface {

    actual constructor(address: String, name: String) {
        this.jmDNS = JmDNS.create(InetAddress.getByAddress(address.toByteArray()), name)
    }


    constructor(jmDNS: JmDNS) {
        this.jmDNS = jmDNS
    }

    private val listenersMap: MutableMap<String, javax.jmdns.ServiceListener> = mutableMapOf()

    private var jmDNS: JmDNS
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun requestServiceInfo(type: String, name: String, timeout: Long) {
        jmDNS.requestServiceInfo(type, name)
    }

    override fun addServiceListener(type: String, serviceListener: ServiceListener) {
        val jServiceListener = object : javax.jmdns.ServiceListener {

            override fun serviceAdded(event: javax.jmdns.ServiceEvent) {
                scope.launch { serviceListener.serviceAdded(event.toKServiceEvent()) }
            }

            override fun serviceRemoved(event: javax.jmdns.ServiceEvent) {
                scope.launch { serviceListener.serviceRemoved(event.toKServiceEvent()) }
            }

            override fun serviceResolved(event: javax.jmdns.ServiceEvent) {
                scope.launch { serviceListener.serviceResovled(event.toKServiceEvent()) }
            }
        }
        listenersMap[type.lowercase()] = jServiceListener
        jmDNS.addServiceListener(type, jServiceListener)
    }

    override fun removeServiceListener(type: String) {
        val listener = listenersMap[type.lowercase()]
        if (listener != null) {
            jmDNS.removeServiceListener(type, listener)
        }
    }

    override fun close() {
        listenersMap.clear()
        jmDNS.close()
    }
}