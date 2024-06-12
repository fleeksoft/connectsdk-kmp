package com.fleeksoft.connectsdk.discovery.provider

import com.fleeksoft.connectsdk.MainDispatcherRule
import com.fleeksoft.connectsdk.discovery.DiscoveryFilter
import com.fleeksoft.connectsdk.discovery.DiscoveryManager
import com.fleeksoft.connectsdk.discovery.DiscoveryProvider
import com.fleeksoft.connectsdk.discovery.DiscoveryProviderListener
import com.fleeksoft.connectsdk.ported.mdns.KmDNS
import com.fleeksoft.connectsdk.ported.mdns.ServiceEvent
import com.fleeksoft.connectsdk.ported.mdns.ServiceInfo
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import java.net.InetAddress
import javax.jmdns.impl.JmDNSImpl

class ZeroConfDiscoveryProviderTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dp: ZeroconfDiscoveryProvider
    private lateinit var mDNS: KmDNS
    private lateinit var eventMock: ServiceEvent

    // Stub classes to allow mocking inside Robolectric test
    class StubJmDNS : JmDNSImpl {
        constructor() : super(null, null)
        constructor(address: InetAddress, name: String) : super(address, name)
    }

    inner class StubZeroConfDiscoveryProvider : ZeroconfDiscoveryProvider() {
        override fun createJmDNS(): KmDNS? {
            return mDNS
        }
    }

    @Before
    fun setUp() {
        dp = StubZeroConfDiscoveryProvider()
        mDNS = mock { KmDNS(Mockito.mock(StubJmDNS::class.java)) }
        eventMock = Mockito.mock(ServiceEvent::class.java)
        dp.kmdns = mDNS
    }

    @Test
    fun testStartShouldAddDeviceFilter() = runTest {
        val filter = DiscoveryFilter("Apple TV", "_testservicetype._tcp.local.")

        dp.addDeviceFilter(filter)
        dp.start()

        Thread.sleep(500)
        Mockito.verify(mDNS).addServiceListener(filter.serviceFilter, dp.kmdnsListener)
    }

    @Test
    @Throws(Exception::class)
    fun testStartShouldCancelPreviousSearch() = runTest {
        val filter = DiscoveryFilter("Apple TV", "_testservicetype._tcp.local.")

        dp.addDeviceFilter(filter)
        dp.stop()

        Mockito.verify(mDNS).removeServiceListener(filter.serviceFilter)
    }

    @Test
    fun testJmdnsServiceAdded() = runTest {
        dp.kmdnsListener.serviceAdded(eventMock)
        dp.start()

        Mockito.verify(eventMock, Mockito.atLeastOnce()).type
        Mockito.verify(eventMock, Mockito.atLeastOnce()).name
        Mockito.verify(mDNS, Mockito.timeout(100)).requestServiceInfo(eventMock.type, eventMock.name, 1)
    }

    @Test
    @Throws(Exception::class)
    fun testAddListener() {
        val listenerMock = Mockito.mock(DiscoveryManager::class.java)

        Assert.assertFalse(dp.serviceListeners.contains(listenerMock))
        dp.addListener(listenerMock)

        Assert.assertTrue(dp.serviceListeners.contains(listenerMock))
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveListener() {
        val listenerMock = Mockito.mock(DiscoveryManager::class.java)

        Assert.assertFalse(dp.serviceListeners.contains(listenerMock))
        dp.serviceListeners.add(listenerMock)
        Assert.assertTrue(dp.serviceListeners.contains(listenerMock))

        dp.removeListener(listenerMock)

        Assert.assertFalse(dp.serviceListeners.contains(listenerMock))
    }

    @Test
    @Throws(Exception::class)
    fun testFiltersAreEmptyByDefault() {
        val filter = DiscoveryFilter("Apple TV", "_testservicetype._tcp.local.")
        Assert.assertTrue(dp.isEmpty())

        dp.serviceFilters.add(filter)

        Assert.assertFalse(dp.isEmpty())
    }

    @Test
    fun testStopZeroConfService() = runTest {
        val filter = DiscoveryFilter("Apple TV", "_testservicetype._tcp.local.")
        dp.serviceFilters.add(filter)

        Mockito.verify(mDNS, Mockito.never()).removeServiceListener(filter.serviceFilter)
        dp.stop()
        Mockito.verify(mDNS, Mockito.times(1)).removeServiceListener(filter.serviceFilter)
    }

    @Test
    fun testReset() = runTest {
        val serviceDesc = ServiceDescription()
        dp.foundServices["service"] = serviceDesc
        Assert.assertFalse(dp.foundServices.isEmpty())

        dp.reset()
        Assert.assertTrue(dp.foundServices.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testAddDeviceFilter() {
        val filter = DiscoveryFilter("Test TV", "_testservicetype._tcp.local.")

        Assert.assertFalse(dp.serviceFilters.contains(filter))
        dp.addDeviceFilter(filter)

        Assert.assertTrue(dp.serviceFilters.contains(filter))
    }

    @Test
    @Throws(Exception::class)
    fun testRemoveDeviceFilter() {
        val filter = DiscoveryFilter("Test TV", "_testservicetype._tcp.local.")

        dp.serviceFilters.add(filter)
        Assert.assertFalse(dp.serviceFilters.isEmpty())

        dp.removeDeviceFilter(filter)

        Assert.assertTrue(dp.serviceFilters.isEmpty())
    }

    @Test
    @Throws(Exception::class)
    fun testServiceIdForFilter() {
        val filter = DiscoveryFilter("Test TV", "_testservicetype._tcp.local.")
        dp.serviceFilters.add(filter)

        val serviceId = dp.serviceIdForFilter(filter.serviceFilter)

        Assert.assertEquals("Test TV", serviceId)
    }

    private fun createMockedServiceEvent(ip: String, name: String): ServiceEvent {
        val event = Mockito.mock(ServiceEvent::class.java)
        val info = Mockito.mock(ServiceInfo::class.java)

        Mockito.`when`(event.name).thenReturn(name)
        Mockito.`when`(event.type).thenReturn("serviceEventType")
        Mockito.`when`(event.info).thenReturn(info)
        Mockito.`when`(info.hostAddresses).thenReturn(listOf(ip))
        Mockito.`when`(info.port).thenReturn(7000)
        Mockito.`when`(info.name).thenReturn(name)
        Mockito.`when`(info.type).thenReturn("infoType")
        return event
    }

    @Test
    fun testServiceResolveEvent() = runTest {
        val event = createMockedServiceEvent("192.168.0.1", "Test TV")
        val listener = Mockito.mock(DiscoveryProviderListener::class.java)
        dp.addListener(listener)

        dp.kmdnsListener.serviceResolved(event)

        Mockito.verify(listener).onServiceAdded(
            any<DiscoveryProvider>(),
            any<ServiceDescription>()
        )
    }

    @Test
    fun testServiceResolveEventWhenThereIsFoundService() = runTest {
        val uuid = "192.168.0.1"
        val name = "Test TV"
        val serviceDescription = ServiceDescription("_testservicetype._tcp.local.", uuid, uuid).apply {
            friendlyName = name
        }
        val event = createMockedServiceEvent(uuid, name)
        dp.foundServices[uuid] = serviceDescription
        val listener = Mockito.mock(DiscoveryProviderListener::class.java)
        dp.addListener(listener)

        dp.kmdnsListener.serviceResolved(event)

        Mockito.verify(listener, Mockito.never()).onServiceAdded(
            any<DiscoveryProvider>(),
            any<ServiceDescription>()
        )
    }

    @Test
    fun testServiceRemoveEvent() = runTest {
        val uuid = "192.168.0.1"
        val name = "Test TV"
        val serviceDescription = ServiceDescription("_testservicetype._tcp.local.", uuid, uuid).apply {
            friendlyName = name
        }
        val event = createMockedServiceEvent(uuid, name)
        val listener = Mockito.mock(DiscoveryProviderListener::class.java)
        dp.addListener(listener)
        dp.foundServices[uuid] = serviceDescription

        dp.kmdnsListener.serviceRemoved(event)

        Mockito.verify(listener)
            .onServiceRemoved(any<DiscoveryProvider>(), any<ServiceDescription>())
    }

    @Test
    fun testStateAfterConstruction() {
        Assert.assertNotNull(dp.foundServices)
        Assert.assertNotNull(dp.serviceFilters)
        Assert.assertNotNull(dp.serviceListeners)
        Assert.assertTrue(dp.foundServices.isEmpty())
        Assert.assertTrue(dp.serviceFilters.isEmpty())
        Assert.assertTrue(dp.serviceListeners.isEmpty())
        Assert.assertNotNull(dp.srcAddress)
    }
}
