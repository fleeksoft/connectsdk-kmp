package com.fleeksoft.connectsdk.discovery.provider.ssdp;

import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.text.toByteArray

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SSDPPacketTest {

    private lateinit var mDatagramPacket: Datagram
    private lateinit var ssdpPacket: SSDPPacket

    @Before
    fun setUp() {
        val testDatagramData = ""
        mDatagramPacket = Datagram(ByteReadPacket(testDatagramData.toByteArray()), InetSocketAddress("0", 0))
    }

    @Test
    fun testParseDatagram() {
        val testDatagramData =
            "NOTIFY * HTTP/1.1\r\n" +
                "HOST: 239.255.255.250:1900\r\n" +
                "NT: nt_value\r\n" +
                "NTS: ssdp:byebye\r\n" +
                "USN: uuid:advertisement_UUID\r\n\r\n"
        mDatagramPacket = Datagram(ByteReadPacket(testDatagramData.toByteArray()), InetSocketAddress("0", 0))
        ssdpPacket = SSDPPacket(mDatagramPacket)
        assertEquals("NOTIFY * HTTP/1.1", ssdpPacket.getType())
        assertEquals("239.255.255.250:1900", ssdpPacket.getData()["HOST"])
        assertEquals("nt_value", ssdpPacket.getData()["NT"])
        assertEquals("ssdp:byebye", ssdpPacket.getData()["NTS"])
        assertEquals("uuid:advertisement_UUID", ssdpPacket.getData()["USN"])
    }

    @Test
    fun testParseLowercaseDatagram() {
        val testDatagramData =
            "NOTIFY * HTTP/1.1\r\n" +
                "host: 239.255.255.250:1900\r\n" +
                "nt: nt_value\r\n" +
                "Nts: ssdp:byebye\r\n" +
                "uSN: uuid:advertisement_UUID\r\n\r\n"
        mDatagramPacket = Datagram(ByteReadPacket(testDatagramData.toByteArray()), InetSocketAddress("0", 0))
        ssdpPacket = SSDPPacket(mDatagramPacket)
        assertEquals("NOTIFY * HTTP/1.1", ssdpPacket.getType())
        assertEquals("239.255.255.250:1900", ssdpPacket.getData()["HOST"])
        assertEquals("nt_value", ssdpPacket.getData()["NT"])
        assertEquals("ssdp:byebye", ssdpPacket.getData()["NTS"])
        assertEquals("uuid:advertisement_UUID", ssdpPacket.getData()["USN"])
    }

    @Test
    fun testParseEmptyDatagram() {
        val testDatagramData = "Unknown"
        mDatagramPacket = Datagram(ByteReadPacket(testDatagramData.toByteArray()), InetSocketAddress("0", 0))
        ssdpPacket = SSDPPacket(mDatagramPacket)
        assertNull(ssdpPacket.getType())
        assertTrue(ssdpPacket.getData().isEmpty())
    }

    @Test
    fun testParseGarbage() {
        val testDatagramData =
            """\n\r\n\r\r\n\n\r\n\r\n::::sdkfjh::\r\n:\r\n::\r\nKEY:\r\nsdf\r\n\u000E¾<ƒÄ\f^‹Ã_Ã\u0007Ì\u0001ÜLÿ›îÿ$\u0004‰P\u0004‹T$\b‰H\u001B\f\f‰\r\n"""
        try {
            mDatagramPacket = Datagram(ByteReadPacket(testDatagramData.toByteArray()), InetSocketAddress("0", 0))
            ssdpPacket = SSDPPacket(mDatagramPacket)
            assertNull(ssdpPacket.getData()["KEY"])
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    @Test
    fun testParseDatagramWithoutLineEnd() {
        val testDatagramData = "key:value"
        mDatagramPacket = Datagram(ByteReadPacket(testDatagramData.toByteArray()), InetSocketAddress("0", 0))
        ssdpPacket = SSDPPacket(mDatagramPacket)
        assertEquals(null, ssdpPacket.getData()["key"])
    }
}
