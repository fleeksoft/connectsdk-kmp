package com.fleeksoft.connectsdk.ported

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject.optString(key: String): String {
    return this[key]?.jsonPrimitive?.contentOrNull ?: ""
}

fun JsonObject.getString(key: String): String {
    return this[key]?.jsonPrimitive?.contentOrNull ?: throw Exception("$key key not found!")
}
