/*
 * DiscoveryManager
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
package com.fleeksoft.connectsdk.discovery

import co.touchlab.kermit.Logger
import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import com.fleeksoft.connectsdk.DefaultPlatform
import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.device.ConnectableDevice
import com.fleeksoft.connectsdk.device.ConnectableDeviceListener
import com.fleeksoft.connectsdk.device.ConnectableDeviceStore
import com.fleeksoft.connectsdk.device.DefaultConnectableDeviceStore
import com.fleeksoft.connectsdk.ported.DeviceServiceProvider
import com.fleeksoft.connectsdk.ported.PortedUtil
import com.fleeksoft.connectsdk.service.DLNAService
import com.fleeksoft.connectsdk.service.DeviceService
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import kotlinx.datetime.DateTimePeriod

/**
 * ###Overview
 *
 * At the heart of Connect SDK is DiscoveryManager, a multi-protocol service discovery engine with a pluggable architecture. Much of your initial experience with Connect SDK will be with the DiscoveryManager class, as it consolidates discovered service information into ConnectableDevice objects.
 *
 * ###In depth
 * DiscoveryManager supports discovering services of differing protocols by using DiscoveryProviders. Many services are discoverable over [SSDP][0] and are registered to be discovered with the SSDPDiscoveryProvider class.
 *
 * As services are discovered on the network, the DiscoveryProviders will notify DiscoveryManager. DiscoveryManager is capable of attributing multiple services, if applicable, to a single ConnectableDevice instance. Thus, it is possible to have a mixed-mode ConnectableDevice object that is theoretically capable of more functionality than a single service can provide.
 *
 * DiscoveryManager keeps a running list of all discovered devices and maintains a filtered list of devices that have satisfied any of your CapabilityFilters. This filtered list is used by the DevicePicker when presenting the user with a list of devices.
 *
 * Only one instance of the DiscoveryManager should be in memory at a time. To assist with this, DiscoveryManager has static method at sharedManager.
 *
 * Example:
 *
 * @capability kMediaControlPlay
 * @code
 * DiscoveryManager.init(getApplicationContext());
 * DiscoveryManager discoveryManager = DiscoveryManager.getInstance();
 * discoveryManager.addListener(this);
 * discoveryManager.start();
 * @endcode
 *
 * [0]: http://tools.ietf.org/html/draft-cai-ssdp-v1-03
 */
class DiscoveryManager(
    private var connectableDeviceStore: ConnectableDeviceStore = DefaultConnectableDeviceStore(),
) : ConnectableDeviceListener, DiscoveryProviderListener, ServiceConfig.ServiceConfigListener {
    /**
     * Describes a pairing level for a DeviceService. It's used by a DiscoveryManager and all
     * services.
     */
    enum class PairingLevel {
        /**
         * Specifies that pairing is off. DeviceService will never try to pair with a first
         * screen device.
         */
        OFF,

        /**
         * Specifies that pairing is protected. DeviceService will try to pair in protected mode
         * if it is required by a first screen device (webOS - Protected Permission).
         */
        PROTECTED,

        /**
         * Specifies that pairing is on. DeviceService will try to pair if it is required by a first
         * screen device.
         */
        ON
    }

    var rescanInterval: Int = 10

    private val allDevices: ConcurrentMutableMap<String, ConnectableDevice> = ConcurrentMutableMap()
    private val compatibleDevices: ConcurrentMutableMap<String, ConnectableDevice> =
        ConcurrentMutableMap()

    internal var deviceProvideClasses: ConcurrentMutableMap<String, DeviceServiceProvider<DeviceService>> =
        ConcurrentMutableMap()
    internal var discoveryProviders: ConcurrentMutableList<DiscoveryProvider> =
        ConcurrentMutableList()

    private val discoveryListeners: ConcurrentMutableList<DiscoveryManagerListener> =
        ConcurrentMutableList()
    private var capabilityFilters: List<CapabilityFilter> = ArrayList()

    private var isBroadcastReceiverRegistered: Boolean = false

    var rescanTimer: DateTimePeriod? = null

    private var pairingLevel: PairingLevel = PairingLevel.OFF

    private var mSearching: Boolean = false

    /**
     * If serviceIntegrationEnabled is false (default), all services look like in different devices.
     * If serviceIntegrationEnabled is true, services in a device are managed by one device instance.
     */
    private var serviceIntegrationEnabled: Boolean = false

    /**
     * Create a new instance of DiscoveryManager.
     * Direct use of this constructor is not recommended. In most cases,
     * you should use DiscoveryManager.getInstance() instead.
     */

    /**
     * Create a new instance of DiscoveryManager.
     * Direct use of this constructor is not recommended. In most cases,
     * you should use DiscoveryManager.getInstance() instead.
     */
    init {

//        In Android, a MulticastLock is a mechanism that prevents the Wi-Fi interface from entering power-saving modes (like sleep) while your app needs to receive multicast traffic. Multicast traffic is a type of network communication where data is sent to multiple recipients simultaneously. Examples include streaming media, group chat, and some discovery protocols.
        // TODO: solution for wifi multicastlock
        /*val wifiMgr: WifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifiMgr.createMulticastLock(Util.T)
        multicastLock.setReferenceCounted(true)*/

        // TODO: solution for BroadcastReceiver to capture whifi packets
        /*receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action: String? = intent.getAction()

                if ((action == WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    val networkInfo: NetworkInfo =
                        intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)

                    when (networkInfo.getState()) {
                        NetworkInfo.State.CONNECTED -> if (mSearching) {
                            for (provider: DiscoveryProvider? in discoveryProviders!!) {
                                provider!!.restart()
                            }
                        }

                        NetworkInfo.State.DISCONNECTED -> {
                            Log.w(Util.T, "Network connection is disconnected")

                            for (provider: DiscoveryProvider? in discoveryProviders!!) {
                                provider!!.reset()
                            }

                            allDevices.clear()

                            for (device: ConnectableDevice? in compatibleDevices.values) {
                                handleDeviceLoss(device)
                            }
                            compatibleDevices.clear()
                        }

                        NetworkInfo.State.CONNECTING -> {}
                        NetworkInfo.State.DISCONNECTING -> {}
                        NetworkInfo.State.SUSPENDED -> {}
                        NetworkInfo.State.UNKNOWN -> {}
                    }
                }
            }
        }*/

        registerBroadcastReceiver()
    }


    fun setServiceIntegration(value: Boolean) {
        serviceIntegrationEnabled = value
    }

    private fun isServiceIntegrationEnabled(): Boolean {
        return serviceIntegrationEnabled
    }

    /**
     * Use device name and IP for identification of device,
     * because some devices have multiple device instances with same IP.
     * (i.e., a device including docker containers with host network setting.)
     * And if service integration is false (default), all services look like different devices.
     */
    private fun getDeviceKey(device: ConnectableDevice): String {
        if (isServiceIntegrationEnabled()) return device.friendlyName + device.ipAddress
        return device.friendlyName + device.ipAddress + device.serviceId
    }

    private fun getDeviceKey(srvDesc: ServiceDescription): String {
        if (isServiceIntegrationEnabled()) return srvDesc.friendlyName + srvDesc.ipAddress
        return srvDesc.friendlyName + srvDesc.ipAddress + srvDesc.serviceID
    }

    private fun registerBroadcastReceiver() {
        if (!isBroadcastReceiverRegistered) {
            isBroadcastReceiverRegistered = true

            // TODO: wifi state change listener
            /*val intentFilter: IntentFilter = IntentFilter()
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            context.registerReceiver(receiver, intentFilter)*/
        }
    }

    private fun unregisterBroadcastReceiver() {
        if (isBroadcastReceiverRegistered) {
            isBroadcastReceiverRegistered = false

            // TODO: wifi state change listener unregister
//            context.unregisterReceiver(receiver)
        }
    }

    /**
     * Listener which should receive discovery updates. It is not necessary to set this listener property unless you are implementing your own device picker. Connect SDK provides a default DevicePicker which acts as a DiscoveryManagerListener, and should work for most cases.
     *
     * If you have provided a capabilityFilters array, the listener will only receive update messages for ConnectableDevices which satisfy at least one of the CapabilityFilters. If no capabilityFilters array is provided, the listener will receive update messages for all ConnectableDevice objects that are discovered.
     */
    fun addListener(listener: DiscoveryManagerListener) {
        // notify listener of all devices so far
        for (device: ConnectableDevice in compatibleDevices.values) {
            listener.onDeviceAdded(this, device)
        }
        discoveryListeners.add(listener)
    }

    /**
     * Removes a previously added listener
     */
    fun removeListener(listener: DiscoveryManagerListener) {
        discoveryListeners.remove(listener)
    }

    suspend fun setCapabilityFilters(vararg capabilityFilters: CapabilityFilter) {
        setCapabilityFilters(capabilityFilters.toList())
    }

    private suspend fun setCapabilityFilters(capabilityFilters: List<CapabilityFilter>) {
        this.capabilityFilters = capabilityFilters

        for (device: ConnectableDevice in compatibleDevices.values) {
            handleDeviceLoss(device)
        }

        compatibleDevices.clear()

        for (device: ConnectableDevice in allDevices.values) {
            if (deviceIsCompatible(device)) {
                compatibleDevices[getDeviceKey(device)] = device

                handleDeviceAdd(device)
            }
        }
    }

    /**
     * Returns the list of capability filters.
     */
    fun getCapabilityFilters(): List<CapabilityFilter>? {
        return capabilityFilters
    }

    private fun deviceIsCompatible(device: ConnectableDevice): Boolean {
        if (capabilityFilters.isEmpty()) {
            return true
        }

        var isCompatible: Boolean = false

        for (filter: CapabilityFilter in capabilityFilters) {
            if (device.hasCapabilities(filter.capabilities)) {
                isCompatible = true
                break
            }
        }

        return isCompatible
    }

    // @cond INTERNAL
    /**
     * Registers a commonly-used set of DeviceServices with DiscoveryManager. This method will be called on first call of startDiscovery if no DeviceServices have been registered.
     *
     * - CastDiscoveryProvider
     * + CastService
     * - SSDPDiscoveryProvider
     * + DIALService
     * + DLNAService (limited to LG TVs, currently)
     * + NetcastTVService
     * + RokuService
     * + WebOSTVService
     * + MultiScreenService
     * - ZeroconfDiscoveryProvider
     * + AirPlayService
     */
    suspend fun registerDefaultDeviceTypes() {
        val devicesList: HashMap<DeviceServiceProvider<DeviceService>, Lazy<DiscoveryProvider>> =
            DefaultPlatform.getDeviceServiceMap()

        for (entry: Map.Entry<DeviceServiceProvider<DeviceService>, Lazy<DiscoveryProvider>> in devicesList.entries) {
            try {
                registerDeviceService(entry.key, entry.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Registers a DeviceService with DiscoveryManager and tells it which DiscoveryProvider to use to find it. Each DeviceService has a JsonObject of discovery parameters that its DiscoveryProvider will use to find it.
     *
     * @param deviceServiceProvider Class for object that should be instantiated when DeviceService is found
     * @param discoveryProviderLazy Class for object that should discover this DeviceService. If a DiscoveryProvider of this class already exists, then the existing DiscoveryProvider will be used.
     */
    suspend fun registerDeviceService(
        deviceServiceProvider: DeviceServiceProvider<DeviceService>,
        discoveryProviderLazy: Lazy<DiscoveryProvider>,
    ) {
        try {
//            todo:// not sure if it work with child class
            var discoveryProvider: DiscoveryProvider? =
                discoveryProviders.find { it::class == DiscoveryProvider::class }

            if (discoveryProvider == null) {
                discoveryProvider = discoveryProviderLazy.value
                discoveryProvider.addListener(this)
                discoveryProviders.add(discoveryProvider)
            }
            val discoveryFilter: DiscoveryFilter = deviceServiceProvider.getDiscoveryFilter()
            val serviceId: String = discoveryFilter.serviceId

            deviceProvideClasses[serviceId] = deviceServiceProvider

            discoveryProvider.addDeviceFilter(discoveryFilter)
            if (mSearching) {
                discoveryProvider.restart()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Unregisters a DeviceService with DiscoveryManager. If no other DeviceServices are set to being discovered with the associated DiscoveryProvider, then that DiscoveryProvider instance will be stopped and shut down.
     *
     * @param deviceClass Class for DeviceService that should no longer be discovered
     * @param discoveryClass Class for DiscoveryProvider that is discovering DeviceServices of deviceClass type
     */
    suspend fun unregisterDeviceService(
        discoveryFilter: DiscoveryFilter,
        discoveryProvider: DiscoveryProvider,
    ) {

        try {
            val serviceId: String = discoveryFilter.serviceId

            // do not remove provider if there is no such service
            if (null == deviceProvideClasses.remove(serviceId)) {
                return
            }

            discoveryProvider.removeDeviceFilter(discoveryFilter)

            if (discoveryProvider.isEmpty()) {
                discoveryProvider.stop()
                discoveryProviders.remove(discoveryProvider)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // @endcond
    /**
     * Start scanning for devices on the local network.
     */
    suspend fun start() {
        if (mSearching) return

        mSearching = true
        // TODO: handle multicastlock
//        multicastLock.acquire()

        Util.runOnUI {

            if (discoveryProviders.size == 0) {
                registerDefaultDeviceTypes()
            }

            if (PortedUtil.isWifiConnected()) {
                for (provider: DiscoveryProvider? in discoveryProviders) {
                    provider!!.start()
                }
            } else {
                Logger.w(Util.T, message = { "Wifi is not connected yet" })

                Util.runOnUI {
                    for (listener: DiscoveryManagerListener in discoveryListeners) listener.onDiscoveryFailed(
                        this@DiscoveryManager, ServiceCommandError(0, "No wifi connection", null)
                    )
                }
            }
        }
    }

    /**
     * Stop scanning for devices.
     */
    suspend fun stop() {
        if (!mSearching) return

        mSearching = false

        for (provider: DiscoveryProvider? in discoveryProviders) {
            provider!!.stop()
        }

        // TODO: handle multicastLock
        /*if (multicastLock.isHeld()) {
            multicastLock.release()
        }*/
    }

    /**
     * ConnectableDeviceStore object which loads & stores references to all discovered devices. Pairing codes/keys, SSL certificates, recent access times, etc are kept in the device store.
     *
     * ConnectableDeviceStore is a protocol which may be implemented as needed. A default implementation, DefaultConnectableDeviceStore, exists for convenience and will be used if no other device store is provided.
     *
     * In order to satisfy user privacy concerns, you should provide a UI element in your app which exposes the ConnectableDeviceStore removeAll method.
     *
     * To disable the ConnectableDeviceStore capabilities of Connect SDK, set this value to nil. This may be done at the time of instantiation with `DiscoveryManager.init(context, null);`.
     */
    fun setConnectableDeviceStore(connectableDeviceStore: ConnectableDeviceStore) {
        this.connectableDeviceStore = connectableDeviceStore
    }

    /**
     * ConnectableDeviceStore object which loads & stores references to all discovered devices. Pairing codes/keys, SSL certificates, recent access times, etc are kept in the device store.
     *
     * ConnectableDeviceStore is a protocol which may be implemented as needed. A default implementation, DefaultConnectableDeviceStore, exists for convenience and will be used if no other device store is provided.
     *
     * In order to satisfy user privacy concerns, you should provide a UI element in your app which exposes the ConnectableDeviceStore removeAll method.
     *
     * To disable the ConnectableDeviceStore capabilities of Connect SDK, set this value to nil. This may be done at the time of instantiation with `DiscoveryManager.init(context, null);`.
     */
    fun getConnectableDeviceStore(): ConnectableDeviceStore? {
        return connectableDeviceStore
    }

    // @cond INTERNAL
    private fun handleDeviceAdd(device: ConnectableDevice) {
        if (!deviceIsCompatible(device)) return

        compatibleDevices[getDeviceKey(device)] = device

        for (listenter: DiscoveryManagerListener in discoveryListeners) {
            listenter.onDeviceAdded(this, device)
        }
    }

    private suspend fun handleDeviceUpdate(device: ConnectableDevice) {
        val devKey: String = getDeviceKey(device)

        if (deviceIsCompatible(device)) {
            if (device.ipAddress != null && compatibleDevices.containsKey(devKey)) {
                for (listenter: DiscoveryManagerListener in discoveryListeners) {
                    listenter.onDeviceUpdated(this, device)
                }
            } else {
                handleDeviceAdd(device)
            }
        } else {
            compatibleDevices.remove(devKey)
            handleDeviceLoss(device)
        }
    }

    private suspend fun handleDeviceLoss(device: ConnectableDevice) {
        for (listenter: DiscoveryManagerListener in discoveryListeners) {
            listenter.onDeviceRemoved(this, device)
        }

        device.disconnect()
    }

    private fun isNetcast(description: ServiceDescription): Boolean {
        TODO("NOT IMPLEMENTED YET")
        /*var isNetcastTV: Boolean = false

        val modelName: String? = description.modelName
        val modelDescription: String? = description.modelDescription

        if (modelName != null && (modelName.uppercase() == "LG TV")) {
            if (modelDescription != null && !(modelDescription.uppercase().contains("WEBOS"))) {
                if ((description.serviceID == NetcastTVService.Companion.ID)) {
                    isNetcastTV = true
                }
            }
        }

        return isNetcastTV*/
    }

    // @endcond
    /**
     * List of all devices discovered by DiscoveryManager. Each ConnectableDevice object is keyed against its current IP address.
     */
    private fun getAllDevices(): Map<String, ConnectableDevice> {
        return allDevices
    }

    /**
     * Returns the device which is matched with deviceId.
     * Returns null if deviceId is null.
     */
    fun getDeviceById(deviceId: String?): ConnectableDevice? {
        if (deviceId != null) {
            for (dvc: ConnectableDevice in allDevices.values) {
                if (deviceId == dvc.id) return dvc
            }
        }

        return null
    }

    /**
     * Returns the device which is matched with deviceId.
     * Returns null if deviceId is null.
     */
    fun getDeviceByIpAddress(ipAddress: String?): ConnectableDevice? {
        if (ipAddress != null) {
            for (dvc: ConnectableDevice in allDevices.values) {
                if ((ipAddress == dvc.ipAddress) == true) return dvc
            }
        }

        return null
    }

    /**
     * Filtered list of discovered ConnectableDevices, limited to devices that match at least one of the CapabilityFilters in the capabilityFilters array. Each ConnectableDevice object is keyed against its current IP address.
     */
    fun getCompatibleDevices(): Map<String, ConnectableDevice?> {
        return compatibleDevices
    }

    /**
     * The pairingLevel property determines whether capabilities that require pairing (such as entering a PIN) will be available.
     *
     * If pairingLevel is set to ConnectableDevicePairingLevelOn, ConnectableDevices that require pairing will prompt the user to pair when connecting to the ConnectableDevice.
     *
     * If pairingLevel is set to ConnectableDevicePairingLevelOff (the default), connecting to the device will avoid requiring pairing if possible but some capabilities may not be available.
     */
    fun getPairingLevel(): PairingLevel {
        return pairingLevel
    }

    /**
     * The pairingLevel property determines whether capabilities that require pairing (such as entering a PIN) will be available.
     *
     * If pairingLevel is set to ConnectableDevicePairingLevelOn, ConnectableDevices that require pairing will prompt the user to pair when connecting to the ConnectableDevice.
     *
     * If pairingLevel is set to ConnectableDevicePairingLevelOff (the default), connecting to the device will avoid requiring pairing if possible but some capabilities may not be available.
     */
    fun setPairingLevel(pairingLevel: PairingLevel) {
        this.pairingLevel = pairingLevel
    }


    fun onDestroy() {
        unregisterBroadcastReceiver()
    }

    fun getDiscoveryProviders(): List<DiscoveryProvider?> {
        return ArrayList(discoveryProviders)
    }

    override suspend fun onServiceConfigUpdate(serviceConfig: ServiceConfig) {
        for (device: ConnectableDevice in getAllDevices().values) {
            if (null != device.getServiceWithUUID(serviceConfig._serviceUUID)) {
                connectableDeviceStore.updateDevice(device)
            }
        }
    }

    override suspend fun onCapabilityUpdated(
        device: ConnectableDevice,
        added: List<String>,
        removed: List<String>,
    ) {
        handleDeviceUpdate(device)
    }

    override fun onConnectionFailed(device: ConnectableDevice, error: ServiceCommandError) {}
    override fun onDeviceDisconnected(device: ConnectableDevice) {}
    override fun onDeviceReady(device: ConnectableDevice) {}
    override fun onPairingRequired(
        device: ConnectableDevice,
        service: DeviceService,
        pairingType: DeviceService.PairingType,
    ) {
    }

    override suspend fun onServiceAdded(
        provider: DiscoveryProvider,
        serviceDescription: ServiceDescription,
    ) {
        Logger.d(
            Util.T,
            message = { "Service added: " + serviceDescription.friendlyName + " (" + serviceDescription.serviceID + ")" }
        )

        val devKey: String = getDeviceKey(serviceDescription)
        var deviceIsNew: Boolean = !allDevices.containsKey(devKey)
        var device: ConnectableDevice? = null

        if (deviceIsNew) {
            device = connectableDeviceStore.getDevice(serviceDescription.uuid)

            if (device != null) {
                allDevices[devKey] = device
                device.ipAddress = serviceDescription.ipAddress
            }
        } else {
            device = allDevices[devKey]
        }

        if (device == null) {
            device = ConnectableDevice(serviceDescription)
            device.ipAddress = serviceDescription.ipAddress
            allDevices[devKey] = device
            deviceIsNew = true
        }

        device.friendlyName = serviceDescription.friendlyName
        device.lastDetection = Util.getTime()
        device.lastKnownIPAddress = serviceDescription.ipAddress
        device.serviceId = serviceDescription.serviceID

        //  TODO: Implement the currentSSID Property in DiscoveryManager
//        device.setLastSeenOnWifi(currentSSID);
        addServiceDescriptionToDevice(serviceDescription, device)

        if (device.getServices().isEmpty()) {
            // we get here when a non-LG DLNA TV is found

            allDevices.remove(devKey)

            return
        }

        if (deviceIsNew) handleDeviceAdd(device)
        else handleDeviceUpdate(device)
    }

    override suspend fun onServiceRemoved(
        provider: DiscoveryProvider,
        serviceDescription: ServiceDescription?,
    ) {
        if (serviceDescription == null) {
            Logger.w(Util.T, message = { "onServiceRemoved: unknown service description" })

            return
        }

        Logger.d(
            Util.T,
            message = { "onServiceRemoved: friendlyName: ${serviceDescription.friendlyName}" })

        val devKey: String = getDeviceKey(serviceDescription)
        val device: ConnectableDevice? = allDevices[devKey]

        if (device != null) {
            device.removeServiceWithId(serviceDescription.serviceID)

            if (device.getServices().isEmpty()) {
                allDevices.remove(devKey)

                handleDeviceLoss(device)
            } else {
                handleDeviceUpdate(device)
            }
        }
    }

    override fun onServiceDiscoveryFailed(
        provider: DiscoveryProvider,
        error: ServiceCommandError,
    ) {
        Logger.w(
            Util.T,
            message = { "DiscoveryProviderListener, Service Discovery Failed: ${error.message}" })
    }

    private suspend fun addServiceDescriptionToDevice(
        desc: ServiceDescription,
        device: ConnectableDevice,
    ) {
        Logger.d(
            Util.T,
            message = { "Adding service ${desc.serviceID} to device with address ${device.ipAddress} and id ${device.id}" }
        )

        val deviceServiceProvider: DeviceServiceProvider<DeviceService> = deviceProvideClasses[desc.serviceID] ?: return

        if (deviceServiceProvider.isSameClass(DLNAService::class)) {
            if (desc.locationXML == null) return
        }
        // TODO: handle for NetcastTVService
        /* else if (deviceServiceClass is NetcastTVService::class) {
            if (!isNetcast(desc)) return
        }*/

        var serviceConfig: ServiceConfig? = connectableDeviceStore.getServiceConfig(desc)

        if (serviceConfig == null) serviceConfig = ServiceConfig(desc)
        serviceConfig.listener = this@DiscoveryManager

        var hasType: Boolean = false
        var hasService: Boolean = false

        for (service: DeviceService in device.getServices()) {
            val serviceDescription = service.getServiceDescription()
            if ((serviceDescription?.serviceID == desc.serviceID)) {
                hasType = true
                if ((serviceDescription?.uuid == desc.uuid)) {
                    hasService = true
                }
                break
            }
        }

        if (hasType) {
            if (hasService) {
                device.serviceDescription = desc

                val alreadyAddedService: DeviceService? = desc.serviceID?.let {
                    device.getServiceByName(it)
                }
                alreadyAddedService?.setServiceDescription(desc)

                return
            }

            desc.serviceID?.let { device.removeServiceByName(it) }
        }


        val deviceService: DeviceService = deviceServiceProvider.create(desc, serviceConfig)

        deviceService.setServiceDescription(desc)
        device.addService(deviceService)
    }

    companion object {

        private var instance: DiscoveryManager? = null

        /**
         * Initilizes the Discovery manager with a valid context.  This should be done as soon as possible and it should use getApplicationContext() as the Discovery manager could persist longer than the current Activity.
         *
         * @code
         * DiscoveryManager.init(getApplicationContext());
         * @endcode
         */

        fun init() {
            instance = DiscoveryManager()
        }

        fun destroy() {
            instance?.onDestroy()
        }

        /**
         * Initilizes the Discovery manager with a valid context.  This should be done as soon as possible and it should use getApplicationContext() as the Discovery manager could persist longer than the current Activity.
         *
         * This accepts a ConnectableDeviceStore to use instead of the default device store.
         *
         * @code
         * MyConnectableDeviceStore myDeviceStore = new MyConnectableDeviceStore();
         * DiscoveryManager.init(getApplicationContext(), myDeviceStore);
         * @endcode
         */
        fun init(connectableDeviceStore: ConnectableDeviceStore) {
            instance = DiscoveryManager(connectableDeviceStore)
        }

        /**
         * Get a shared instance of DiscoveryManager.
         */
        fun getInstance(): DiscoveryManager? {
            if (instance == null) throw Error("Call DiscoveryManager.init(Context) first")

            return instance
        }
    }
}
