package com.fleeksoft.connectsdk.service.upnp

import korlibs.io.serialization.xml.Xml
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.readStringz
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DLNANotifyParser {

    suspend fun parse(inputStream: AsyncStream): JsonArray {
        try {
            val xml = Xml.parse(inputStream.readStringz())
            return readPropertySet(xml)
        } finally {
            inputStream.close()
        }
    }

    private fun readPropertySet(xml: Xml): JsonArray {
        require(xml.name == "e:propertyset") { "Expected e:propertyset, found ${xml.name}" }
        val jsonElements = mutableSetOf<JsonElement>()

        for (child in xml.allChildren) {
            when (child.name) {
                "e:property" -> {
                    jsonElements.add(readProperty(child))
                }

                else -> {
                    skip(xml)
                }
            }
        }

        return JsonArray(jsonElements.toList())
    }

    private fun readProperty(xml: Xml): JsonObject {
        var property = mutableMapOf<String, JsonElement>()

        require(xml.name == "e:property") { "Expected e:property, found ${xml.name}" }
        for (child in xml.allChildren) {
            when (child.name) {
                "LastChange" -> {
                    val eventStr = readText(xml)

                    var event: JsonObject

                    val eventParser = DLNAEventParser()

                    event = eventParser.parse(eventStr)
                    property["LastChange"] = event
                }

                else -> {
                    property = readPropertyData(xml.name, xml).toMutableMap()
                }
            }
        }
        return JsonObject(property)
    }

    private fun readPropertyData(target: String, xml: Xml): JsonObject {
        val data = mutableMapOf<String, JsonElement>()
        require(xml.name == target) { "Expected $target, found ${xml.name}" }

        val value = readText(xml)
        data[target] = JsonPrimitive(value)

        return JsonObject(data)
    }

    private fun readText(xml: Xml): String {

        if (xml.type == Xml.Type.TEXT) {
            return xml.text
        }

        return ""
    }

    //    TODO: remove it
    private fun skip(xml: Xml) {
        /*var depth = 1
        while (depth != 0) {
            when (xml.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }*/
    }

    companion object {
        private val ns: String? = null
    }
}
