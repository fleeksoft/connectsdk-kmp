/*
 * TVControl
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

import com.fleeksoft.connectsdk.core.ChannelInfo
import com.fleeksoft.connectsdk.core.ProgramInfo
import com.fleeksoft.connectsdk.core.ProgramList
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceSubscription

interface TVControl : CapabilityMethods {
    val tVControl: TVControl
    val tVControlCapabilityLevel: CapabilityPriorityLevel?

    fun channelUp(listener: ResponseListener<Any?>?)
    fun channelDown(listener: ResponseListener<Any?>?)

    fun setChannel(channelNumber: ChannelInfo?, listener: ResponseListener<Any?>)

    fun getCurrentChannel(listener: ChannelListener?)
    fun subscribeCurrentChannel(listener: ChannelListener?): ServiceSubscription<ChannelListener?>?

    fun getChannelList(listener: ChannelListListener?)

    fun getProgramInfo(listener: ProgramInfoListener?)
    fun subscribeProgramInfo(listener: ProgramInfoListener?): ServiceSubscription<ProgramInfoListener>?

    fun getProgramList(listener: ProgramListListener?)
    fun subscribeProgramList(listener: ProgramListListener?): ServiceSubscription<ProgramListListener?>?

    fun get3DEnabled(listener: State3DModeListener)
    fun set3DEnabled(enabled: Boolean, listener: ResponseListener<Any?>)
    fun subscribe3DEnabled(listener: State3DModeListener): ServiceSubscription<State3DModeListener?>

    /**
     * Success block that is called upon successfully getting the TV's 3D mode
     *
     * Passes a Boolean to see Whether 3D mode is currently enabled on the TV
     */
    interface State3DModeListener : ResponseListener<Boolean?>

    /**
     * Success block that is called upon successfully getting the current channel's information.
     *
     * Passes a ChannelInfo object containing information about the current channel
     */
    interface ChannelListener : ResponseListener<ChannelInfo?>

    /**
     * Success block that is called upon successfully getting the channel list.
     *
     * Passes a List of ChannelList objects for each available channel on the TV
     */
    interface ChannelListListener : ResponseListener<List<ChannelInfo?>?>

    /**
     * Success block that is called upon successfully getting the current program's information.
     *
     * Passes a ProgramInfo object containing information about the current program
     */
    interface ProgramInfoListener : ResponseListener<ProgramInfo?>

    /**
     * Success block that is called upon successfully getting the program list for the current channel.
     *
     * Passes a ProgramList containing a ProgramInfo object for each available program on the TV's current channel
     */
    interface ProgramListListener : ResponseListener<ProgramList?>
    companion object {
        val Any: String = "TVControl.Any"

        val Channel_Get: String = "TVControl.Channel.Get"
        val Channel_Set: String = "TVControl.Channel.Set"
        val Channel_Up: String = "TVControl.Channel.Up"
        val Channel_Down: String = "TVControl.Channel.Down"
        val Channel_List: String = "TVControl.Channel.List"
        val Channel_Subscribe: String = "TVControl.Channel.Subscribe"
        val Program_Get: String = "TVControl.Program.Get"
        val Program_List: String = "TVControl.Program.List"
        val Program_Subscribe: String = "TVControl.Program.Subscribe"
        val Program_List_Subscribe: String = "TVControl.Program.List.Subscribe"
        val Get_3D: String = "TVControl.3D.Get"
        val Set_3D: String = "TVControl.3D.Set"
        val Subscribe_3D: String = "TVControl.3D.Subscribe"

        val Capabilities: Array<String> = arrayOf(
            Channel_Get,
            Channel_Set,
            Channel_Up,
            Channel_Down,
            Channel_List,
            Channel_Subscribe,
            Program_Get,
            Program_List,
            Program_Subscribe,
            Program_List_Subscribe,
            Get_3D,
            Set_3D,
            Subscribe_3D
        )
    }
}
