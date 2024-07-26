package com.fleeksoft.connectsdk.ported

import ContextUtil
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.wifi.WifiManager

actual class DeviceInfo {
    /*actual fun getIpAddress(): String {
        val context: Context = ContextUtil.context
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val link: LinkProperties =
            connectivityManager.getLinkProperties(connectivityManager.activeNetwork) as LinkProperties
        return link.linkAddresses.toString()
    }*/

    actual fun getIpAddress(): String {
        val context: Context = ContextUtil.context
        /*val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip = wifiManager.connectionInfo.ipAddress*/
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val link: LinkProperties =
            connectivityManager.getLinkProperties(connectivityManager.activeNetwork) as LinkProperties
        return link.linkAddresses.find { !it.address.isLoopbackAddress && it.address.isSiteLocalAddress }?.address?.hostAddress
            ?: ""
    }
}