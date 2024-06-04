package com.fleeksoft.connectsdk

import com.fleeksoft.connectsdk.discovery.DiscoveryProvider
import com.fleeksoft.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.fleeksoft.connectsdk.ported.DeviceServiceProvider
import com.fleeksoft.connectsdk.service.DLNAService
import com.fleeksoft.connectsdk.service.DeviceService

object DefaultPlatform {
    fun getDeviceServiceMap(): HashMap<DeviceServiceProvider<DeviceService>, Lazy<DiscoveryProvider>> {
        return hashMapOf(
            DLNAService.getServiceProvider() to lazy { SSDPDiscoveryProvider.instance }
        )
        /*val devicesList: HashMap<String, String> = hashMapOf()
        devicesList["com.connectsdk.service.WebOSTVService"] =
            "com.connectsdk.discovery.provider.SSDPDiscoveryProvider"
        devicesList["com.connectsdk.service.NetcastTVService"] =
            "com.connectsdk.discovery.provider.SSDPDiscoveryProvider"
        devicesList["com.connectsdk.service.DLNAService"] =
            "com.connectsdk.discovery.provider.SSDPDiscoveryProvider"
        devicesList["com.connectsdk.service.DIALService"] =
            "com.connectsdk.discovery.provider.SSDPDiscoveryProvider"
        devicesList["com.connectsdk.service.RokuService"] =
            "com.connectsdk.discovery.provider.SSDPDiscoveryProvider"
        devicesList["com.connectsdk.service.CastService"] =
            "com.connectsdk.discovery.provider.CastDiscoveryProvider"
        devicesList["com.connectsdk.service.AirPlayService"] =
            "com.fleeksoft.connectsdk.discovery.provider.ZeroconfDiscoveryProvider"
        devicesList["com.connectsdk.service.FireTVService"] =
            "com.connectsdk.discovery.provider.FireTVDiscoveryProvider"
        return devicesList*/
    }
}
