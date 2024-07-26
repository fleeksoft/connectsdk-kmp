package com.fleeksoft.connectsdk.ported.multicastsocket

import io.ktor.network.sockets.*
import io.ktor.util.network.*
import io.ktor.utils.io.core.*
import java.net.DatagramPacket


fun DatagramPacket.toDatagram(): Datagram {
    return Datagram(ByteReadPacket(this.data), InetSocketAddress(this.socketAddress.hostname, this.socketAddress.port))
}

