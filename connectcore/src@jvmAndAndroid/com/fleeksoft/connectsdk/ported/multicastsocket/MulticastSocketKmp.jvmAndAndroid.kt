package com.fleeksoft.connectsdk.ported.multicastsocket

import com.fleeksoft.connectsdk.ported.toDatagram
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

actual class MulticastSocketKmp actual constructor(override val port: Int) : MulticastSocketKmpInterface {
    private val networkInterfacesMap: MutableMap<String, NetworkInterface> = mutableMapOf()
    private val socket = MulticastSocket(port)
    override suspend fun joinGroup(group: InetSocketAddress, interfaceIp: String) = withContext(Dispatchers.IO) {
        val networkInterface = NetworkInterface.getByInetAddress(InetAddress.getByAddress(interfaceIp.toByteArray()))
        networkInterfacesMap[interfaceIp] = networkInterface
        socket.joinGroup(group.toJavaAddress(), networkInterface)
    }

    override suspend fun leaveGroup(group: InetSocketAddress, interfaceIp: String) = withContext(Dispatchers.IO) {
        val networkInterface = networkInterfacesMap[interfaceIp]
            ?: NetworkInterface.getByInetAddress(InetAddress.getByAddress(interfaceIp.toByteArray()))
        networkInterfacesMap.remove(interfaceIp)
        socket.leaveGroup(group.toJavaAddress(), networkInterface)
    }

    override suspend fun receive(): Datagram = withContext(Dispatchers.IO) {
        val byteArray = ByteArray(1024)
        val datagramPacket = DatagramPacket(byteArray, byteArray.size)
        socket.receive(datagramPacket)
        datagramPacket.toDatagram()
    }

    override suspend fun isConnected(): Boolean {
        return socket.isConnected
    }

    override suspend fun close() {
        networkInterfacesMap.clear()
        socket.close()
    }
}