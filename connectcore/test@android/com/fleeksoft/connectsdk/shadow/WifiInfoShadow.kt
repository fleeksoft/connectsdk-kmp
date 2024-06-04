package com.fleeksoft.connectsdk.shadow;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowWifiInfo;

import android.net.wifi.WifiInfo;

@Implements(WifiInfo::class)
class WifiInfoShadow : ShadowWifiInfo() {

    fun getIpAddress(): Int {
        return try {
            val addr = InetAddress.getLocalHost().address
            (addr[0].toInt() and 0xFF) or
                ((addr[1].toInt() and 0xFF) shl 8) or
                ((addr[2].toInt() and 0xFF) shl 16) or
                ((addr[3].toInt() and 0xFF) shl 24)
        } catch (e: UnknownHostException) {
            e.printStackTrace()
            0
        }
    }
}
