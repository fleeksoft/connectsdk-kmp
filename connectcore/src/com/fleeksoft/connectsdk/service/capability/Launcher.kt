/*
 * Launcher
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

import com.fleeksoft.connectsdk.core.AppInfo
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceSubscription
import com.fleeksoft.connectsdk.service.sessions.LaunchSession

interface Launcher : CapabilityMethods {
    fun getLauncher(): Launcher
    fun getLauncherCapabilityLevel(): CapabilityPriorityLevel?

    suspend fun launchAppWithInfo(appInfo: AppInfo, listener: AppLaunchListener?)
    suspend fun launchAppWithInfo(appInfo: AppInfo, params: Any?, listener: AppLaunchListener?)
    suspend fun launchApp(appId: String, listener: AppLaunchListener)

    suspend fun closeApp(launchSession: LaunchSession, listener: ResponseListener<Any?>)

    suspend fun getAppList(listener: AppListListener)

    suspend fun getRunningApp(listener: AppInfoListener)
    suspend fun subscribeRunningApp(listener: AppInfoListener): ServiceSubscription<AppInfoListener>

    fun getAppState(launchSession: LaunchSession, listener: AppStateListener?)
    fun subscribeAppState(
        launchSession: LaunchSession,
        listener: AppStateListener
    ): ServiceSubscription<AppStateListener>?

    suspend fun launchBrowser(url: String, listener: AppLaunchListener?)
    suspend fun launchYouTube(contentId: String, listener: AppLaunchListener?)
    suspend fun launchYouTube(contentId: String, startTime: Float, listener: AppLaunchListener?)
    suspend fun launchNetflix(contentId: String, listener: AppLaunchListener)
    suspend fun launchHulu(contentId: String, listener: AppLaunchListener?)
    suspend fun launchAppStore(appId: String, listener: AppLaunchListener)

    /**
     * Success listener that is called upon successfully launching an app.
     *
     * Passes a LaunchSession Object containing important information about the app's launch session
     */
    interface AppLaunchListener : ResponseListener<LaunchSession?>

    /**
     * Success listener that is called upon requesting info about the current running app.
     *
     * Passes an AppInfo object containing info about the running app
     */
    interface AppInfoListener : ResponseListener<AppInfo?>

    /**
     * Success block that is called upon successfully getting the app list.
     *
     * Passes a List containing an AppInfo for each available app on the device
     */
    interface AppListListener : ResponseListener<List<AppInfo?>?>

    // @cond INTERNAL
    interface AppCountListener : ResponseListener<Int?>

    // @endcond
    /**
     * Success block that is called upon successfully getting an app's state.
     *
     * Passes an AppState object which contains information about the running app.
     */
    interface AppStateListener : ResponseListener<AppState>

    /**
     * Helper class used with the AppStateListener to return the current state of an app.
     */
    class AppState(
        /** Whether the app is currently running.  */
        var running: Boolean,
        /** Whether the app is currently visible.  */
        var visible: Boolean
    )

    companion object {
        val Any: String = "Launcher.Any"

        val Application: String = "Launcher.App"
        val Application_Params: String = "Launcher.App.Params"
        val Application_Close: String = "Launcher.App.Close"
        val Application_List: String = "Launcher.App.List"
        val Browser: String = "Launcher.Browser"
        val Browser_Params: String = "Launcher.Browser.Params"
        val Hulu: String = "Launcher.Hulu"
        val Hulu_Params: String = "Launcher.Hulu.Params"
        val Netflix: String = "Launcher.Netflix"
        val Netflix_Params: String = "Launcher.Netflix.Params"
        val YouTube: String = "Launcher.YouTube"
        val YouTube_Params: String = "Launcher.YouTube.Params"
        val AppStore: String = "Launcher.AppStore"
        val AppStore_Params: String = "Launcher.AppStore.Params"
        val AppState: String = "Launcher.AppState"
        val AppState_Subscribe: String = "Launcher.AppState.Subscribe"
        val RunningApp: String = "Launcher.RunningApp"
        val RunningApp_Subscribe: String = "Launcher.RunningApp.Subscribe"

        val Capabilities: Array<String> = arrayOf(
            Application,
            Application_Params,
            Application_Close,
            Application_List,
            Browser,
            Browser_Params,
            Hulu,
            Hulu_Params,
            Netflix,
            Netflix_Params,
            YouTube,
            YouTube_Params,
            AppStore,
            AppStore_Params,
            AppState,
            AppState_Subscribe,
            RunningApp,
            RunningApp_Subscribe
        )
    }
}
