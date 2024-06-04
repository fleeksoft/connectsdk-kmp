/*
 * MouseControl
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

import com.fleeksoft.connectsdk.ported.PointF
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel

interface MouseControl : CapabilityMethods {
    val mouseControl: MouseControl
    val mouseControlCapabilityLevel: CapabilityPriorityLevel?

    fun connectMouse()
    fun disconnectMouse()

    fun click()
    fun move(dx: Double, dy: Double)
    fun move(distance: PointF)
    fun scroll(dx: Double, dy: Double)
    fun scroll(distance: PointF)

    companion object {
        val Any: String = "MouseControl.Any"

        val Connect: String = "MouseControl.Connect"
        val Disconnect: String = "MouseControl.Disconnect"
        val Click: String = "MouseControl.Click"
        val Move: String = "MouseControl.Move"
        val Scroll: String = "MouseControl.Scroll"

        val Capabilities: Array<String> = arrayOf(
            Connect,
            Disconnect,
            Click,
            Move,
            Scroll
        )
    }
}
