package com.fleeksoft.connectsdk.core

import kotlinx.serialization.json.JsonObject

interface JSONDeserializable {
    fun fromJsonObject(obj: JsonObject)
}
