/*
 * DIALService
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 24 Jan 2014
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

import co.touchlab.kermit.Logger
import com.benasher44.uuid.uuid4
import com.fleeksoft.connectsdk.core.AppInfo
import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.discovery.DiscoveryFilter
import com.fleeksoft.connectsdk.etc.helper.DeviceServiceReachability
import com.fleeksoft.connectsdk.etc.helper.HttpConnection
import com.fleeksoft.connectsdk.etc.helper.HttpMessage
import com.fleeksoft.connectsdk.ported.DeviceServiceProvider
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.Launcher
import com.fleeksoft.connectsdk.service.capability.Launcher.*
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.NotSupportedServiceSubscription
import com.fleeksoft.connectsdk.service.command.ServiceCommand
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import com.fleeksoft.connectsdk.service.command.ServiceSubscription
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import com.fleeksoft.connectsdk.service.sessions.LaunchSession
import com.fleeksoft.connectsdk.service.sessions.LaunchSession.LaunchSessionType
import io.ktor.http.*
import korlibs.util.format
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KClass

class DIALService(serviceDescription: ServiceDescription?, serviceConfig: ServiceConfig) :
    DeviceService(serviceDescription, serviceConfig), Launcher {
    override fun getPriorityLevel(clazz: KClass<out CapabilityMethods>): CapabilityPriorityLevel? {
        if ((clazz == Launcher::class)) {
            return getLauncherCapabilityLevel()
        }
        return CapabilityPriorityLevel.NOT_SUPPORTED
    }


    override suspend fun setServiceDescription(serviceDescription: ServiceDescription) {
        super.setServiceDescription(serviceDescription)

        val responseHeaders: Map<String, List<String>>? =
            getServiceDescription()?.responseHeaders

        if (responseHeaders != null) {
            val commandPath: String?
            val commandPaths: List<String>? = responseHeaders["Application-URL"]

            if (!commandPaths.isNullOrEmpty()) {
                commandPath = commandPaths[0]
                getServiceDescription()?.applicationURL = commandPath
            }
        }

        probeForAppSupport()
    }

    override fun getLauncher(): Launcher {
        return this
    }

    override fun getLauncherCapabilityLevel(): CapabilityPriorityLevel {
        return CapabilityPriorityLevel.NORMAL
    }

    override suspend fun launchApp(appId: String, listener: AppLaunchListener) {
        launchApp(appId, null, listener)
    }

    private suspend fun launchApp(appId: String, params: JsonObject?, listener: AppLaunchListener) {
        if (appId.isEmpty()) {
            Util.postError(listener, ServiceCommandError(0, "Must pass a valid appId", null))
            return
        }

        val appInfo: AppInfo = AppInfo(id = appId, name = appId)

        launchAppWithInfo(appInfo, listener)
    }

    override suspend fun launchAppWithInfo(appInfo: AppInfo, listener: AppLaunchListener?) {
        launchAppWithInfo(appInfo, null, listener)
    }

    override suspend fun launchAppWithInfo(appInfo: AppInfo, params: Any?, listener: AppLaunchListener?) {
        val command: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand<ResponseListener<Any>>(commandProcessor,
                requestURL(appInfo.name), params, object : ResponseListener<Any?> {
                    override fun onError(error: ServiceCommandError) {
                        Util.postError(
                            listener,
                            ServiceCommandError(0, "Problem Launching app", null)
                        )
                    }

                    override suspend fun onSuccess(response: Any?) {
                        val launchSession: LaunchSession =
                            LaunchSession.launchSessionForAppId(appInfo.id)
                        launchSession.appName = appInfo.name
                        launchSession.sessionId = response as? String
                        launchSession.service = this@DIALService
                        launchSession.sessionType = LaunchSessionType.App
                        Util.postSuccess(listener, launchSession)
                    }
                })

        command.send()
    }

    override fun launchBrowser(url: String?, listener: AppLaunchListener?) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    override suspend fun closeApp(launchSession: LaunchSession, listener: ResponseListener<Any?>) {
        getAppState(launchSession.appName, object : AppStateListener {
            override suspend fun onSuccess(state: AppState) {
                var uri: String? = requestURL(launchSession.appName)

                uri = if ((launchSession.sessionId!!.contains("http://") ||
                        launchSession.sessionId!!.contains("https://"))
                ) {
                    launchSession.sessionId!!
                } else if ((launchSession.sessionId!!.endsWith("run")
                        || launchSession.sessionId!!.endsWith("run/"))
                ) {
                    requestURL(launchSession.getAppId() + "/run")
                } else {
                    requestURL(launchSession.sessionId)
                }

                val command: ServiceCommand<ResponseListener<Any>> =
                    ServiceCommand(
                        launchSession.service,
                        uri, null, listener
                    )
                command.httpMethod = ServiceCommand.TYPE_DEL
                command.send()
            }

            override fun onError(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        })
    }

    override suspend fun launchYouTube(contentId: String?, listener: AppLaunchListener?) {
        launchYouTube(contentId, 0.0.toFloat(), listener)
    }

    override suspend fun launchYouTube(contentId: String?, startTime: Float, listener: AppLaunchListener?) {
        var params: String? = null
        val appInfo: AppInfo = AppInfo("YouTube", "YouTube")
        if (!contentId.isNullOrEmpty()) {
            if (startTime < 0.0) {
                listener?.onError(ServiceCommandError(0, "Start time may not be negative", null))
                return
            }

            val pairingCode: String = uuid4().toString()
            params = "pairingCode=%s&v=%s&t=%.1f".format(pairingCode, contentId, startTime)
        }

        launchAppWithInfo(appInfo, params, listener)
    }

    override fun launchHulu(contentId: String?, listener: AppLaunchListener?) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    override suspend fun launchNetflix(contentId: String?, listener: AppLaunchListener) {
        var params: JsonObject? = null

        if (!contentId.isNullOrEmpty()) {
            try {
                params = JsonObject(mapOf("v" to JsonPrimitive(contentId)))
            } catch (e: Exception) {
                Logger.e(Util.T, e, message = { "Launch Netflix error" })
            }
        }

        val appInfo = AppInfo(APP_NETFLIX)
        launchAppWithInfo(appInfo, params, listener)
    }

    override fun launchAppStore(appId: String?, listener: AppLaunchListener) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    private suspend fun getAppState(appName: String?, listener: AppStateListener) {
        val responseListener: ResponseListener<Any?> = object : ResponseListener<Any?> {
            override suspend fun onSuccess(response: Any?) {
                val str: String = response as String
                val stateTAG: Array<String?> = arrayOfNulls(2)
                stateTAG[0] = "<state>"
                stateTAG[1] = "</state>"


                var start: Int = str.indexOf(stateTAG[0]!!)
                val end: Int = str.indexOf(stateTAG[1]!!)

                if (start != -1 && end != -1) {
                    start += stateTAG[0]!!.length

                    val state: String = str.substring(start, end)
                    val appState: AppState = AppState(("running" == state), ("running" == state))

                    Util.postSuccess(listener, appState)
                    // TODO: This isn't actually reporting anything.
//                    if (listener != null) 
//                        listener.onAppStateSuccess(state);
                } else {
                    Util.postError(
                        listener,
                        ServiceCommandError(0, "Malformed response for app state", null)
                    )
                }
            }

            override fun onError(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        }

        val uri: String = requestURL(appName)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(
                commandProcessor, uri, null,
                responseListener
            )
        request.httpMethod = ServiceCommand.TYPE_GET

        request.send()
    }

    override fun getAppList(listener: AppListListener) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    override fun getRunningApp(listener: AppInfoListener) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    override fun subscribeRunningApp(listener: AppInfoListener): ServiceSubscription<AppInfoListener> {
        Util.postError(listener, ServiceCommandError.notSupported())

        return NotSupportedServiceSubscription()
    }

    override fun getAppState(launchSession: LaunchSession, listener: AppStateListener?) {
        // TODO Auto-generated method stub
    }

    override fun subscribeAppState(
        launchSession: LaunchSession,
        listener: AppStateListener
    ): ServiceSubscription<AppStateListener>? {
        // TODO Auto-generated method stub
        return null
    }

    override suspend fun closeLaunchSession(
        launchSession: LaunchSession?,
        listener: ResponseListener<Any?>
    ) {
        if (launchSession?.sessionType == LaunchSessionType.App) {
            getLauncher().closeApp(launchSession, listener)
        } else {
            Util.postError(
                listener,
                ServiceCommandError(
                    -1,
                    "Could not find a launcher associated with this LaunchSession",
                    launchSession
                )
            )
        }
    }

    override fun isConnectable(): Boolean {
        return true
    }

    override fun isConnected(): Boolean {
        return connected
    }

    override suspend fun connect() {
        //  TODO:  Fix this for roku.  Right now it is using the InetAddress reachable function.  Need to use an HTTP Method.
//        mServiceReachability = DeviceServiceReachability.getReachability(serviceDescription.getIpAddress(), this);
//        mServiceReachability.start();

        connected = true

        reportConnected(true)
    }

    override suspend fun disconnect() {
        connected = false

        if (mServiceReachability != null) mServiceReachability!!.stop()

        Util.runOnUI {
            if (listener != null) listener!!.onDisconnect(this@DIALService, null)
        }
    }

    override suspend fun onLoseReachability(reachability: DeviceServiceReachability?) {
        if (connected) {
            disconnect()
        } else {
            mServiceReachability!!.stop()
        }
    }

    override suspend fun sendCommand(mCommand: ServiceCommand<*>) {
        Util.runInBackground {

            val command: ServiceCommand<ResponseListener<Any>> =
                mCommand as ServiceCommand<ResponseListener<Any>>
            val payload: Any? = command.payload

            try {
                val connection: HttpConnection = createHttpConnection(mCommand.target)
                if (payload != null || command.httpMethod
                        .equals(ServiceCommand.TYPE_POST, ignoreCase = true)
                ) {
                    connection.setMethod(HttpConnection.Method.POST)
                    if (payload != null) {
                        connection.addHeader(
                            HttpMessage.CONTENT_TYPE_HEADER, "text/plain; " +
                                "charset=\"utf-8\""
                        )
                        connection.setPayload(payload.toString())
                    }
                } else if (command.httpMethod
                        .equals(ServiceCommand.TYPE_DEL, ignoreCase = true)
                ) {
                    connection.setMethod(HttpConnection.Method.DELETE)
                }
                connection.execute()
                val code: Int = connection.getResponseCode()
                when (code) {
                    200 -> {
                        Util.postSuccess(
                            command.responseListener,
                            connection.getResponseString()
                        )
                    }

                    201 -> {
                        Util.postSuccess(
                            command.responseListener,
                            connection.getResponseHeader("Location")
                        )
                    }

                    else -> {
                        Util.postError(
                            command.responseListener,
                            ServiceCommandError.getError(code)
                        )
                    }
                }
            } catch (e: Exception) {
                Util.postError(
                    command.responseListener,
                    ServiceCommandError(0, e.message, null)
                )
            }
        }
    }

    fun createHttpConnection(target: String): HttpConnection {
        return HttpConnection.newInstance(Url(target))
    }

    private fun requestURL(appName: String?): String {
        val applicationURL: String = _serviceDescription?.applicationURL
            ?: throw IllegalStateException("DIAL service application URL not available")


        val sb: StringBuilder = StringBuilder()

        sb.append(applicationURL)

        if (!applicationURL.endsWith("/")) sb.append("/")

        sb.append(appName)

        return sb.toString()
    }

    override suspend fun updateCapabilities() {
        val capabilities: MutableList<String> = ArrayList()

        capabilities.add(Launcher.Application)
        capabilities.add(Launcher.Application_Params)
        capabilities.add(Launcher.Application_Close)
        capabilities.add(Launcher.AppState)

        setCapabilities(capabilities)
    }

    private suspend fun hasApplication(appID: String, listener: ResponseListener<Any?>) {
        val uri: String = requestURL(appID)

        val command: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(commandProcessor, uri, null, listener)
        command.httpMethod = ServiceCommand.TYPE_GET
        command.send()
    }

    private suspend fun probeForAppSupport() {
        if (_serviceDescription?.applicationURL == null) {
            Logger.d(
                Util.T,
                message = { "unable to check for installed app; no service application url" })
            return
        }

        for (appID: String in registeredApps) {
            hasApplication(appID, object : ResponseListener<Any?> {
                override fun onError(error: ServiceCommandError) {}

                override suspend fun onSuccess(response: Any?) {
                    addCapability("Launcher.$appID")
                    addCapability("Launcher.$appID.Params")
                }
            })
        }
    }

    companion object {
        val ID: String = "DIAL"
        private const val APP_NETFLIX: String = "Netflix"

        private val registeredApps: MutableList<String> = ArrayList()

        init {
            registeredApps.add("YouTube")
            registeredApps.add("Netflix")
            registeredApps.add("Amazon")
        }

        fun registerApp(appId: String) {
            if (!registeredApps.contains(appId)) registeredApps.add(appId)
        }

        fun discoveryFilter(): DiscoveryFilter {
            return DiscoveryFilter(ID, "urn:dial-multiscreen-org:service:dial:1")
        }

        fun getServiceProvider() =
            DeviceServiceProvider(kClass = DIALService::class, constructor = { serviceDescription, serviceConfig ->
                DIALService(serviceDescription, serviceConfig)
            }, discoverFilter = {
                discoveryFilter()
            })
    }
}
