package com.fleeksoft.connectsdk.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ProgramList(var channel: ChannelInfo?, var programList: JsonArray?) : JSONSerializable {
    override fun toJsonObject(): JsonObject {
        return JsonObject(
            mapOf(
                "channel" to JsonPrimitive(if (channel != null) channel.toString() else null),
                "programList" to JsonPrimitive(if (programList != null) programList.toString() else null)
            )
        )
    }
}