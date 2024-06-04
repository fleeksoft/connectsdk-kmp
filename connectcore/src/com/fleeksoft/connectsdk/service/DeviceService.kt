/*
 * DeviceService
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fleeksoft.connectsdk.service

import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.device.ConnectableDevice
import com.fleeksoft.connectsdk.discovery.DiscoveryFilter
import com.fleeksoft.connectsdk.etc.helper.DeviceServiceReachability
import com.fleeksoft.connectsdk.etc.helper.DeviceServiceReachability.DeviceServiceReachabilityListener
import com.fleeksoft.connectsdk.service.capability.*
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceCommand
import com.fleeksoft.connectsdk.service.command.ServiceCommand.ServiceCommandProcessor
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import com.fleeksoft.connectsdk.service.command.ServiceSubscription
import com.fleeksoft.connectsdk.service.command.URLServiceSubscription
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import com.fleeksoft.connectsdk.service.sessions.LaunchSession
import com.fleeksoft.connectsdk.service.sessions.LaunchSession.LaunchSessionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KClass

/**
 * ###Overview
 * From a high-level perspective, DeviceService completely abstracts the functionality of a particular service/protocol (webOS TV, Netcast TV, Chromecast, Roku, DIAL, etc).
 *
 * ###In Depth
 * DeviceService is an abstract class that is meant to be extended. You shouldn't ever use DeviceService directly, unless extending it to provide support for an additional service/protocol.
 *
 * Immediately after discovery of a DeviceService, DiscoveryManager will set the DeviceService's Listener to the ConnectableDevice that owns the DeviceService. You should not change the Listener unless you intend to manage the lifecycle of that service. The DeviceService will proxy all of its Listener method calls through the ConnectableDevice's ConnectableDeviceListener.
 *
 * ####Connection & Pairing
 * Your ConnectableDevice object will let you know if you need to connect or pair to any services.
 *
 * ####Capabilities
 * All DeviceService objects have a group of capabilities. These capabilities can be implemented by any object, and that object will be returned when you call the DeviceService's capability methods (launcher, mediaPlayer, volumeControl, etc).
 */
abstract class DeviceService : DeviceServiceReachabilityListener, ServiceCommandProcessor {
    /**
     * Enumerates available pairing types. It is used by a DeviceService for implementing pairing
     * strategy.
     */
    enum class PairingType {
        /**
         * DeviceService doesn't require pairing
         */
        NONE,

        /**
         * In this mode user must confirm pairing on the first screen device (e.g. an alert on a TV)
         */
        FIRST_SCREEN,

        /**
         * In this mode user must enter a pin code from a mobile device and send it to the first
         * screen device
         */
        PIN_CODE,

        /**
         * In this mode user can either enter a pin code from a mobile device or confirm
         * pairing on the TV
         */
        MIXED,
    }

    var _pairingType: PairingType = PairingType.NONE

    var _serviceDescription: ServiceDescription? = null

    // @cond INTERNAL
    // @endcond
    var serviceConfig: ServiceConfig

    protected var mServiceReachability: DeviceServiceReachability? = null
    protected var connected: Boolean = false

    var commandProcessor: ServiceCommandProcessor? = null
        get() {
            return if (field == null) this else field
        }

    // @endcond
    /**
     * An array of capabilities supported by the DeviceService. This array may change based off a number of factors.
     * - DiscoveryManager's pairingLevel value
     * - Connect SDK framework version
     * - First screen device OS version
     * - First screen device configuration (apps installed, settings, etc)
     * - Physical region
     */
    private var mCapabilities: MutableList<String> = ArrayList()

    // @cond INTERNAL
    var listener: DeviceServiceListener? = null


    constructor(serviceDescription: ServiceDescription?, serviceConfig: ServiceConfig) {
        this._serviceDescription = serviceDescription
        this.serviceConfig = serviceConfig
    }

    constructor(serviceConfig: ServiceConfig) {
        this.serviceConfig = serviceConfig
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch { updateCapabilities() }
    }

    fun setPairingType(pairingType: PairingType) {
    }

    fun getPairingType() = _pairingType

    // TODO: remove reflection
    fun <T : CapabilityMethods?> getAPI(clazz: KClass<*>): T? {
        return if (clazz.isInstance(this)) {
            this as? T
        } else null
    }

    open fun getPriorityLevel(clazz: KClass<out CapabilityMethods>): CapabilityPriorityLevel? {
        return CapabilityPriorityLevel.NOT_SUPPORTED
    }

    // @endcond
    /**
     * Will attempt to connect to the DeviceService. The failure/success will be reported back to the DeviceServiceListener. If the connection attempt reveals that pairing is required, the DeviceServiceListener will also be notified in that event.
     */
    open suspend fun connect() {
    }

    /**
     * Will attempt to disconnect from the DeviceService. The failure/success will be reported back to the DeviceServiceListener.
     */
    open suspend fun disconnect() {
    }

    /** Whether the DeviceService is currently connected  */
    open fun isConnected(): Boolean {
        return true
    }

    open fun isConnectable(): Boolean {
        return false
    }

    /** Explicitly cancels pairing in services that require pairing. In some services, this will hide a prompt that is displaying on the device.  */
    open fun cancelPairing() {
    }

    protected suspend fun reportConnected(ready: Boolean) {
        if (listener == null) return

        // only run callback on main thread if the callback is leaving the SDK
        if (listener is ConnectableDevice) (listener as ConnectableDevice).onConnectionSuccess(this)
        else {
            Util.runOnUI { if (listener != null) listener!!.onConnectionSuccess(this@DeviceService) }
        }
    }

    /**
     * Will attempt to pair with the DeviceService with the provided pairingData. The failure/success will be reported back to the DeviceServiceListener.
     *
     * @param pairingKey Data to be used for pairing. The type of this parameter will vary depending on what type of pairing is required, but is likely to be a string (pin code, pairing key, etc).
     */
    open fun sendPairingKey(pairingKey: String?) {
    }

    // @cond INTERNAL
    override suspend fun unsubscribe(subscription: URLServiceSubscription<*>?) {
    }

    override fun unsubscribe(subscription: ServiceSubscription<*>?) {
    }

    override suspend fun sendCommand(command: ServiceCommand<*>) {
    }

    val capabilities: List<String>
        get() {
            return mCapabilities
        }

    protected open suspend fun updateCapabilities() {}

    protected suspend fun setCapabilities(newCapabilities: MutableList<String>) {
        val oldCapabilities: List<String> = mCapabilities

        mCapabilities = newCapabilities

        val _lostCapabilities: MutableList<String> = ArrayList()

        for (capability: String in oldCapabilities) {
            if (!newCapabilities.contains(capability)) _lostCapabilities.add(capability)
        }

        val _addedCapabilities: MutableList<String> = ArrayList()

        for (capability: String in newCapabilities) {
            if (!oldCapabilities.contains(capability)) _addedCapabilities.add(capability)
        }

        val lostCapabilities: List<String> = _lostCapabilities
        val addedCapabilities: List<String> = _addedCapabilities

        if (this.listener != null) {
            Util.runOnUI {
                listener!!.onCapabilitiesUpdated(
                    this@DeviceService, addedCapabilities, lostCapabilities
                )
            }
        }
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
        val matchResult = CapabilityMethods.ANY_PATTERN.find(capability)
        if (matchResult != null) {
            val match = matchResult.value
            return mCapabilities.any { it.contains(match) }
        }

        return mCapabilities.contains(capability)
    }

    /**
     * Test to see if the capabilities array contains at least one capability in a given set of capabilities. See the individual Capability classes for acceptable capability values.
     *
     * See hasCapability: for a description of the wildcard feature provided by this method.
     *
     * @param capabilities Set of capabilities to test against
     */
    fun hasAnyCapability(vararg capabilities: String): Boolean {
        for (capability: String in capabilities) {
            if (hasCapability(capability)) return true
        }

        return false
    }

    /**
     * Test to see if the capabilities array contains a given set of capabilities. See the individual Capability classes for acceptable capability values.
     *
     * See hasCapability: for a description of the wildcard feature provided by this method.
     *
     * @param capabilities List of capabilities to test against
     */
    fun hasCapabilities(capabilities: List<String>): Boolean {
        return hasCapabilities(*capabilities.toTypedArray())
    }

    /**
     * Test to see if the capabilities array contains a given set of capabilities. See the individual Capability classes for acceptable capability values.
     *
     * See hasCapability: for a description of the wildcard feature provided by this method.
     *
     * @param capabilities Set of capabilities to test against
     */
    fun hasCapabilities(vararg capabilities: String): Boolean {
        var hasCaps: Boolean = true

        for (capability: String in capabilities) {
            if (!hasCapability(capability)) {
                hasCaps = false
                break
            }
        }

        return hasCaps
    }

    open suspend fun setServiceDescription(serviceDescription: ServiceDescription) {
        this._serviceDescription = serviceDescription
    }

    fun getServiceDescription(): ServiceDescription? {
        return _serviceDescription
    }

    fun toJsonObject(): JsonObject {
        val jsonObj = JsonObject(
            mapOf(
                KEY_CLASS to JsonPrimitive(this@DeviceService::class.simpleName),
                KEY_CONFIG to serviceConfig.toJsonObject(),
                KEY_DESC to _serviceDescription!!.toJsonObject()
            )
        )

        return jsonObj
    }

    val serviceName: String?
        /** Name of the DeviceService (webOS, Chromecast, etc)  */
        get() {
            return _serviceDescription?.serviceID
        }

    // @cond INTERNAL
    /**
     * Create a LaunchSession from a serialized JSON object.
     * May return null if the session was not the one that created the session.
     *
     * Intended for internal use.
     */
    open fun decodeLaunchSession(type: String, sessionObj: JsonObject): LaunchSession? {
        return null
    }

    // @endcond
    /**
     * Closes the session on the first screen device. Depending on the sessionType, the associated service will have different ways of handling the close functionality.
     *
     * @param launchSession LaunchSession to close
     * @param listener (optional) listener to be called on success/failure
     */
    open suspend fun closeLaunchSession(launchSession: LaunchSession?, listener: ResponseListener<Any?>) {
        if (launchSession == null) {
            Util.postError(
                listener, ServiceCommandError(0, "You must provide a valid LaunchSession", null)
            )
            return
        }

        val service: DeviceService? = launchSession.service
        if (service == null) {
            Util.postError(
                listener,
                ServiceCommandError(0, "There is no service attached to this launch session", null)
            )
            return
        }

        when (launchSession.sessionType) {
            LaunchSessionType.App -> if (service is Launcher) (service as Launcher).closeApp(
                launchSession, listener
            )

            LaunchSessionType.Media -> if (service is MediaPlayer) (service as MediaPlayer).closeMedia(
                launchSession, listener
            )

            LaunchSessionType.ExternalInputPicker -> if (service is ExternalInputControl) (service as ExternalInputControl).closeInputPicker(
                launchSession, listener
            )

            LaunchSessionType.WebApp -> if (service is WebAppLauncher) (service as WebAppLauncher).closeWebApp(
                launchSession, listener
            )

            LaunchSessionType.Unknown -> Util.postError(
                listener, ServiceCommandError(
                    0, "This DeviceService does not know ho to close this LaunchSession", null
                )
            )

            else -> Util.postError(
                listener, ServiceCommandError(
                    0, "This DeviceService does not know ho to close this LaunchSession", null
                )
            )
        }
    }

    // @cond INTERNAL
    suspend fun addCapability(capability: String) {
        if ((capability.isEmpty()) || mCapabilities.contains(capability)) return

        mCapabilities.add(capability)

        Util.runOnUI {
            val added: MutableList<String> = ArrayList()
            added.add(capability)

            if (listener != null) listener!!.onCapabilitiesUpdated(
                this@DeviceService, added, ArrayList()
            )
        }
    }

    suspend fun addCapabilities(capabilities: List<String>) {

        for (capability: String? in capabilities) {
            if ((capability == null) || (capability.length == 0) || mCapabilities.contains(
                    capability
                )
            ) continue

            mCapabilities.add(capability)
        }

        Util.runOnUI {
            if (listener != null) listener!!.onCapabilitiesUpdated(
                this@DeviceService, capabilities, ArrayList()
            )
        }
    }

    suspend fun addCapabilities(vararg capabilities: String) {
        addCapabilities(capabilities.toList())
    }

    suspend fun removeCapability(capability: String) {

        mCapabilities.remove(capability)

        Util.runOnUI {
            val removed: MutableList<String> = ArrayList()
            removed.add(capability)

            if (listener != null) listener!!.onCapabilitiesUpdated(
                this@DeviceService, ArrayList(), removed
            )
        }
    }

    suspend fun removeCapabilities(capabilities: List<String>) {

        for (capability: String? in capabilities) {
            mCapabilities.remove(capability)
        }

        Util.runOnUI {
            if (listener != null) listener!!.onCapabilitiesUpdated(
                this@DeviceService, ArrayList(), capabilities
            )
        }
    }

    suspend fun removeCapabilities(vararg capabilities: String) {
        removeCapabilities(capabilities.toList())
    }

    //  Unused by default.
    override suspend fun onLoseReachability(reachability: DeviceServiceReachability?) {}

    interface DeviceServiceListener {
        /**!
         * If the DeviceService requires an active connection (websocket, pairing, etc) this method will be called.
         *
         * @param service DeviceService that requires connection
         */
        fun onConnectionRequired(service: DeviceService)

        /**!
         * After the connection has been successfully established, and after pairing (if applicable), this method will be called.
         *
         * @param service DeviceService that was successfully connected
         */
        suspend fun onConnectionSuccess(service: DeviceService)

        /**
         * There are situations in which a DeviceService will update the capabilities it supports and propagate these changes to the DeviceService. Such situations include:
         * - on discovery, DIALService will reach out to detect if certain apps are installed
         * - on discovery, certain DeviceServices need to reach out for version & region information
         *
         * For more information on this particular method, see ConnectableDeviceDelegate's connectableDevice:capabilitiesAdded:removed: method.
         *
         * @param service DeviceService that has experienced a change in capabilities
         * @param added List<String> of capabilities that are new to the DeviceService
         * @param removed List<String> of capabilities that the DeviceService has lost
         */
        suspend fun onCapabilitiesUpdated(
            service: DeviceService,
            added: List<String>,
            removed: List<String>,
        )

        /**
         * This method will be called on any disconnection. If error is nil, then the connection was clean and likely triggered by the responsible DiscoveryProvider or by the user.
         *
         * @param service DeviceService that disconnected
         * @param error Error with a description of any errors causing the disconnect. If this value is nil, then the disconnect was clean/expected.
         */
        fun onDisconnect(service: DeviceService, error: Error?)

        /**
         * Will be called if the DeviceService fails to establish a connection.
         *
         * @param service DeviceService which has failed to connect
         * @param error Error with a description of the failure
         */
        fun onConnectionFailure(service: DeviceService, error: Error?)

        /**
         * If the DeviceService requires pairing, valuable data will be passed to the delegate via this method.
         *
         * @param service DeviceService that requires pairing
         * @param pairingType PairingType that the DeviceService requires
         * @param pairingData Any data that might be required for the pairing process, will usually be nil
         */
        fun onPairingRequired(service: DeviceService, pairingType: PairingType, pairingData: Any?)

        /**
         * This method will be called upon pairing success. On pairing success, a connection to the DeviceService will be attempted.
         *
         * @property service DeviceService that has successfully completed pairing
         */
        fun onPairingSuccess(service: DeviceService)

        /**
         * If there is any error in pairing, this method will be called.
         *
         * @param service DeviceService that has failed to complete pairing
         * @param error Error with a description of the failure
         */
        fun onPairingFailed(service: DeviceService, error: Error?)
    }

    companion object {
        // @cond INTERNAL
        val KEY_CLASS: String = "class"
        val KEY_CONFIG: String = "config"
        val KEY_DESC: String = "description"


        fun discoveryFilter(): DiscoveryFilter? {
            return null
        }
    }
}
