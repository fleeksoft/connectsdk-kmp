package com.fleeksoft.connectsdk.device

import co.touchlab.kermit.Logger
import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import com.benasher44.uuid.uuid4
import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.discovery.DiscoveryManager
import com.fleeksoft.connectsdk.service.DeviceService
import com.fleeksoft.connectsdk.service.DeviceService.DeviceServiceListener
import com.fleeksoft.connectsdk.service.DeviceService.PairingType
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import kotlinx.serialization.json.*
import kotlin.reflect.KClass

/**
 * ###Overview
 * ConnectableDevice serves as a normalization layer between your app and each of the device's services. It consolidates a lot of key data about the physical device and provides access to underlying functionality.
 *
 * ###In Depth
 * ConnectableDevice consolidates some key information about the physical device, including model name, friendly name, ip address, connected DeviceService names, etc. In some cases, it is not possible to accurately select which DeviceService has the best friendly name, model name, etc. In these cases, the values of these properties are dependent upon the order of DeviceService discovery.
 *
 * To be informed of any ready/pairing/disconnect messages from each of the DeviceService, you must set a listener.
 *
 * ConnectableDevice exposes capabilities that exist in the underlying DeviceServices such as TV Control, Media Player, Media Control, Volume Control, etc. These capabilities, when accessed through the ConnectableDevice, will be automatically chosen from the most suitable DeviceService by using that DeviceService's CapabilityPriorityLevel.
 */
class ConnectableDevice() : DeviceServiceListener {
    /** Gets the Current IP address of the ConnectableDevice.  */
    /**
     * Sets the IP address of the ConnectableDevice.
     *
     * @param ipAddress IP address of the ConnectableDevice
     */
    var ipAddress: String? = null
    /** Gets an estimate of the ConnectableDevice's current friendly name.  */
    /**
     * Sets an estimate of the ConnectableDevice's current friendly name.
     *
     * @param friendlyName Friendly name of the device
     */
    var friendlyName: String? = null
    /** Gets an estimate of the ConnectableDevice's current model name.  */
    /**
     * Sets an estimate of the ConnectableDevice's current model name.
     *
     * @param modelName Model name of the ConnectableDevice
     */
    var modelName: String? = null
    /** Gets an estimate of the ConnectableDevice's current model number.  */
    /**
     * Sets an estimate of the ConnectableDevice's current model number.
     *
     * @param modelNumber Model number of the ConnectableDevice
     */
    var modelNumber: String? = null

    /** Gets the last IP address this ConnectableDevice was discovered at.  */
    /**
     * Sets the last IP address this ConnectableDevice was discovered at.
     *
     * @param lastKnownIPAddress Last known IP address of the device & it's services
     */
    var lastKnownIPAddress: String? = null
    /** Gets the name of the last wireless network this ConnectableDevice was discovered on.  */
    /**
     * Sets the name of the last wireless network this ConnectableDevice was discovered on.
     *
     * @param lastSeenOnWifi Last Wi-Fi network this device & it's services were discovered on
     */
    var lastSeenOnWifi: String? = null
    /** Gets the last time (in milli seconds from 1970) that this ConnectableDevice was connected to.  */
    /**
     * Sets the last time (in milli seconds from 1970) that this ConnectableDevice was connected to.
     *
     * @param lastConnected Last connected time
     */
    var lastConnected: Long = 0
    /** Gets the last time (in milli seconds from 1970) that this ConnectableDevice was detected.  */
    /**
     * Sets the last time (in milli seconds from 1970) that this ConnectableDevice was detected.
     *
     * @param lastDetection Last detected time
     */
    var lastDetection: Long = 0

    /**
     * Sets the universally unique id of this particular ConnectableDevice object. This is used internally in the SDK and should not be used.
     * @param id New id for the ConnectableDevice
     */
    var id: String? = null
        /**
         * Universally unique id of this particular ConnectableDevice object, persists between sessions in ConnectableDeviceStore for connected devices
         */
        get() {
            if (field == null) field = uuid4().toString()

            return field
        }

    var serviceDescription: ServiceDescription? = null

    var listeners: ConcurrentMutableList<ConnectableDeviceListener> = ConcurrentMutableList()

    public var services: ConcurrentMutableMap<String, DeviceService> = ConcurrentMutableMap()

    // @endcond
    var serviceId: String? = null

    var isConnecting: Boolean = false

    var featuresReady: Boolean = false

    constructor(
        ipAddress: String?,
        friendlyName: String?,
        modelName: String?,
        modelNumber: String?,
    ) : this() {
        this.ipAddress = ipAddress
        this.friendlyName = friendlyName
        this.modelName = modelName
        this.modelNumber = modelNumber
    }

    constructor(description: ServiceDescription) : this() {
        update(description)
    }

    constructor(json: JsonObject) : this() {
        id = json.getValue(KEY_ID).jsonPrimitive.content
        lastKnownIPAddress = json.getValue(KEY_LAST_IP).jsonPrimitive.contentOrNull
        friendlyName = json.getValue(KEY_FRIENDLY).jsonPrimitive.contentOrNull
        modelName = json.getValue(KEY_MODEL_NAME).jsonPrimitive.contentOrNull
        modelNumber = json.getValue(KEY_MODEL_NUMBER).jsonPrimitive.contentOrNull
        lastSeenOnWifi = json.getValue(KEY_LAST_SEEN).jsonPrimitive.contentOrNull
        lastConnected = json.getValue(KEY_LAST_CONNECTED).jsonPrimitive.long
        lastDetection = json.getValue(KEY_LAST_DETECTED).jsonPrimitive.long
    }

    // @endcond
    /**
     * set desirable pairing type for all services
     * @param pairingType
     */
    fun setPairingType(pairingType: PairingType) {
        val services = getServices()
        for (service in services) {
            service.setPairingType(pairingType)
        }
    }

    /**
     * Adds a DeviceService to the ConnectableDevice instance. Only one instance of each DeviceService type (webOS, Netcast, etc) may be attached to a single ConnectableDevice instance. If a device contains your service type already, your service will not be added.
     *
     * @param service DeviceService to be added
     */
    suspend fun addService(service: DeviceService) {
        val added = getMismatchCapabilities(service.capabilities, capabilities)

        service.listener = this

        Util.runOnUI {
            for (listener in listeners) listener.onCapabilityUpdated(
                this@ConnectableDevice, added, ArrayList()
            )
        }

        services[service.serviceName!!] = service
    }

    /**
     * Removes a DeviceService from the ConnectableDevice instance.
     *
     * @param service DeviceService to be removed
     */
    suspend fun removeService(service: DeviceService) {
        removeServiceWithId(service.serviceName)
    }

    /**
     * Removes a DeviceService from the ConnectableDevice instance.
     *
     * @param serviceId ID of the DeviceService to be removed (DLNA, webOS TV, etc)
     */
    suspend fun removeServiceWithId(serviceId: String?) {
        val service = services[serviceId] ?: return

        service.disconnect()

        services.remove(serviceId)

        val removed = getMismatchCapabilities(service.capabilities, capabilities)

        Util.runOnUI {
            for (listener in listeners) listener.onCapabilityUpdated(
                this@ConnectableDevice, ArrayList(), removed
            )
        }
    }

    private fun getMismatchCapabilities(
        capabilities: List<String>,
        allCapabilities: List<String>,
    ): List<String> {
        val list: MutableList<String> = ArrayList()

        for (cap in capabilities) {
            if (!allCapabilities.contains(cap)) {
                list.add(cap)
            }
        }

        return list
    }

    /** Array of all currently discovered DeviceServices this ConnectableDevice has associated with it.  */
    fun getServices(): Collection<DeviceService> {
        return services.values
    }

    /**
     * Obtains a service from the ConnectableDevice with the provided serviceName
     *
     * @param serviceName Service ID of the targeted DeviceService (webOS, Netcast, DLNA, etc)
     * @return DeviceService with the specified serviceName or nil, if none exists
     */
    fun getServiceByName(serviceName: String): DeviceService? {
        for (service in getServices()) {
            if (service.serviceName == serviceName) {
                return service
            }
        }

        return null
    }

    /**
     * Removes a DeviceService form the ConnectableDevice instance.  serviceName is used as the identifier because only one instance of each DeviceService type may be attached to a single ConnectableDevice instance.
     *
     * @param serviceName Name of the DeviceService to be removed from the ConnectableDevice.
     */
    suspend fun removeServiceByName(serviceName: String) {
        val service = getServiceByName(serviceName)
        service?.let { removeService(it) }
    }

    /**
     * Returns a DeviceService from the ConnectableDevice instance. serviceUUID is used as the identifier because only one instance of each DeviceService type may be attached to a single ConnectableDevice instance.
     *
     * @param serviceUUID UUID of the DeviceService to be returned
     */
    fun getServiceWithUUID(serviceUUID: String): DeviceService? {
        for (service in getServices()) {
            if (service._serviceDescription?.uuid == serviceUUID) {
                return service
            }
        }

        return null
    }

    /**
     * Adds the ConnectableDeviceListener to the list of listeners for this ConnectableDevice to receive certain events.
     *
     * @param listener ConnectableDeviceListener to listen to device events (connect, disconnect, ready, etc)
     */
    fun addListener(listener: ConnectableDeviceListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    /**
     * Removes a previously added ConenctableDeviceListener from the list of listeners for this ConnectableDevice.
     *
     * @param listener ConnectableDeviceListener to be removed
     */
    fun removeListener(listener: ConnectableDeviceListener) {
        listeners.remove(listener)
    }

    fun getListeners(): List<ConnectableDeviceListener> {
        return listeners
    }

    /**
     * Enumerates through all DeviceServices and attempts to connect to each of them. When all of a ConnectableDevice's DeviceServices are ready to receive commands, the ConnectableDevice will send a onDeviceReady message to its listener.
     *
     * It is always necessary to call connect on a ConnectableDevice, even if it contains no connectable DeviceServices.
     */
    suspend fun connect() {
        isConnecting = true
        for (service in services.values) {
            if (!service.isConnected()) {
                service.connect()
            }
        }
        isConnecting = false
    }

    /**
     * Enumerates through all DeviceServices and attempts to disconnect from each of them.
     */
    suspend fun disconnect() {
        for (service in services.values) {
            service.disconnect()
        }

        Util.runOnUI {
            for (listener in listeners) listener.onDeviceDisconnected(
                this@ConnectableDevice
            )
        }
    }

    val isConnected: Boolean
        // @cond INTERNAL
        get() {
            var connectedCount = 0

            val iterator: Iterator<DeviceService> = services.values.iterator()

            while (iterator.hasNext()) {
                val service = iterator.next()

                if (!service.isConnectable()) {
                    connectedCount++
                } else {
                    if (service.isConnected()) connectedCount++
                }
            }

            // In case of Service Integration, a device is assumed as connected,
            // if a service in the device is connected.
            return connectedCount >= 1
            //return connectedCount >= services.size();
        }

    // @endcond
    fun isConnectable(): Boolean {
        for (service in services.values) {
            if (service.isConnectable()) return true
        }

        return false
    }

    /**
     * Sends a pairing key to all discovered device services.
     *
     * @param pairingKey Pairing key to send to services.
     */
    fun sendPairingKey(pairingKey: String?) {
        for (service in services.values) {
            service.sendPairingKey(pairingKey)
        }
    }

    /** Explicitly cancels pairing on all services that require pairing. In some services, this will hide a prompt that is displaying on the device.  */
    fun cancelPairing() {
        for (service in services.values) {
            service.cancelPairing()
        }
    }

    val capabilities: List<String>
        /** A combined list of all capabilities that are supported among the detected DeviceServices.  */
        get() {
            val caps: MutableList<String> = ArrayList()

            for (service in services.values) {
                for (capability in service.capabilities) {
                    if (!caps.contains(capability)) {
                        caps.add(capability)
                    }
                }
            }

            return caps
        }

    /**
     * Test to see if the capabilities array contains a given capability. See the individual Capability classes for acceptable capability values.
     *
     * It is possible to append a wildcard search term `.Any` to the end of the search term. This method will return true for capabilities that match the term up to the wildcard.
     *
     * Example: `Launcher.App.Any`
     *
     * @param capability Capability to test against
     */
    fun hasCapability(capability: String): Boolean {
        var hasCap = false

        for (service in services.values) {
            if (service.hasCapability(capability)) {
                hasCap = true
                break
            }
        }

        return hasCap
    }

    /**
     * Test to see if the capabilities array contains at least one capability in a given set of capabilities. See the individual Capability classes for acceptable capability values.
     *
     * See hasCapability: for a description of the wildcard feature provided by this method.
     *
     * @param capabilities Array of capabilities to test against
     */
    fun hasAnyCapability(vararg capabilities: String): Boolean {
        for (service in services.values) {
            if (service.hasAnyCapability(*capabilities)) return true
        }

        return false
    }

    /**
     * Test to see if the capabilities array contains a given set of capabilities. See the individual Capability classes for acceptable capability values.
     *
     * See hasCapability: for a description of the wildcard feature provided by this method.
     *
     * @param capabilities Array of capabilities to test against
     */

    fun hasCapabilities(capabilities: List<String>): Boolean {
        return hasCapabilities(*capabilities.toTypedArray())
    }

    /**
     * Test to see if the capabilities array contains a given set of capabilities. See the individual Capability classes for acceptable capability values.
     *
     * See hasCapability: for a description of the wildcard feature provided by this method.
     *
     * @param capabilites Array of capabilities to test against
     */

    fun hasCapabilities(vararg capabilites: String): Boolean {
        var hasCaps = true

        for (capability in capabilites) {
            if (!hasCapability(capability)) {
                hasCaps = false
                break
            }
        }

        return hasCaps
    }

    /**
     * Get a capability with the highest priority from a device. If device doesn't have such
     * capability then returns null.
     * @param controllerClass type of capability
     * @return capability implementation
     */
    fun <T : CapabilityMethods> getCapability(controllerClass: KClass<T>): T? {
        var foundController: T? = null
        var foundControllerPriority: CapabilityPriorityLevel? =
            CapabilityPriorityLevel.NOT_SUPPORTED
        for (service in services.values) {
            if (service.getAPI<CapabilityMethods?>(controllerClass) == null) continue

            val controller = service.getAPI<T>(controllerClass)
            val controllerPriority = service.getPriorityLevel(controllerClass)

            if (foundController == null) {
                foundController = controller

                if (controllerPriority == null || controllerPriority == CapabilityPriorityLevel.NOT_SUPPORTED) {
                    Logger.w(
                        Util.T,
                        message = { "We found a mathcing capability class, but no priority level for the class. Please check \"getPriorityLevel()\" in your class" }
                    )
                }
                foundControllerPriority = controllerPriority
            } else if (controllerPriority != null && foundControllerPriority != null) {
                if (controllerPriority.value > foundControllerPriority.value) {
                    foundController = controller
                    foundControllerPriority = controllerPriority
                }
            }
        }

        return foundController
    }

    val connectedServiceNames: String?
        // @cond INTERNAL
        get() {
            val serviceCount = getServices().size

            if (serviceCount <= 0) return null

            val serviceNames = arrayOfNulls<String>(serviceCount)
            var serviceIndex = 0

            for (service in getServices()) {
                serviceNames[serviceIndex] = service.serviceName

                serviceIndex++
            }

            // credit: http://stackoverflow.com/a/6623121/2715
            val sb = StringBuilder()

            for (serviceName in serviceNames) {
                if (sb.length > 0) sb.append(", ")

                sb.append(serviceName)
            }

            return sb.toString()
            ////
        }

    fun update(description: ServiceDescription) {
        ipAddress = description.ipAddress
        friendlyName = description.friendlyName
        modelName = description.modelName
        modelNumber = description.modelNumber
        lastConnected = description.lastDetection
    }

    fun toJsonObject(): JsonObject {
        val entries = mutableMapOf<String, JsonElement>(
            KEY_ID to JsonPrimitive(id),
            KEY_LAST_IP to JsonPrimitive(ipAddress),
            KEY_FRIENDLY to JsonPrimitive(friendlyName),
            KEY_MODEL_NAME to JsonPrimitive(modelName),
            KEY_MODEL_NUMBER to JsonPrimitive(modelNumber),
            KEY_LAST_SEEN to JsonPrimitive(lastSeenOnWifi),
            KEY_LAST_CONNECTED to JsonPrimitive(lastConnected),
            KEY_LAST_DETECTED to JsonPrimitive(lastDetection)
        )

        val servicesObjects = mutableMapOf<String, JsonObject>()
        for (service in services.values) {
            servicesObjects[service.serviceConfig._serviceUUID] = service.toJsonObject()
        }
        entries[KEY_SERVICES] = JsonObject(servicesObjects)

        return JsonObject(entries)
    }

    override fun toString(): String {
        return toJsonObject().toString()
    }

    override suspend fun onCapabilitiesUpdated(
        service: DeviceService,
        added: List<String>,
        removed: List<String>,
    ) {
        DiscoveryManager.getInstance()!!.onCapabilityUpdated(this, added, removed)
    }


    override fun onConnectionFailure(service: DeviceService, error: Error?) {
        // disconnect device if all services are not connected
        onDisconnect(service, error)
    }

    override fun onConnectionRequired(service: DeviceService) {
    }

    override suspend fun onConnectionSuccess(service: DeviceService) {
        //  TODO: iOS is passing to a function for when each service is ready on a device.  This is not implemented on Android.

        if (isConnected) {
            val deviceStore: ConnectableDeviceStore? =
                DiscoveryManager.getInstance()?.getConnectableDeviceStore()
            deviceStore?.addDevice(this)

            Util.runOnUI {
                for (listener in listeners) listener.onDeviceReady(
                    this@ConnectableDevice
                )
            }

            lastConnected = Util.getTime()
        }
    }

    override fun onDisconnect(service: DeviceService, error: Error?) {
        if (getConnectedServiceCount() == 0 || services.size == 0) {
            for (listener in listeners) {
                listener.onDeviceDisconnected(this)
            }
        }
    }

    override fun onPairingFailed(service: DeviceService, error: Error?) {
        for (listener in listeners) listener.onConnectionFailed(
            this, ServiceCommandError(0, "Failed to pair with service " + service.serviceName, null)
        )
    }

    override fun onPairingRequired(
        service: DeviceService,
        pairingType: PairingType,
        pairingData: Any?,
    ) {
        for (listener in listeners) listener.onPairingRequired(this, service, pairingType)
    }

    override fun onPairingSuccess(service: DeviceService) {
    }

    private fun getConnectedServiceCount(): Int {
        var count = 0

        for (service in services.values) {
            if (service.isConnectable()) {
                if (service.isConnected()) count++
            } else {
                count++
            }
        }

        return count
    }

    companion object {
        // @cond INTERNAL
        const val KEY_ID: String = "id"
        const val KEY_LAST_IP: String = "lastKnownIPAddress"
        const val KEY_FRIENDLY: String = "friendlyName"
        const val KEY_MODEL_NAME: String = "modelName"
        const val KEY_MODEL_NUMBER: String = "modelNumber"
        const val KEY_LAST_SEEN: String = "lastSeenOnWifi"
        const val KEY_LAST_CONNECTED: String = "lastConnected"
        const val KEY_LAST_DETECTED: String = "lastDetection"
        const val KEY_SERVICES: String = "services"

        fun createFromConfigString(
            ipAddress: String?,
            friendlyName: String?,
            modelName: String?,
            modelNumber: String?,
        ): ConnectableDevice {
            return ConnectableDevice(ipAddress, friendlyName, modelName, modelNumber)
        }

        fun createWithId(
            id: String?,
            ipAddress: String?,
            friendlyName: String?,
            modelName: String?,
            modelNumber: String?,
        ): ConnectableDevice {
            val mDevice = ConnectableDevice(ipAddress, friendlyName, modelName, modelNumber)
            mDevice.id = id

            return mDevice
        }
    }
}
