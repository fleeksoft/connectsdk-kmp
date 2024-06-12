package com.fleeksoft.connectsdk.ported.multicastsocket

import io.ktor.network.sockets.*

actual class MulticastSocketKmp actual constructor(override val port: Int) : MulticastSocketKmpInterface {
    override suspend fun joinGroup(group: InetSocketAddress, interfaceIp: String) {
        TODO("Not yet implemented")
    }

    override suspend fun leaveGroup(group: InetSocketAddress, interfaceIp: String) {
        TODO("Not yet implemented")
    }

    override suspend fun receive(): Datagram {
        TODO("Not yet implemented")
    }

    override suspend fun isConnected(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun close() {
        TODO("Not yet implemented")
    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }

}