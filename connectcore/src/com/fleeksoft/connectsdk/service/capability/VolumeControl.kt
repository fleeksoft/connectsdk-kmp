/*
 * VolumeControl
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

interface VolumeControl : CapabilityMethods {
    fun getVolumeControl(): VolumeControl
    fun getVolumeControlCapabilityLevel(): CapabilityPriorityLevel?

    suspend fun volumeUp(listener: ResponseListener<Any?>)
    suspend fun volumeDown(listener: ResponseListener<Any?>)

    suspend fun setVolume(volume: Float, listener: ResponseListener<Any?>)
    suspend fun getVolume(listener: VolumeListener)

    suspend fun setMute(isMute: Boolean, listener: ResponseListener<Any?>)
    suspend fun getMute(listener: MuteListener?)

    suspend fun subscribeVolume(listener: VolumeListener): ServiceSubscription<VolumeListener>
    suspend fun subscribeMute(listener: MuteListener): ServiceSubscription<MuteListener>

    /**
     * Success block that is called upon successfully getting the device's system volume.
     *
     * Passes the current system volume, value is a float between 0.0 and 1.0
     */
    interface VolumeListener : ResponseListener<Float>

    /**
     * Success block that is called upon successfully getting the device's system mute status.
     *
     * Passes current system mute status
     */
    interface MuteListener : ResponseListener<Boolean>

    /**
     * Success block that is called upon successfully getting the device's system volume status.
     *
     * Passes current system mute status
     */
    interface VolumeStatusListener : ResponseListener<VolumeStatus>

    /**
     * Helper class used with the VolumeControl.VolueStatusListener to return the current volume status.
     */
    class VolumeStatus(var isMute: Boolean, var volume: Float)
    companion object {
        val Any: String = "VolumeControl.Any"

        val Volume_Get: String = "VolumeControl.Get"
        val Volume_Set: String = "VolumeControl.Set"
        val Volume_Up_Down: String = "VolumeControl.UpDown"
        val Volume_Subscribe: String = "VolumeControl.Subscribe"
        val Mute_Get: String = "VolumeControl.Mute.Get"
        val Mute_Set: String = "VolumeControl.Mute.Set"
        val Mute_Subscribe: String = "VolumeControl.Mute.Subscribe"

        val Capabilities: Array<String> = arrayOf(
            Volume_Get,
            Volume_Set,
            Volume_Up_Down,
            Volume_Subscribe,
            Mute_Get,
            Mute_Set,
            Mute_Subscribe
        )
    }
}
