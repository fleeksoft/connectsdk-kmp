/*
 * WebAppLauncher
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
package com.fleeksoft.connectsdk.service.capability

import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceSubscription
import com.fleeksoft.connectsdk.service.sessions.LaunchSession
import com.fleeksoft.connectsdk.service.sessions.WebAppSession
import com.fleeksoft.connectsdk.service.sessions.WebAppSession.WebAppPinStatusListener
import kotlinx.serialization.json.JsonObject

interface WebAppLauncher : CapabilityMethods {
    val webAppLauncher: WebAppLauncher
    val webAppLauncherCapabilityLevel: CapabilityPriorityLevel?

    fun launchWebApp(webAppId: String?, listener: WebAppSession.LaunchListener?)
    fun launchWebApp(
        webAppId: String?,
        relaunchIfRunning: Boolean,
        listener: WebAppSession.LaunchListener?
    )

    fun launchWebApp(
        webAppId: String?,
        params: JsonObject?,
        listener: WebAppSession.LaunchListener?
    )

    fun launchWebApp(
        webAppId: String?,
        params: JsonObject?,
        relaunchIfRunning: Boolean,
        listener: WebAppSession.LaunchListener?
    )

    fun joinWebApp(webAppLaunchSession: LaunchSession, listener: WebAppSession.LaunchListener?)
    fun joinWebApp(webAppId: String?, listener: WebAppSession.LaunchListener?)

    fun closeWebApp(launchSession: LaunchSession?, listener: ResponseListener<Any?>?)

    fun pinWebApp(webAppId: String?, listener: ResponseListener<Any?>?)
    fun unPinWebApp(webAppId: String?, listener: ResponseListener<Any?>?)
    fun isWebAppPinned(webAppId: String?, listener: WebAppPinStatusListener?)
    fun subscribeIsWebAppPinned(
        webAppId: String?,
        listener: WebAppPinStatusListener?
    ): ServiceSubscription<WebAppPinStatusListener>?

    companion object {
        val Any: String = "WebAppLauncher.Any"

        val Launch: String = "WebAppLauncher.Launch"
        val Launch_Params: String = "WebAppLauncher.Launch.Params"
        val Message_Send: String = "WebAppLauncher.Message.Send"
        val Message_Receive: String = "WebAppLauncher.Message.Receive"
        val Message_Send_JSON: String = "WebAppLauncher.Message.Send.JSON"
        val Message_Receive_JSON: String = "WebAppLauncher.Message.Receive.JSON"
        val Connect: String = "WebAppLauncher.Connect"
        val Disconnect: String = "WebAppLauncher.Disconnect"
        val Join: String = "WebAppLauncher.Join"
        val Close: String = "WebAppLauncher.Close"
        val Pin: String = "WebAppLauncher.Pin"

        val Capabilities: Array<String> = arrayOf(
            Launch,
            Launch_Params,
            Message_Send,
            Message_Receive,
            Message_Send_JSON,
            Message_Receive_JSON,
            Connect,
            Disconnect,
            Join,
            Close,
            Pin
        )
    }
}
