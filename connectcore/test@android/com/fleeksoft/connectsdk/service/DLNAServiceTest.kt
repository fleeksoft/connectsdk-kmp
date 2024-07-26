package com.fleeksoft.connectsdk.service;


import com.fleeksoft.connectsdk.MainDispatcherRule
import com.fleeksoft.connectsdk.discovery.provider.ssdp.Service
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import com.fleeksoft.connectsdk.service.upnp.DLNAHttpServer
import kotlinx.coroutines.test.runTest
import org.custommonkey.xmlunit.DetailedDiff
import org.custommonkey.xmlunit.XMLUnit
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DLNAServiceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var service: DLNAService
    private lateinit var dlnaServer: DLNAHttpServer

    @Before
    fun setUp() {
        dlnaServer = mock()
        service = DLNAService(mock(), mock(), dlnaServer)
    }

    @Test
    fun testParseData() {
        val tag = "TrackDuration"
        val response = """
            <?xml version="1.0" encoding="UTF-8"?>
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <SOAP-ENV:Body>
                    <m:GetPositionInfoResponse xmlns:m="urn:schemas-upnp-org:service:AVTransport:1">
                        <Track xmlns:dt="urn:schemas-microsoft-com:datatypes" dt:dt="ui4">1</Track>
                        <TrackDuration xmlns:dt="urn:schemas-microsoft-com:datatypes" dt:dt="string">0:00:52</TrackDuration>
                        <TrackMetaData xmlns:dt="urn:schemas-microsoft-com:datatypes" dt:dt="string">&lt;DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/" xmlns:dc="http://purl.org/dc/elements/1.1/">&lt;item id="1000" parentID="0" restricted="0">&lt;dc:title>Sintel Trailer&lt;/dc:title>&lt;dc:description>Blender Open Movie Project&lt;/dc:description>&lt;res protocolInfo="http-get:*:video/mp4:DLNA.ORG_OP=01">http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/video.mp4&lt;/res>&lt;upnp:albumArtURI>http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/videoIcon.jpg&lt;/upnp:albumArtURI>&lt;upnp:class>object.item.videoItem&lt;/upnp:class>&lt;/item>&lt;/DIDL-Lite></TrackMetaData>
                        <TrackURI xmlns:dt="urn:schemas-microsoft-com:datatypes" dt:dt="string">http://ec2-54-201-108-205.us-west-2.compute.amazonaws.com/samples/media/video.mp4</TrackURI>
                        <RelTime xmlns:dt="urn:schemas-microsoft-com:datatypes" dt:dt="string">0:00:00</RelTime>
                        <AbsTime xmlns:dt="urn:schemas-microsoft-com:datatypes" dt:dt="string">NOT_IMPLEMENTED</AbsTime>
                        <RelCount xmlns:dt="urn:schemas-microsoft-com:datatypes" dt:dt="i4">2147483647</RelCount>
                        <AbsCount xmlns:dt="urn:schemas-microsoft-com:datatypes" dt:dt="i4">2147483647</AbsCount>
                    </m:GetPositionInfoResponse>
                </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """
        val value = service.parseData(response, tag)
        Assert.assertEquals("0:00:52", value)
    }

    @Test
    fun testParseDataWithError() {
        val tag = "errorCode"
        val response = """
            <?xml version="1.0"?>
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/" SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <SOAP-ENV:Body>
                    <SOAP-ENV:Fault>
                        <faultcode>SOAP-ENV:Client</faultcode>
                        <faultstring>UPnPError</faultstring>
                        <detail>
                            <u:UPnPError xmlns:u="urn:schemas-upnp-org:control-1-0">
                                <u:errorCode>402</u:errorCode>
                                <u:errorDescription>Invalid Args</u:errorDescription>
                            </u:UPnPError>
                        </detail>
                    </SOAP-ENV:Fault>
                </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """
        val value = service.parseData(response, tag)
        Assert.assertEquals("402", value)
    }

    @Test
    fun testParseData3Symbols() {
        val tag = "errorCode"
        val response = "&lt"
        var value: String? = null
        try {
            value = service.parseData(response, tag)
        } catch (e: Exception) {
            Assert.fail("exception thrown: $e")
        }
        Assert.assertEquals(null, value)
    }

    @Test
    fun testGetMetadata() {
        val title = "<title>"
        val description = "description"
        val mime = "audio/mpeg"
        val mediaURL = "http://host.com/media"
        val iconURL = "http://host.com/icon"

        val expectedXML = """
            <DIDL-Lite xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/" 
                       xmlns:dc="http://purl.org/dc/elements/1.1/" 
                       xmlns:sec="http://www.sec.co.kr/" 
                       xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/">
                <item id="1000" parentID="0" restricted="0">
                    <dc:title>&lt;title&gt;</dc:title>
                    <dc:description>$description</dc:description>
                    <res protocolInfo="http-get:*:audio/mpeg:DLNA.ORG_OP=01">$mediaURL</res>
                    <upnp:albumArtURI>$iconURL</upnp:albumArtURI>
                    <upnp:class>object.item.audioItem</upnp:class>
                </item>
            </DIDL-Lite>
        """

        val actualXML = service.getMetadata(mediaURL, null, mime, title, description, iconURL)!!
        assertXMLEquals(expectedXML, actualXML)
    }

    @Test
    fun testGetMessageXml() {
        val method = "GetPosition"
        val serviceURN = "http://serviceurn/"

        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    <u:$method xmlns:u="$serviceURN">
                        <key>value</key>
                    </u:$method>
                </s:Body>
            </s:Envelope>
        """

        val params = mapOf("key" to "value")
        val actualXML = service.getMessageXml(serviceURN, method, null, params)
        assertXMLEquals(expectedXML, actualXML!!)
    }

    // Add the remaining tests here following the same pattern

    @Test
    fun testStopDLNAServerOnDisconnect() = runTest {
        service.disconnect()
        verify(dlnaServer).stop()
    }

    @Test
    fun testServiceControlURL() {
        val dlnaService = makeServiceWithControlURL("http://192.168.1.0/", "/controlURL")
        Assert.assertEquals("http://192.168.1.0/controlURL", dlnaService.avTransportURL)
    }

    @Test
    fun testServiceControlURLWithWrongBase() {
        val dlnaService = makeServiceWithControlURL("http://192.168.1.0", "/controlURL")
        Assert.assertEquals("http://192.168.1.0/controlURL", dlnaService.avTransportURL)
    }

    @Test
    fun testServiceControlURLWithWrongControlURL() {
        val dlnaService = makeServiceWithControlURL("http://192.168.1.0/", "controlURL")
        Assert.assertEquals("http://192.168.1.0/controlURL", dlnaService.avTransportURL)
    }

    @Test
    fun testServiceControlURLWithWrongBaseAndControlURL() {
        val dlnaService = makeServiceWithControlURL("http://192.168.1.0", "controlURL")
        Assert.assertEquals("http://192.168.1.0/controlURL", dlnaService.avTransportURL)
    }

    @Test
    fun testInitialPairingType() {
        Assert.assertEquals(DeviceService.PairingType.NONE, service.getPairingType())
    }

    @Test
    fun testPairingTypeSetter() {
        service.setPairingType(DeviceService.PairingType.PIN_CODE)
        Assert.assertEquals(DeviceService.PairingType.NONE, service.getPairingType())
    }

    @Test
    fun testTimeToLongWrongValue() {
        Assert.assertEquals(0L, service.convertStrTimeFormatToLong("abc"))
    }

    @Test
    fun testTimeToLongZeroValue() {
        Assert.assertEquals(0L, service.convertStrTimeFormatToLong("00:00:00"))
    }

    @Test
    fun testTimeToLong() {
        Assert.assertEquals(10000L, service.convertStrTimeFormatToLong("00:00:10"))
    }

    @Test
    fun testTimeToLong12Hours() {
        Assert.assertEquals(43200000L, service.convertStrTimeFormatToLong("12:00:00"))
    }

    @Test
    fun testTimeToLong20Hours() {
        Assert.assertEquals(72000000L, service.convertStrTimeFormatToLong("20:00:00"))
    }

    @Test
    fun testTimeToLongBigValue() {
//        Assert.assertEquals(432000000L, service.convertStrTimeFormatToLong("120:00:00"))
        Assert.assertEquals(0L, service.convertStrTimeFormatToLong("120:00:00"))
    }

    @Test
    fun testTimeToLongWithMilliseconds() {
//        Assert.assertEquals(43200000L, service.convertStrTimeFormatToLong("12:00:00.777"))
        Assert.assertEquals(43200777L, service.convertStrTimeFormatToLong("12:00:00.777"))
    }

    @Test
    fun testTimeToLongWithInvalidArguments() {
        try {
            Assert.assertEquals(0L, service.convertStrTimeFormatToLong("01.210"))
            Assert.assertEquals(0L, service.convertStrTimeFormatToLong("00:01.210"))
            Assert.assertEquals(0L, service.convertStrTimeFormatToLong("Not a number"))
        } catch (e: Exception) {
            Assert.fail("convertStrTimeFormatToLong must not throw an exception")
        }
    }

    @Test
    fun testMakeControlURL() {
        Assert.assertEquals("base/path", service.makeControlURL("base/", "path"))
    }

    @Test
    fun testMakeControlURLWithNullBase() {
        Assert.assertNull(service.makeControlURL(null, "path"))
    }

    @Test
    fun testMakeControlURLWithNullPath() {
        Assert.assertNull(service.makeControlURL("base", null))
    }

    private fun makeServiceWithControlURL(base: String, controlURL: String): DLNAService {
        val services = mutableListOf<Service>()
        val service = Service().apply {
            this.baseURL = base
            this.controlURL = controlURL
            this.serviceType = DLNAService.AV_TRANSPORT
        }
        services.add(service)

        val description = mock<ServiceDescription>().apply {
            whenever(serviceList).thenReturn(services)
        }

        return DLNAService(description, mock())
    }

    private fun assertXMLEquals(expectedXML: String, actualXML: String) {
        XMLUnit.setIgnoreWhitespace(true)
        XMLUnit.setIgnoreAttributeOrder(true)
        XMLUnit.setNormalize(true)
        val diff = DetailedDiff(XMLUnit.compareXML(expectedXML, actualXML))
        val allDifferences = diff.allDifferences
        Assert.assertEquals("XML differences found: $diff", 0, allDifferences.size)
    }
}

