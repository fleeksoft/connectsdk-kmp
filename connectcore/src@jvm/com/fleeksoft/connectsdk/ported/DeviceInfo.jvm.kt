package com.fleeksoft.connectsdk.ported

import java.net.InetAddress
import java.net.NetworkInterface

actual class DeviceInfo {
    actual fun getIpAddress(): String {
        return getJvmIpAddress()
    }


    private fun getJvmIpAddress(): String{
        val sortedInterfaces = NetworkInterface.getNetworkInterfaces().toList().sortedBy { it.index }

        for (anInterface in sortedInterfaces) {
            for (address in anInterface.inetAddresses) {
                if (!address.isLoopbackAddress && address.isSiteLocalAddress) {
                    return address.hostAddress
                }
            }
        }
        return ""
    }

}