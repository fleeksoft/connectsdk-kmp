package com.fleeksoft.connectsdk.ported

import ContextUtil
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties

actual class DeviceInfo {
    actual fun getIpAddress(): String {
        val context: Context = ContextUtil.context
        val connectivityManager =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val link: LinkProperties =
            connectivityManager.getLinkProperties(connectivityManager.activeNetwork) as LinkProperties
        return link.linkAddresses.toString()
    }
}