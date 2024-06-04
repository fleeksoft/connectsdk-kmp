package com.fleeksoft.connectsdk.discovery

import com.fleeksoft.connectsdk.discovery.provider.SSDPDiscoveryProvider
import com.fleeksoft.connectsdk.service.DIALService
import com.fleeksoft.connectsdk.service.DLNAService
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * Created by oleksii.frolov on 2/16/2015.
 */
class DiscoveryManagerTest {
    private var discovery = DiscoveryManager()

    @Test
    fun testUnregisterDeviceServiceWithWrongArguments() = runTest {
        discovery.deviceProvideClasses["service"] = DIALService.getServiceProvider()
        Assert.assertEquals(1, discovery.deviceProvideClasses.size)

        discovery.unregisterDeviceService(DLNAService.discoveryFilter(), SSDPDiscoveryProvider.instance)
        Assert.assertEquals(1, discovery.deviceProvideClasses.size)

        discovery.unregisterDeviceService(DIALService.discoveryFilter(), SSDPDiscoveryProvider.instance)
        Assert.assertEquals(1, discovery.deviceProvideClasses.size)
    }

    @Test
    fun testUnregisterDeviceServiceWithWrongProvider() {
        discovery.discoveryProviders.add(SSDPDiscoveryProvider.instance)
        discovery.deviceProvideClasses[DIALService.ID] = DIALService.getServiceProvider()
        Assert.assertEquals(1, discovery.discoveryProviders.size)
        Assert.assertEquals(1, discovery.deviceProvideClasses.size)

        // TODO: complete it 
        /*discovery.unregisterDeviceService(
            DIALService.discoveryFilter(),
            ZeroconfDiscoveryProvider::class.java
        )*/
        Assert.assertEquals(1, discovery.deviceProvideClasses.size)
        Assert.assertEquals(1, discovery.discoveryProviders.size)
    }

    @Test
    fun testUnregisterDeviceServiceWithWrongServiceID() = runTest {
        discovery.discoveryProviders.add(SSDPDiscoveryProvider.instance)
        discovery.deviceProvideClasses[DLNAService.ID] = DIALService.getServiceProvider()
        Assert.assertEquals(1, discovery.discoveryProviders.size)
        Assert.assertEquals(1, discovery.deviceProvideClasses.size)

        discovery.unregisterDeviceService(
            DIALService.discoveryFilter(),
            SSDPDiscoveryProvider.instance
        )
        Assert.assertEquals(1, discovery.deviceProvideClasses.size)
        Assert.assertEquals(1, discovery.discoveryProviders.size)
    }

    @Test
    fun testUnregisterDeviceService() = runTest {
        discovery.discoveryProviders.add(SSDPDiscoveryProvider.instance)
        discovery.deviceProvideClasses[DIALService.ID] = DIALService.getServiceProvider()
        Assert.assertEquals(1, discovery.discoveryProviders.size)
        Assert.assertEquals(1, discovery.deviceProvideClasses.size)

        discovery.unregisterDeviceService(
            DIALService.discoveryFilter(),
            SSDPDiscoveryProvider.instance
        )
        Assert.assertEquals(0, discovery.deviceProvideClasses.size)
        Assert.assertEquals(0, discovery.discoveryProviders.size)
    }

    @Test
    fun testRegisterDeviceService() = runTest {
        Assert.assertEquals(0, discovery.discoveryProviders.size)
        Assert.assertEquals(0, discovery.deviceProvideClasses.size)

        discovery.registerDeviceService(
            DIALService.getServiceProvider(),
            lazy { SSDPDiscoveryProvider.instance }
        )
        Assert.assertEquals(1, discovery.discoveryProviders.size)
        Assert.assertEquals(1, discovery.deviceProvideClasses.size)
    }
}
