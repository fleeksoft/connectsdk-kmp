/*
 * SSDPClient
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 6 Jan 2015
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fleeksoft.connectsdk.discovery.provider.ssdp

import com.fleeksoft.connectsdk.ported.multicastsocket.MulticastSocketKmp
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.*
import korlibs.io.lang.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking

class SSDPClient constructor(
    var localInAddress: InetSocketAddress,
    var multicastSocket: MulticastSocketKmp = MulticastSocketKmp(PORT),
    var datagramSocket: BoundDatagramSocket = aSocket(SelectorManager(Dispatchers.IO)).udp()
        .bind(localInAddress),
) {
    var timeout: Int = 0


    init {
        runBlocking {
            multicastSocket.joinGroup(defaultMulticastGroup, localInAddress.hostname)
        }
    }

    /** Used to send SSDP packet  */
    suspend fun send(data: String) {
        val dp = Datagram(ByteReadPacket(data.toByteArray()), defaultMulticastGroup)
        datagramSocket.send(dp)
    }


    /** Used to receive SSDP Response packet  */
    suspend fun responseReceive(): Datagram {
        return datagramSocket.receive()
    }

    /** Used to receive SSDP Multicast packet  */
    suspend fun multicastReceive(): Datagram {
        return multicastSocket.receive()
    }

    //    /** Starts the socket */
    //    public void start() {
    //    
    //    }
    suspend fun isConnected(): Boolean {
        return !datagramSocket.isClosed && multicastSocket.isConnected()
    }

    /** Close the socket  */
    suspend fun close() {
        runCatching {
            multicastSocket.leaveGroup(defaultMulticastGroup, localInAddress.hostname)
        }.onFailure { it.printStackTrace() }
        multicastSocket.close()

        datagramSocket.close()
    }

    companion object {
        /* New line definition */
        const val NEWLINE: String = "\r\n"

        const val MULTICAST_ADDRESS: String = "239.255.255.250"
        const val PORT: Int = 1900
        val defaultMulticastGroup: InetSocketAddress = InetSocketAddress(MULTICAST_ADDRESS, PORT)

        /* Definitions of start line */
        const val NOTIFY: String = "NOTIFY * HTTP/1.1"
        const val MSEARCH: String = "M-SEARCH * HTTP/1.1"
        const val OK: String = "HTTP/1.1 200 OK"

        /* Definitions of search targets */ //    public static final String DEVICE_MEDIA_SERVER_1 = "urn:schemas-upnp-org:device:MediaServer:1"; 
        //    public static final String SERVICE_CONTENT_DIRECTORY_1 = "urn:schemas-upnp-org:service:ContentDirectory:1";
        //    public static final String SERVICE_CONNECTION_MANAGER_1 = "urn:schemas-upnp-org:service:ConnectionManager:1";
        //    public static final String SERVICE_AV_TRANSPORT_1 = "urn:schemas-upnp-org:service:AVTransport:1";
        //    
        //    public static final String ST_ContentDirectory = ST + ":" + UPNP.SERVICE_CONTENT_DIRECTORY_1;
        /* Definitions of notification sub type */
        const val ALIVE: String = "ssdp:alive"
        const val BYEBYE: String = "ssdp:byebye"
        const val UPDATE: String = "ssdp:update"

        private var MX: Int = 5

        fun getSSDPSearchMessage(ST: String?): String {
            val sb: StringBuilder = StringBuilder()

            sb.append(MSEARCH + NEWLINE)
            sb.append("HOST: $MULTICAST_ADDRESS:$PORT$NEWLINE")
            sb.append("MAN: \"ssdp:discover\"$NEWLINE")
            sb.append("ST: ").append(ST).append(NEWLINE)
            sb.append("MX: ").append(MX).append(NEWLINE)
            if (ST!!.contains("udap")) {
                sb.append("USER-AGENT: UDAP/2.0$NEWLINE")
            }
            sb.append(NEWLINE)

            return sb.toString()
        }
    }
}
