package com.fleeksoft.connectsdk.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Normalized reference object for information about a DeviceService's app. This
 * object will, in most cases, be used to launch apps.
 *
 * In some cases, all that is needed to launch an app is the app id.
 */
@Serializable
data class AppInfo(val id: String, val name: String = id) : JSONSerializable {
    override fun toJsonObject(): JsonObject {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (other is AppInfo) {
            return this.id == other.id
        }
        return super.equals(other)
    }
}