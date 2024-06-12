package com.fleeksoft.connectsdk.ported.mdns


fun javax.jmdns.ServiceEvent.toKServiceEvent(): ServiceEvent {
    val info = ServiceInfo(this.info.type, this.info.name, this.info.port, this.info.hostAddresses.toList())
    return ServiceEvent(this.name, this.type, info)
}

