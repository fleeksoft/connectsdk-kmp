package com.fleeksoft.connectsdk.ported

import java.net.InetAddress

actual class DeviceInfo {
    actual fun getIpAddress(): String {
        return InetAddress.getLocalHost().hostAddress
    }
}