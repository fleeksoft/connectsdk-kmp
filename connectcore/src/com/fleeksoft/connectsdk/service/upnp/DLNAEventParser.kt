package com.fleeksoft.connectsdk.service.upnp

import korlibs.io.serialization.xml.Xml
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DLNAEventParser {
    fun parse(str: String): JsonObject {
        val xml = Xml(str)
        return readEvent(xml)
    }

    private fun readEvent(xml: Xml): JsonObject {
        val event = mutableMapOf<String, JsonElement>()

        val instanceIDs = mutableListOf<JsonElement>()
        val queueIDs = mutableListOf<JsonElement>()

        require(xml.name == "EVENT") { "Expected EVENT, found ${xml.name}" }
        for (child in xml.allChildren) {
            val name = xml.name
            when (name) {
                "InstanceID" -> {
                    instanceIDs.add(readInstanceID(xml))
                }

                "QueueID" -> {
                    queueIDs.add(readQueueID(xml))
                }

                else -> {
                    skip(xml)
                }
            }
        }

        if (instanceIDs.isNotEmpty()) event["InstanceID"] = JsonArray(instanceIDs)
        if (queueIDs.isNotEmpty()) event["QueueID"] = JsonArray(queueIDs)

        return JsonObject(event)
    }

    private fun readInstanceID(xml: Xml): JsonArray {
        val instanceIDs = mutableListOf<JsonElement>()
        val data = mutableMapOf<String, JsonElement>()

        require(xml.name == "InstanceID") { "Expected InstanceID, but found ${xml.name}" }

        data["value"] = JsonPrimitive(xml.attribute("val"))
        instanceIDs.add(JsonObject(data))

        xml.allChildren.forEach { child ->
            val name = child.name
            instanceIDs.add(readEntry(name, child))
        }

        return JsonArray(instanceIDs)
    }

    private fun readQueueID(xml: Xml): JsonArray {
        val queueIDs = mutableListOf<JsonElement>()
        val data = mutableMapOf<String, JsonElement>()

        require(xml.name == "QueueID") { "Expected QueueID, but found ${xml.name}" }

        data["value"] = JsonPrimitive(xml.attribute("val"))
        queueIDs.add(JsonObject(data))

        xml.allChildren.forEach { child ->
            val name = child.name
            queueIDs.add(readEntry(name, child))
        }

        return JsonArray(queueIDs)
    }

    private fun readEntry(target: String, xml: Xml): JsonObject {
        require(xml.name == target) { "Expected $target, but found ${xml.name}" }
        val value = xml.attribute("val")
        val channel = xml.attribute("channel")

        val objData = mutableMapOf(target to JsonPrimitive(value))

        if (channel != null) objData["channel"] = JsonPrimitive(channel)

        return JsonObject(objData)
    }

    //    TODO: remove it
    private fun skip(xml: Xml) {
        /*check(parser.eventType == XmlPullParser.START_TAG)
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }*/
    }

    companion object {
        private val ns: String? = null
    }
}
