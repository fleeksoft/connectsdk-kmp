package com.fleeksoft.connectsdk.discovery.provider.ssdp


import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.ported.multicastsocket.MulticastSocketKmp
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.text.String

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SSDPClientTest {

    private lateinit var localAddress: InetSocketAddress
    private lateinit var ssdpClient: SSDPClient

    private val testDataBytes = "hello world".toByteArray()
    private val wildSocket: BoundDatagramSocket = mock<BoundDatagramSocket> {
        on { runBlocking { receive() } } doReturn Datagram(ByteReadPacket(testDataBytes), InetSocketAddress("0", 0))
    }
    private val mLocalSocket: MulticastSocketKmp = mock<MulticastSocketKmp> {
        on { runBlocking { receive() } } doReturn Datagram(ByteReadPacket(testDataBytes), InetSocketAddress("0", 0))
    }

    @Before
    fun setUp() {
        localAddress = InetSocketAddress(Util.getIpAddress(), 0)
        ssdpClient = SSDPClient(localAddress, mLocalSocket, wildSocket)
    }

    @Test
    fun testSend() = runTest {
        // Verify is ssdpClient.send() is sending correct SSDP packet to DatagramSocket

        val stringData = "some data"
        val datagram = Datagram(
            ByteReadPacket(stringData.toByteArray()),
            InetSocketAddress("239.255.255.250", 1900)
        )
        ssdpClient.send(stringData)

        val argument = argumentCaptor<Datagram>()
        verify(wildSocket).send(argument.capture())


        assertEquals(datagram.address, argument.firstValue.address)
        assertEquals(String(datagram.packet.readBytes()), String(argument.firstValue.packet.readBytes()))
    }

    @Test
    fun testResponseReceive() = runTest {
        // Verify is ssdpClient.responseReceive() receive SSDP Response packet to DatagramSocket

        val response = ssdpClient.responseReceive()

        verify(wildSocket).receive()
        val responseBytes = response.packet.readBytes()
        assertEquals(testDataBytes.size, responseBytes.size)
        assertEquals(String(testDataBytes), String(responseBytes))
    }

    @Test
    fun testNotifyReceive() = runTest {
        // Verify is ssdpClient.NotifyReceive() receive SSDP Notify packet to DatagramSocket.

        val response = ssdpClient.multicastReceive()

        verify(mLocalSocket).receive()
        val responseBytes = response.packet.readBytes()
        assertEquals(testDataBytes.size, responseBytes.size)
        assertEquals(String(testDataBytes), String(responseBytes))
    }

    /*@Test
    fun testClose() = runTest {
        wildSocket.connect(localAddress, 1903)
        mLocalSocket.connect(localAddress, 1904)
        ssdpClient.close()

        verify(mLocalSocket, Mockito.times(1))
            .leaveGroup(any(), any())
        verify(mLocalSocket, Mockito.times(1)).close()
        verify(wildSocket, Mockito.times(1)).close()
    }

    @Test
    fun testSetTimeout() {
        val testTimeout = 1000
        ssdpClient.timeout = testTimeout

        val argument = argumentCaptor<Int>()
        verify(wildSocket, Mockito.times(1)).setSoTimeout(argument.capture())
        assertEquals(testTimeout, argument.firstValue)
    }*/
}

