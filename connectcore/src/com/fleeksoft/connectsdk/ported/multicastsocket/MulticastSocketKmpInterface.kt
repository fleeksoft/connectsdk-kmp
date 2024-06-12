package com.fleeksoft.connectsdk.ported.multicastsocket

import io.ktor.network.sockets.*


interface MulticastSocketKmpInterface {
    val port: Int
    suspend fun joinGroup(group: InetSocketAddress, interfaceIp: String)
    suspend fun leaveGroup(group: InetSocketAddress, interfaceIp: String)
    suspend fun receive(): Datagram
    suspend fun isConnected(): Boolean
    suspend fun close()
    suspend fun disconnect()
}