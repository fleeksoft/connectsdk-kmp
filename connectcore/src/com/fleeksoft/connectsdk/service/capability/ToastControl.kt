/*
 * ToastControl
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
import kotlinx.serialization.json.JsonObject

interface ToastControl : CapabilityMethods {
    val toastControl: ToastControl
    val toastControlCapabilityLevel: CapabilityPriorityLevel?

    fun showToast(message: String?, listener: ResponseListener<Any?>?)
    fun showToast(
        message: String?,
        iconData: String?,
        iconExtension: String?,
        listener: ResponseListener<Any?>?
    )

    fun showClickableToastForApp(
        message: String?,
        appInfo: AppInfo?,
        params: JsonObject?,
        listener: ResponseListener<Any?>?
    )

    fun showClickableToastForApp(
        message: String?,
        appInfo: AppInfo?,
        params: JsonObject?,
        iconData: String?,
        iconExtension: String?,
        listener: ResponseListener<Any?>?
    )

    fun showClickableToastForURL(message: String?, url: String?, listener: ResponseListener<Any?>?)
    fun showClickableToastForURL(
        message: String?,
        url: String?,
        iconData: String?,
        iconExtension: String?,
        listener: ResponseListener<Any?>?
    )

    companion object {
        val Any: String = "ToastControl.Any"

        val Show_Toast: String = "ToastControl.Show"
        val Show_Clickable_Toast_App: String = "ToastControl.Show.Clickable.App"
        val Show_Clickable_Toast_App_Params: String = "ToastControl.Show.Clickable.App.Params"
        val Show_Clickable_Toast_URL: String = "ToastControl.Show.Clickable.URL"

        val Capabilities: Array<String> = arrayOf(
            Show_Toast,
            Show_Clickable_Toast_App,
            Show_Clickable_Toast_App_Params,
            Show_Clickable_Toast_URL
        )
    }
}