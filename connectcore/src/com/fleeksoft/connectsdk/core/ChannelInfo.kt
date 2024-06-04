package com.fleeksoft.connectsdk.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Normalized reference object for information about a TVs channels. This object is required to set the channel on a TV.
 */
@Serializable
data class ChannelInfo(
    var name: String? = null,
    /** TV's unique ID for the channel  */
    var id: String? = null,
    /** TV channel's number (likely to be a combination of the major & minor numbers)  */
    var number: String? = null,
    /** TV channel's minor number  */
    var minorNumber: Int = 0,
    /** TV channel's major number  */
    var majorNumber: Int = 0,
): JSONSerializable {
    override fun toJsonObject(): JsonObject {
        TODO("Not yet implemented")
    }

    override fun equals(other: Any?): Boolean {
        if (other is ChannelInfo) {
            if (this.id != null) {
                if (this.id == other.id) return true
            } else if (this.name != null && this.number != null) {
                return this.name == other.name && (this.number == other.number) && (this.majorNumber == other.majorNumber
                        ) && (this.minorNumber == other.minorNumber)
            }
            return false
        }
        return super.equals(other)
    }
}