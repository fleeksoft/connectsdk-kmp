package com.fleeksoft.connectsdk.ported.mdns

import com.fleeksoft.connectsdk.ported.ServiceListener

actual class KmDNS actual constructor(address: String, name: String) : KmDNSInterface {

    override fun requestServiceInfo(type: String, name: String, timeout: Long) {
        TODO("Not yet implemented")
    }

    override fun addServiceListener(type: String, serviceListener: ServiceListener) {
        TODO("Not yet implemented")
    }

    override fun removeServiceListener(type: String) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}