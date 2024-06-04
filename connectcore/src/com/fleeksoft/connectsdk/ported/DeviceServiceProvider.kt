package com.fleeksoft.connectsdk.ported

import com.fleeksoft.connectsdk.discovery.DiscoveryFilter
import com.fleeksoft.connectsdk.service.DeviceService
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import kotlin.reflect.KClass

class DeviceServiceProvider<out T : DeviceService>(
    private val kClass: KClass<T>,
    private val constructor: (serviceDescription: ServiceDescription, serviceConfig: ServiceConfig) -> T,
    private val discoverFilter: () -> DiscoveryFilter,
) {
    fun create(serviceDescription: ServiceDescription, serviceConfig: ServiceConfig) =
        constructor.invoke(serviceDescription, serviceConfig)

    fun getDiscoveryFilter() = discoverFilter()


    fun isSameClass(kClass: KClass<*>) = this.kClass == kClass
}