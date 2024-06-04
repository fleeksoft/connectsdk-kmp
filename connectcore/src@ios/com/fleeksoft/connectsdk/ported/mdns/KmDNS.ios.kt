package com.fleeksoft.connectsdk.ported.mdns

import com.fleeksoft.connectsdk.ported.ServiceListener

actual class KmDNS actual constructor(address: String, name: String) : KmDNSInterface {
    override val address: String
        get() = TODO("Not yet implemented")
    override val name: String
        get() = TODO("Not yet implemented")

    override fun requestServiceInfo(type: String, name: String, timeout: Long) {
        TODO("Not yet implemented")
    }

    override fun addServiceListener(type: String, serviceListener: ServiceListener) {
        TODO("Not yet implemented")
    }

    override fun removeServiceListener(type: String, serviceListener: ServiceListener) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}