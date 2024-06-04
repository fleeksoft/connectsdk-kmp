package com.fleeksoft.connectsdk.service.config

import com.fleeksoft.connectsdk.discovery.provider.ssdp.Service
import kotlinx.serialization.json.*

open class ServiceDescription {
    lateinit var uuid: String
    var ipAddress: String = ""
    var friendlyName: String? = null
    var modelName: String? = null
    var modelNumber: String? = null
    var manufacturer: String? = null
    var modelDescription: String? = null
    var serviceFilter: String? = null
    var port: Int = 0
    var applicationURL: String? = null
    var version: String? = null
    var serviceList: List<Service>? = null
    var locationXML: String? = null
    var serviceURI: String? = null
    var responseHeaders: Map<String, List<String>>? = null
    var serviceID: String? = null
    var device: Any? = null

    var lastDetection: Long = Long.MAX_VALUE

    constructor()

    constructor(serviceFilter: String?, uuid: String, ipAddress: String) {
        this.serviceFilter = serviceFilter
        this.uuid = uuid
        this.ipAddress = ipAddress
    }

    constructor(json: JsonObject) {
        serviceFilter = json[KEY_FILTER]?.jsonPrimitive?.contentOrNull
        ipAddress = json[KEY_IP_ADDRESS]!!.jsonPrimitive.content
        uuid = json.getValue(KEY_UUID).jsonPrimitive.content
        friendlyName = json[KEY_FRIENDLY]?.jsonPrimitive?.contentOrNull
        modelName = json[KEY_MODEL_NAME]?.jsonPrimitive?.contentOrNull
        modelNumber = json[KEY_MODEL_NUMBER]?.jsonPrimitive?.contentOrNull
        port = json[KEY_PORT]?.jsonPrimitive?.intOrNull ?: -1
        version = json[KEY_VERSION]?.jsonPrimitive?.contentOrNull
        serviceID = json[KEY_SERVICE_ID]?.jsonPrimitive?.contentOrNull
    }

    fun toJsonObject(): JsonObject {
        val jsonObj = JsonObject(
            mapOf(
                KEY_FILTER to JsonPrimitive(serviceFilter),
                KEY_IP_ADDRESS to JsonPrimitive(ipAddress),
                KEY_UUID to JsonPrimitive(uuid),
                KEY_FRIENDLY to JsonPrimitive(friendlyName),
                KEY_MODEL_NAME to JsonPrimitive(modelName),
                KEY_MODEL_NUMBER to JsonPrimitive(modelNumber),
                KEY_PORT to JsonPrimitive(port),
                KEY_VERSION to JsonPrimitive(version),
                KEY_SERVICE_ID to JsonPrimitive(serviceID),
            )
        )

        return jsonObj
    }

    fun clone(): ServiceDescription {
        val service = ServiceDescription()
        service.port = port

        // we can ignore all these NullPointerExceptions, it's OK if those properties don't have values
        try {
            service.serviceID = serviceID
        } catch (ex: NullPointerException) {
        }
        try {
            service.ipAddress = ipAddress
        } catch (ex: NullPointerException) {
        }
        try {
            service.uuid = uuid
        } catch (ex: NullPointerException) {
        }
        try {
            service.version = version
        } catch (ex: NullPointerException) {
        }
        try {
            service.friendlyName = friendlyName
        } catch (ex: NullPointerException) {
        }
        try {
            service.manufacturer = manufacturer
        } catch (ex: NullPointerException) {
        }
        try {
            service.modelName = modelName
        } catch (ex: NullPointerException) {
        }
        try {
            service.modelNumber = modelNumber
        } catch (ex: NullPointerException) {
        }
        try {
            service.modelDescription = modelDescription
        } catch (ex: NullPointerException) {
        }
        try {
            service.applicationURL = applicationURL
        } catch (ex: NullPointerException) {
        }
        try {
            service.locationXML = locationXML
        } catch (ex: NullPointerException) {
        }
        try {
            service.responseHeaders = responseHeaders
        } catch (ex: NullPointerException) {
        }
        try {
            service.serviceList = serviceList
        } catch (ex: NullPointerException) {
        }
        try {
            service.serviceFilter = serviceFilter
        } catch (ex: NullPointerException) {
        }

        return service
    }

    companion object {
        const val KEY_FILTER: String = "filter"
        const val KEY_IP_ADDRESS: String = "ipAddress"
        const val KEY_UUID: String = "uuid"
        const val KEY_FRIENDLY: String = "friendlyName"
        const val KEY_MODEL_NAME: String = "modelName"
        const val KEY_MODEL_NUMBER: String = "modelNumber"
        const val KEY_PORT: String = "port"
        const val KEY_VERSION: String = "version"
        const val KEY_SERVICE_ID: String = "serviceId"

        fun getDescription(json: JsonObject): ServiceDescription {
            return ServiceDescription(json)
        }
    }
}
