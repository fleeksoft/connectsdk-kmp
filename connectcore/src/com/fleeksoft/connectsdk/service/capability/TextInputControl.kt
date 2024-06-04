/*
 * TextInputControl
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

import com.fleeksoft.connectsdk.core.TextInputStatusInfo
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceSubscription

interface TextInputControl : CapabilityMethods {
    val textInputControl: TextInputControl
    val textInputControlCapabilityLevel: CapabilityPriorityLevel?

    fun subscribeTextInputStatus(listener: TextInputStatusListener?): ServiceSubscription<TextInputStatusListener?>?

    fun sendText(input: String?)
    fun sendEnter()
    fun sendDelete()

    /**
     * Response block that is fired on any change of keyboard visibility.
     *
     * Passes TextInputStatusInfo object that provides keyboard type & visibility information
     */
    interface TextInputStatusListener : ResponseListener<TextInputStatusInfo?>
    companion object {
        val Any: String = "TextInputControl.Any"

        val Send: String = "TextInputControl.Send"
        val Send_Enter: String = "TextInputControl.Enter"
        val Send_Delete: String = "TextInputControl.Delete"
        val Subscribe: String = "TextInputControl.Subscribe"

        val Capabilities: Array<String> = arrayOf(
            Send,
            Send_Enter,
            Send_Delete,
            Subscribe
        )
    }
}
