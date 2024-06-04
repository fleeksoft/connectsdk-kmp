/*
 * MediaPlayer
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on Jan 19 2014
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

import com.fleeksoft.connectsdk.core.MediaInfo
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceSubscription
import com.fleeksoft.connectsdk.service.sessions.LaunchSession

interface MediaPlayer : CapabilityMethods {
    fun getMediaPlayer(): MediaPlayer?

    fun getMediaPlayerCapabilityLevel(): CapabilityPriorityLevel?

    suspend fun getMediaInfo(listener: MediaInfoListener)

    suspend fun subscribeMediaInfo(listener: MediaInfoListener): ServiceSubscription<MediaInfoListener>?

    suspend fun displayImage(mediaInfo: MediaInfo, listener: LaunchListener?)

    suspend fun playMedia(mediaInfo: MediaInfo, shouldLoop: Boolean, listener: LaunchListener?)

    suspend fun closeMedia(launchSession: LaunchSession, listener: ResponseListener<Any?>)

    /**
     * Success block that is called upon successfully playing/displaying a media file.
     *
     * Passes a MediaLaunchObject which contains the objects for controlling media playback.
     */
    interface LaunchListener : ResponseListener<MediaLaunchObject?>

    /**
     * Helper class used with the MediaPlayer.LaunchListener to return the current media playback.
     */
    class MediaLaunchObject {
        /** The LaunchSession object for the media launched.  */
        var launchSession: LaunchSession

        /** The MediaControl object for the media launched.  */
        var mediaControl: MediaControl?

        /** The PlaylistControl object for the media launched  */
        var playlistControl: PlaylistControl? = null

        constructor(launchSession: LaunchSession, mediaControl: MediaControl?) {
            this.launchSession = launchSession
            this.mediaControl = mediaControl
        }

        constructor(
            launchSession: LaunchSession,
            mediaControl: MediaControl?,
            playlistControl: PlaylistControl?,
        ) {
            this.launchSession = launchSession
            this.mediaControl = mediaControl
            this.playlistControl = playlistControl
        }
    }

    interface MediaInfoListener : ResponseListener<MediaInfo?>

    companion object {
        val Any: String = "MediaPlayer.Any"


        val Display_Image: String = "MediaPlayer.Display.Image"
        val Play_Video: String = "MediaPlayer.Play.Video"
        val Play_Audio: String = "MediaPlayer.Play.Audio"
        val Play_Playlist: String = "MediaPlayer.Play.Playlist"
        val Close: String = "MediaPlayer.Close"
        val Loop: String = "MediaPlayer.Loop"
        val Subtitle_SRT: String = "MediaPlayer.Subtitle.SRT"
        val Subtitle_WebVTT: String = "MediaPlayer.Subtitle.WebVTT"

        val MetaData_Title: String = "MediaPlayer.MetaData.Title"
        val MetaData_Description: String = "MediaPlayer.MetaData.Description"
        val MetaData_Thumbnail: String = "MediaPlayer.MetaData.Thumbnail"
        val MetaData_MimeType: String = "MediaPlayer.MetaData.MimeType"

        val MediaInfo_Get: String = "MediaPlayer.MediaInfo.Get"
        val MediaInfo_Subscribe: String = "MediaPlayer.MediaInfo.Subscribe"

        val Capabilities: Array<String> = arrayOf(
            Display_Image,
            Play_Video,
            Play_Audio,
            Close,
            MetaData_Title,
            MetaData_Description,
            MetaData_Thumbnail,
            MetaData_MimeType,
            MediaInfo_Get,
            MediaInfo_Subscribe
        )
    }
}
