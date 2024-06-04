/*
 * PlaylistControl
 * Connect SDK
 * 
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 15 Jan 2015
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

/**
 * The PlaylistControl capability interface serves to define the methods required for normalizing
 * the control of playlist (next, previous, jumpToTrack, etc)
 */
interface PlaylistControl : CapabilityMethods {
    /**
     * Enumerates available playlist mode
     */
    enum class PlayMode {
        /**
         * Default mode, play tracks in sequence and stop at the end.
         */
        Normal,

        /**
         * Shuffle the playlist and play in sequeance.
         */
        Shuffle,

        /**
         * Repeat current track
         */
        RepeatOne,

        /**
         * Repeat entire playlist
         */
        RepeatAll,
    }

    fun getPlaylistControl(): PlaylistControl?

    fun getPlaylistControlCapabilityLevel(): CapabilityPriorityLevel?

    /**
     * Play previous track in the playlist
     * @param listener optional response listener
     */
    suspend fun previous(listener: ResponseListener<Any?>)

    /**
     * Play next track in the playlist
     * @param listener optional response listener
     */
    suspend fun next(listener: ResponseListener<Any?>)

    /**
     * Play a track specified by index in the playlist
     *
     * @param index index in the playlist, it starts from zero like index of array
     * @param listener optional response listener
     */
    suspend fun jumpToTrack(index: Long, listener: ResponseListener<Any?>)

    /**
     * Set order of playing tracks
     *
     * @param playMode
     * @param listener optional response listener
     */
    suspend fun setPlayMode(playMode: PlayMode?, listener: ResponseListener<Any?>)

    companion object {
        val Any: String = "PlaylistControl.Any"
        val JumpToTrack: String = "PlaylistControl.JumpToTrack"
        val SetPlayMode: String = "PlaylistControl.SetPlayMode"
        val Previous: String = "PlaylistControl.Previous"
        val Next: String = "PlaylistControl.Next"


        val Capabilities: Array<String> = arrayOf(
            Previous,
            Next,
            JumpToTrack,
            SetPlayMode,
            JumpToTrack,
        )
    }
}
