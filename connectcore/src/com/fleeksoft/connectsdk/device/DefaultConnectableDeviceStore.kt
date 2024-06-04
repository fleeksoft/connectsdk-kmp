package com.fleeksoft.connectsdk.device

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.service.DeviceService
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import korlibs.io.file.std.uniVfs
import korlibs.io.lang.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.Duration.Companion.days

/**
 * Default implementation of ConnectableDeviceStore. It stores data in a file in application
 * data directory.
 */
// TODO: may be we can replace file with preferences or database
class DefaultConnectableDeviceStore : ConnectableDeviceStore {
    /** Date (in seconds from 1970) that the ConnectableDeviceStore was created.  */
    var created: Long = 0

    /** Date (in seconds from 1970) that the ConnectableDeviceStore was last updated.  */
    var updated: Long = 0

    /** Current version of the ConnectableDeviceStore, may be necessary for migrations  */
    var version: Int = 0

    /**
     * Max length of time for a ConnectableDevice to remain in the ConnectableDeviceStore without being discovered. Default is 3 days, and modifications to this value will trigger a scan for old devices.
     */
    var maxStoreDuration: Long = 3.days.inWholeSeconds

    //    private val fileFullPath: String = File(context.filesDir, FILENAME).path
    private val fileFullPath: String = FILENAME.uniVfs.path

    private val storedDevices: ConcurrentMutableMap<String, JsonObject> = ConcurrentMutableMap()

    private val activeDevices: MutableMap<String, ConnectableDevice> = ConcurrentMutableMap()

    private var waitToWrite = false

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        // TODO: it may produce some errors if accessed before this will complete

        scope.launch {
            Util.runInBackground {
                load()
            }
        }
    }

    override suspend fun addDevice(device: ConnectableDevice) {
        if (device.getServices().isEmpty()) return

        if (!activeDevices.containsKey(device.id)) activeDevices[device.id!!] = device

        val storedDevice = getStoredDevice(device.id!!)

        if (storedDevice != null) {
            updateDevice(device)
        } else {
            storedDevices[device.id!!] = device.toJsonObject()
            store()
        }
    }

    override suspend fun removeDevice(device: ConnectableDevice) {
        activeDevices.remove(device.id)
        storedDevices.remove(device.id)

        store()
    }

    override suspend fun updateDevice(device: ConnectableDevice) {
        if (device.getServices().isEmpty()) return

        val storedDevice = getStoredDevice(device.id!!)?.toMutableMap() ?: return

        try {
            storedDevice[ConnectableDevice.KEY_LAST_IP] = JsonPrimitive(device.lastKnownIPAddress)
            storedDevice[ConnectableDevice.KEY_LAST_SEEN] = JsonPrimitive(device.lastSeenOnWifi)
            storedDevice[ConnectableDevice.KEY_LAST_CONNECTED] = JsonPrimitive(device.lastConnected)
            storedDevice[ConnectableDevice.KEY_LAST_DETECTED] = JsonPrimitive(device.lastDetection)

            val services =
                runCatching { storedDevice.getValue(ConnectableDevice.KEY_SERVICES).jsonObject }.getOrNull()
                    ?.toMutableMap() ?: mutableMapOf()

            for (service in device.getServices()) {
                val serviceInfo = service.toJsonObject()

                services[service._serviceDescription?.uuid!!] = serviceInfo
            }

            storedDevice[ConnectableDevice.KEY_SERVICES] = JsonObject(services)

            storedDevices[device.id!!] = JsonObject(storedDevice)
            activeDevices[device.id!!] = device

            store()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun removeAll() {
        activeDevices.clear()
        storedDevices.clear()

        store()
    }

    override fun getStoredDevices(): JsonObject {
        val objectEntries = mutableMapOf<String, JsonElement>()
        for ((key, value) in storedDevices) {
            try {
                objectEntries[key] = value
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return JsonObject(objectEntries)
    }

    override fun getDevice(uuid: String?): ConnectableDevice? {
        if (uuid.isNullOrEmpty()) return null

        var foundDevice = getActiveDevice(uuid)

        if (foundDevice == null) {
            val foundDeviceInfo = getStoredDevice(uuid)

            if (foundDeviceInfo != null) foundDevice = ConnectableDevice(foundDeviceInfo)
        }

        return foundDevice
    }

    private fun getActiveDevice(uuid: String): ConnectableDevice? {
        val foundDevice = activeDevices[uuid]

        if (foundDevice == null) {
            for (device in activeDevices.values) {
                for (service in device.getServices()) {
                    if (uuid == service._serviceDescription?.uuid) {
                        return device
                    }
                }
            }
        }
        return foundDevice
    }

    private fun getStoredDevice(uuid: String): JsonObject? {
        val foundDevice = storedDevices[uuid]
        if (foundDevice == null) {
            for (device in storedDevices.values) {
                val services =
                    runCatching { device.getValue(ConnectableDevice.KEY_SERVICES).jsonObject }.getOrNull()
                if (services != null && services.containsKey(uuid)) return device
            }
        }
        return foundDevice
    }

    override fun getServiceConfig(serviceDescription: ServiceDescription?): ServiceConfig? {
        if (serviceDescription == null) {
            return null
        }
        val uuid = serviceDescription.uuid
        if (uuid.isEmpty()) {
            return null
        }

        val device = getStoredDevice(uuid)
        if (device != null) {
            val services =
                runCatching { device.getValue(ConnectableDevice.KEY_SERVICES).jsonObject }.getOrNull()
            if (services != null) {
                val service = runCatching { services.getValue(uuid).jsonObject }.getOrNull()
                if (service != null) {
                    val serviceConfigInfo =
                        runCatching { service.getValue(DeviceService.KEY_CONFIG).jsonObject }.getOrNull()
                    if (serviceConfigInfo != null) {
                        return ServiceConfig.getConfig(serviceConfigInfo)
                    }
                }
            }
        }

        return null
    }

    private suspend fun load() {
        val file = fileFullPath.uniVfs

        if (!file.exists()) {
            version = CURRENT_VERSION

            created = Util.getTime()
            updated = Util.getTime()
        } else {
            var encounteredException = false

            try {

                val fileContent = file.readString()

                val data = Json.Default.decodeFromString<JsonElement>(fileContent).jsonObject
                val deviceArray = runCatching { data.getValue(KEY_DEVICES).jsonArray }.getOrNull()
                if (deviceArray != null) {
                    for (i in 0 until deviceArray.size) {
                        val device = deviceArray[i].jsonObject
                        storedDevices[device.getValue(ConnectableDevice.KEY_ID).jsonPrimitive.content] =
                            device
                    }
                }

                version = data[KEY_VERSION]?.jsonPrimitive?.intOrNull ?: CURRENT_VERSION
                created = data[KEY_CREATED]?.jsonPrimitive?.longOrNull ?: 0
                updated = data[KEY_UPDATED]?.jsonPrimitive?.longOrNull ?: 0
            } catch (e: IOException) {
                e.printStackTrace()

                // it is likely that the device store has been corrupted
                encounteredException = true
            } catch (e: Exception) {
                e.printStackTrace()

                // it is likely that the device store has been corrupted
                encounteredException = true
            }

            if (encounteredException && storedDevices.isEmpty()) {
                file.delete()

                version = CURRENT_VERSION

                created = Util.getTime()
                updated = Util.getTime()
            }
        }
    }

    private suspend fun store() {
        updated = Util.getTime()
        val deviceArray = JsonArray(storedDevices.values.map { it })

        val deviceStore = JsonObject(
            mapOf(
                KEY_VERSION to JsonPrimitive(version),
                KEY_CREATED to JsonPrimitive(created),
                KEY_UPDATED to JsonPrimitive(updated),
                KEY_DEVICES to deviceArray
            )
        )

        if (!waitToWrite) writeStoreToDisk(deviceStore)
    }

    private suspend fun writeStoreToDisk(deviceStore: JsonObject) {
        val lastUpdate = updated
        waitToWrite = true

        Util.runInBackground {
            try {
                val output = fileFullPath.uniVfs
                val parentDir = output.parent
                if (!parentDir.exists()) parentDir.mkdirs()

                output.writeString(Json.Default.encodeToString(deviceStore))

            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                waitToWrite = false
            }
            if (lastUpdate < updated) writeStoreToDisk(deviceStore)
        }
    }

    companion object {
        // @cond INTERNAL
        const val KEY_VERSION: String = "version"
        const val KEY_CREATED: String = "created"
        const val KEY_UPDATED: String = "updated"
        const val KEY_DEVICES: String = "devices"

        const val CURRENT_VERSION: Int = 0

        const val FILENAME: String = "StoredDevices"

        const val IP_ADDRESS: String = "ipAddress"
        const val FRIENDLY_NAME: String = "friendlyName"
        const val MODEL_NAME: String = "modelName"
        const val MODEL_NUMBER: String = "modelNumber"
        const val SERVICES: String = "services"
        const val DESCRIPTION: String = "description"
        const val CONFIG: String = "config"

        const val FILTER: String = "filter"
        const val UUID: String = "uuid"
        const val PORT: String = "port"

        const val SERVICE_UUID: String = "serviceUUID"
        const val CLIENT_KEY: String = "clientKey"
        const val SERVER_CERTIFICATE: String = "serverCertificate"
        const val PAIRING_KEY: String = "pairingKey"

        const val DEFAULT_SERVICE_WEBOSTV: String = "WebOSTVService"
        const val DEFAULT_SERVICE_NETCASTTV: String = "NetcastTVService"
    }
}
