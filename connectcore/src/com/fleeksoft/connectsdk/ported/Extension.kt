package com.fleeksoft.connectsdk.ported

import korlibs.io.serialization.xml.Xml
import korlibs.io.serialization.xml.isNode
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject.optString(key: String): String {
    return this[key]?.jsonPrimitive?.contentOrNull ?: ""
}

fun JsonObject.getString(key: String): String {
    return this[key]?.jsonPrimitive?.contentOrNull ?: throw Exception("$key key not found!")
}


fun Xml.findChild(tag: String): Xml? {
    this.allNodeChildren.forEach { child ->
        if (child.name == tag) {
            return child
        } else if (child.allNodeChildren.isNotEmpty()) {
            return child.findChild(tag)
        }
    }

    return null
}