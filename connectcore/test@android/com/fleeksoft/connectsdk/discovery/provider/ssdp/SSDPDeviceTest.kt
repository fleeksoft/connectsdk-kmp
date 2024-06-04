package com.fleeksoft.connectsdk.discovery.provider.ssdp

import com.fleeksoft.connectsdk.core.TestUtil
import com.fleeksoft.connectsdk.helper.NetworkHelper
import com.fleeksoft.connectsdk.provideHttpClientEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.net.UnknownHostException

class SSDPDeviceTest {

    init {
        NetworkHelper.init(provideHttpClientEngine())
    }

    private val deviceDescription = """
        <root xmlns="urn:schemas-upnp-org:device-1-0" xmlns:dlna="urn:schemas-dlna-org:device-1-0">
        <specVersion>
        <major>1</major>
        <minor>0</minor>
        </specVersion>
        <device>
        <deviceType>urn:schemas-upnp-org:device:Basic:1</deviceType>
        <friendlyName>Adnan TV</friendlyName>
        <manufacturer>LG Electronics</manufacturer>
        <manufacturerURL>http://www.lge.com</manufacturerURL>
        <modelDescription/>
        <modelName>LG Smart TV</modelName>
        <modelURL>http://www.lge.com</modelURL>
        <modelNumber>WEBOS1</modelNumber>
        <serialNumber/>
        <UDN>uuid:86ea12c3-4ad7-2117-edbd-8177429fe21e</UDN>
            <serviceList>
                <service>
                    <serviceType>urn:lge-com:service:webos-second-screen:1</serviceType>
                    <serviceId>
                    urn:lge-com:serviceId:webos-second-screen-3000-3001
                    </serviceId>
                    <SCPDURL>
                    /WebOS_SecondScreen/86ea12c3-4ad7-2117-edbd-8177429fe21e/scpd.xml
                    </SCPDURL>
                    <controlURL>
                    /WebOS_SecondScreen/86ea12c3-4ad7-2117-edbd-8177429fe21e/control.xml
                    </controlURL>
                    <eventSubURL>
                    /WebOS_SecondScreen/86ea12c3-4ad7-2117-edbd-8177429fe21e/event.xml
                    </eventSubURL>
                </service>
            </serviceList>
        </device>
        </root>
    """.trimIndent()

    private val deviceSmallDescription = """
        <root xmlns="urn:schemas-upnp-org:device-1-0" xmlns:dlna="urn:schemas-dlna-org:device-1-0">
        <specVersion>
        <major>1</major>
        <minor>0</minor>
        </specVersion>
        <device>
        <deviceType>urn:schemas-upnp-org:device:Basic:1</deviceType>
        </device>
        </root>
    """.trimIndent()

    @Test
    fun testCreateDeviceWithWrongUrl() = runTest {
        try {
            SSDPDevice.parse("http://unknown.host", null)
            Assert.fail("IllegalArgumentException should be thrown")
        } catch (e: UnknownHostException) {
            // OK
        } catch (e: Exception) {
            Assert.fail("MalformedURLException should be thrown but was ${e.cause}")
        }
    }

    @Test
    fun testCreateDeviceFromPlainTextContent() = runTest {
        try {
            val mockUrl = TestUtil.getMockUrl("plain text", null)
            SSDPDevice.parse(mockUrl, null)
            Assert.fail("IllegalArgumentException should be thrown")
        } catch (e: IllegalArgumentException) {
            // OK
        } catch (e: Exception) {
            Assert.fail("IllegalArgumentException should be thrown but ${e.cause}")
        }
    }

    @Test
    fun testCreateDeviceFrom() = runTest {
        val mockUrl = TestUtil.getMockUrl(deviceDescription, "http://application_url/")
        val device = SSDPDevice.parse(mockUrl, null)
        Assert.assertEquals("urn:schemas-upnp-org:device:Basic:1", device.deviceType)
        Assert.assertEquals("Adnan TV", device.friendlyName)
        Assert.assertEquals("LG Electronics", device.manufacturer)
        Assert.assertEquals(device.modelDescription?.isEmpty(), true)
        Assert.assertEquals(deviceDescription, device.locationXML)
        Assert.assertEquals("http://application_url/", device.applicationURL)
        Assert.assertEquals("hostname", device.ipAddress)
        Assert.assertEquals(80, device.port)
        Assert.assertEquals("http://hostname", device.serviceURI)
        Assert.assertEquals("http://hostname:80", device.baseURL)
        Assert.assertEquals("WEBOS1", device.modelNumber)
    }

    @Test
    fun testCreateDeviceFromSmallDescription() = runTest {
        val mockUrl = TestUtil.getMockUrl(deviceSmallDescription, "http://application_url")
        val device = SSDPDevice.parse(mockUrl, null)
        Assert.assertEquals("urn:schemas-upnp-org:device:Basic:1", device.deviceType)
        Assert.assertNull(device.friendlyName)
        Assert.assertNull(device.manufacturer)
        Assert.assertNull(device.modelDescription)
        Assert.assertEquals(deviceSmallDescription, device.locationXML)
        Assert.assertEquals("http://application_url/", device.applicationURL)
        Assert.assertEquals("hostname", device.ipAddress)
        Assert.assertEquals(80, device.port)
        Assert.assertEquals("http://hostname", device.serviceURI)
        Assert.assertEquals("http://hostname:80", device.baseURL)
        Assert.assertNull(device.modelNumber)
    }
}
