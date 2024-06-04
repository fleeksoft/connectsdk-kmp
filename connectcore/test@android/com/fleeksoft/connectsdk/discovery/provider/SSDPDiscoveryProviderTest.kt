package com.fleeksoft.connectsdk.discovery.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fleeksoft.connectsdk.core.TestUtil
import com.fleeksoft.connectsdk.discovery.DiscoveryFilter
import com.fleeksoft.connectsdk.discovery.provider.ssdp.SSDPClient
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify
import java.io.IOException
import kotlin.test.*

@RunWith(AndroidJUnit4::class)
class SSDPDiscoveryProviderTest {

    private lateinit var dp: SSDPDiscoveryProvider
    private val ssdpClient: SSDPClient = Mockito.mock(SSDPClient::class.java)

    inner class StubSSDPDiscoveryProvider : SSDPDiscoveryProvider() {

        override fun createSocket(source: InetSocketAddress): SSDPClient {
            return ssdpClient
        }
    }

    @BeforeTest
    fun setUp() = runTest {
        val data = ByteArray(1)
        Mockito.`when`(ssdpClient.responseReceive())
            .thenReturn(Datagram(ByteReadPacket(data), InetSocketAddress("1", 80)))
        Mockito.`when`(ssdpClient.multicastReceive())
            .thenReturn(Datagram(ByteReadPacket(data), InetSocketAddress("1", 80)))
        dp = StubSSDPDiscoveryProvider()
        assertNotNull(dp)
    }

    @AfterTest
    fun tearDown() = runTest {
        dp.stop()
    }

    @Test
    fun testStop() = runTest {
        dp.start()
        dp.stop()
        verify(ssdpClient, Mockito.times(1)).close()
    }

    @Test
    fun testAddDeviceFilter() {
        val filter = DiscoveryFilter("DLNA", "urn:schemas-upnp-org:device:MediaRenderer:1")
        dp.addDeviceFilter(filter)
        assertTrue(dp.serviceFilters.contains(filter))
    }

    @Test
    fun testRemoveDeviceFilters() {
        val filter = DiscoveryFilter("DLNA", "urn:schemas-upnp-org:device:MediaRenderer:1")
        dp.serviceFilters.add(filter)
        dp.removeDeviceFilter(filter)
        assertFalse(dp.serviceFilters.contains(filter))
    }

    @Test
    fun testRemoveDeviceFiltersWithEmptyFilterString() {
        val filter = DiscoveryFilter("DLNA", "")
        dp.serviceFilters.add(filter)
        dp.removeDeviceFilter(filter)
        assertFalse(dp.serviceFilters.contains(filter))
    }

    @Test
    fun testRemoveDeviceFiltersWithEmptyId() {
        val filter = DiscoveryFilter("", "urn:schemas-upnp-org:device:MediaRenderer:1")
        dp.serviceFilters.add(filter)
        dp.removeDeviceFilter(filter)
        assertFalse(dp.serviceFilters.contains(filter))
    }

    @Test
    fun testRemoveDeviceFiltersWithDifferentFilterStrings() {
        val filter = DiscoveryFilter("DLNA", "urn:schemas-upnp-org:device:MediaRenderer:1")
        dp.serviceFilters.add(filter)
        dp.removeDeviceFilter(DiscoveryFilter("DLNA", ""))
        assertTrue(dp.serviceFilters.contains(filter))
    }

    @Test
    fun testIsEmpty() {
        val filter = DiscoveryFilter("DLNA", "urn:schemas-upnp-org:device:MediaRenderer:1")
        assertTrue(dp.isEmpty())
        dp.serviceFilters.add(filter)
        assertFalse(dp.isEmpty())
    }

    @Test
    fun testGetLocationDataFrom() = runTest {
        val uuid = "0f574021-141a-ebe8-eeac-bcf7b973615a"
        val serviceFilter = "urn:lge-com:service:webos-second-screen:1"
        val deviceDescription = """
            <root xmlns="urn:schemas-upnp-org:device-1-0" xmlns:dlna="urn:schemas-dlna-org:device-1-0">
            <specVersion>
            <major>1</major>
            <minor>0</minor>
            </specVersion>
            <device>
            <UDN>$uuid</UDN>
            <deviceType>$serviceFilter</deviceType>
            <friendlyName>Adnan TV</friendlyName>
            </device>
            </root>
        """.trimIndent()
        val applicationUrl = "http://appurl/"
        val location = TestUtil.getMockUrl(deviceDescription, applicationUrl)

        val foundService = ServiceDescription(
            uuid = uuid,
            serviceFilter = serviceFilter,
            ipAddress = "hostname"
        ).apply {
            port = 80
        }

        dp.discoveredServices[uuid] = foundService

        try {
            dp.getLocationData(location, uuid, serviceFilter)
        } catch (e: Exception) {
            Assert.fail(e.javaClass.simpleName)
        }
        assertFalse(dp.foundServices.isEmpty())
        Assert.assertEquals("Adnan TV", dp.foundServices[uuid]?.friendlyName)
    }

    @Test
    @Throws(JSONException::class)
    fun testServiceIdsForFilter() {
        val filter = DiscoveryFilter("DLNA", "urn:schemas-upnp-org:device:MediaRenderer:1")
        dp.serviceFilters.add(filter)
        val expectedResult = arrayListOf(filter.serviceId)
        Assert.assertEquals(expectedResult, dp.serviceIdsForFilter(filter.serviceFilter))
    }

    @Test
    @Throws(JSONException::class)
    fun testIsSearchingForFilter() {
        val filter = DiscoveryFilter("DLNA", "urn:schemas-upnp-org:device:MediaRenderer:1")
        dp.serviceFilters.add(filter)
        assertTrue(dp.isSearchingForFilter(filter.serviceFilter))
    }

    @Test
    @Throws(IOException::class)
    fun testReset() = runTest {
        assertTrue(dp.foundServices.isEmpty())
        dp.start()
        assertTrue(dp.foundServices.isEmpty())
        dp.reset()
        assertTrue(dp.foundServices.isEmpty())
    }
}
