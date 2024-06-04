package com.fleeksoft.connectsdk.core

import kotlinx.serialization.json.JsonObject

interface JSONSerializable {
    fun toJsonObject(): JsonObject
}
