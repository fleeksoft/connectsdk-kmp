/*
 * WebAppSession
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Jeffrey Glenn on 07 Mar 2014
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
package com.fleeksoft.connectsdk.service.sessions

import com.fleeksoft.connectsdk.core.MediaInfo
import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.service.DeviceService
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.MediaControl
import com.fleeksoft.connectsdk.service.capability.MediaControl.*
import com.fleeksoft.connectsdk.service.capability.MediaPlayer
import com.fleeksoft.connectsdk.service.capability.MediaPlayer.MediaInfoListener
import com.fleeksoft.connectsdk.service.capability.PlaylistControl
import com.fleeksoft.connectsdk.service.capability.PlaylistControl.PlayMode
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import com.fleeksoft.connectsdk.service.command.ServiceSubscription
import kotlinx.serialization.json.JsonObject

/**
 * ###Overview When a web app is launched on a first screen device, there are
 * certain tasks that can be performed with that web app. WebAppSession serves
 * as a second screen reference of the web app that was launched. It behaves
 * similarly to LaunchSession, but is not nearly as static.
 *
 * ###In Depth On top of maintaining session information (contained in the
 * launchSession property), WebAppSession provides access to a number of
 * capabilities. - MediaPlayer - MediaControl - Bi-directional communication
 * with web app
 *
 * MediaPlayer and MediaControl are provided to allow for the most common first
 * screen use cases -- a media player (audio, video, & images).
 *
 * The Connect SDK JavaScript Bridge has been produced to provide normalized
 * support for these capabilities across protocols (Chromecast, webOS, etc).
 */
/**
 * Instantiates a WebAppSession object with all the information necessary to
 * interact with a web app.
 *
 * @param launchSession
 * LaunchSession containing info about the web app session
 * @param _service
 * DeviceService that was responsible for launching this web app
 */
open class WebAppSession(
    /**
     * LaunchSession object containing key session information. Much of this
     * information is required for web app messaging & closing the web app.
     */
    var launchSession: LaunchSession?, // @cond INTERNAL
    private var _service: DeviceService?,
) : MediaControl, MediaPlayer, PlaylistControl {
    /** Status of the web app  */
    enum class WebAppStatus {
        /** Web app status is unknown  */
        Unknown,

        /** Web app is running and in the foreground  */
        Open,

        /** Web app is running and in the background  */
        Background,

        /** Web app is in the foreground but has not started running yet  */
        Foreground,

        /** Web app is not running and is not in the foreground or background  */
        Closed
    }

    /**
     * Success block that is called upon successfully getting a web app's status.
     *
     * @param status The current running & foreground status of the web app
     */
    interface WebAppPinStatusListener : ResponseListener<Boolean?>

    /**
     * When messages are received from a web app, they are parsed into the
     * appropriate object type (string vs JSON/NSDictionary) and routed to the
     * WebAppSessionListener.
     */
    /**
     * When messages are received from a web app, they are parsed into the
     * appropriate object type (string vs JSON/NSDictionary) and routed to the
     * WebAppSessionListener.
     *
     * @param listener
     * WebAppSessionListener to be called when messages are received
     * from the web app
     */
    var webAppSessionListener: WebAppSessionListener? = null

    // @endcond

    /**
     * DeviceService that was responsible for launching this web app.
     */
    protected fun setService(service: DeviceService?) {
    }


    /**
     * Subscribes to changes in the web app's status.
     *
     * @param listener
     * (optional) MessageListener to be called on app status change
     */
    fun subscribeWebAppStatus(
        listener: MessageListener?,
    ): ServiceSubscription<MessageListener>? {
        listener?.onError(ServiceCommandError.notSupported())

        return null
    }

    /**
     * Establishes a communication channel with the web app.
     *
     * @param connectionListener
     * (optional) ResponseListener to be called on success
     */
    open fun connect(connectionListener: ResponseListener<Any>?) {
        Util.postError(connectionListener, ServiceCommandError.notSupported())
    }

    /**
     * Establishes a communication channel with a currently running web app.
     *
     * @param connectionListener
     */
    open fun join(connectionListener: ResponseListener<Any>?) {
        Util.postError(connectionListener, ServiceCommandError.notSupported())
    }

    /**
     * Closes any open communication channel with the web app.
     */
    open fun disconnectFromWebApp() {
    }

    /**
     * Pin the web app on the launcher.
     */
    open fun pinWebApp(webAppId: String?, listener: ResponseListener<Any?>?) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    /**
     * UnPin the web app on the launcher.
     *
     * @param webAppId NSString webAppId to be unpinned.
     */
    open fun unPinWebApp(webAppId: String?, listener: ResponseListener<Any?>?) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    /**
     * To check if the web app is pinned or not
     */
    open fun isWebAppPinned(webAppId: String?, listener: WebAppPinStatusListener?) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    /**
     * Subscribe to check if the web app is pinned or not
     */
    open fun subscribeIsWebAppPinned(
        webAppId: String?,
        listener: WebAppPinStatusListener?,
    ): ServiceSubscription<WebAppPinStatusListener>? {
        Util.postError(listener, ServiceCommandError.notSupported())
        return null
    }

    /**
     * Closes the web app on the first screen device.
     *
     * @param listener
     * (optional) ResponseListener to be called on success
     */
    open fun close(listener: ResponseListener<Any?>?) {
        listener?.onError(ServiceCommandError.notSupported())
    }

    /**
     * Sends a simple string to the web app. The Connect SDK JavaScript Bridge
     * will receive this message and hand it off as a string object.
     *
     * @param listener
     * (optional) ResponseListener to be called on success
     */
    open fun sendMessage(message: String?, listener: ResponseListener<Any?>?) {
        listener?.onError(ServiceCommandError.notSupported())
    }

    /**
     * Sends a JSON object to the web app. The Connect SDK JavaScript Bridge
     * will receive this message and hand it off as a JavaScript object.
     *
     * @param success
     * (optional) ResponseListener to be called on success
     */
    open fun sendMessage(
        message: JsonObject?,
        listener: ResponseListener<Any?>?,
    ) {
        listener?.onError(ServiceCommandError.notSupported())
    }

    override fun getMediaControl(): MediaControl? = null

    override fun getMediaControlCapabilityLevel(): CapabilityPriorityLevel? =
        CapabilityPriorityLevel.VERY_LOW

    override suspend fun getMediaInfo(listener: MediaInfoListener) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    override suspend fun subscribeMediaInfo(
        listener: MediaInfoListener,
    ): ServiceSubscription<MediaInfoListener>? {
        listener.onError(ServiceCommandError.notSupported())
        return null
    }

    override suspend fun play(listener: ResponseListener<Any?>) {
        var mediaControl: MediaControl? = null

        if (_service != null) mediaControl = _service!!.getAPI(MediaControl::class)

        if (mediaControl != null) {
            mediaControl.play(listener)
        } else {
            listener.onError(ServiceCommandError.notSupported())
        }
    }

    override suspend fun pause(listener: ResponseListener<Any?>) {
        var mediaControl: MediaControl? = null

        if (_service != null) mediaControl = _service!!.getAPI(MediaControl::class)

        if (mediaControl != null) mediaControl.pause(listener)
        else listener.onError(ServiceCommandError.notSupported())
    }

    override suspend fun stop(listener: ResponseListener<Any?>) {
        var mediaControl: MediaControl? = null

        if (_service != null) mediaControl = _service!!.getAPI(MediaControl::class)

        if (mediaControl != null) mediaControl.stop(listener)
        else listener.onError(ServiceCommandError.notSupported())
    }

    override fun rewind(listener: ResponseListener<Any?>) {
        var mediaControl: MediaControl? = null

        if (_service != null) mediaControl = _service!!.getAPI(MediaControl::class)

        if (mediaControl != null) mediaControl.rewind(listener)
        else listener.onError(ServiceCommandError.notSupported())
    }

    override fun fastForward(listener: ResponseListener<Any?>) {
        var mediaControl: MediaControl? = null

        if (_service != null) mediaControl = _service!!.getAPI(MediaControl::class)

        if (mediaControl != null) mediaControl.fastForward(listener)
        else listener.onError(ServiceCommandError.notSupported())
    }

    override suspend fun previous(listener: ResponseListener<Any?>) {
        // TODO: deprecated mediaController.previous replaced with playlistControl.previous

        if (getPlaylistControl() != null) getPlaylistControl()!!.previous(listener)
        else listener.onError(ServiceCommandError.notSupported())
    }

    override suspend fun next(listener: ResponseListener<Any?>) {
        // TODO: deprecated mediaController.next replaced with playlistControl.next

        if (getPlaylistControl() != null) getPlaylistControl()!!.next(listener)
        else listener.onError(ServiceCommandError.notSupported())
    }

    override suspend fun seek(position: Long, listener: ResponseListener<Any?>) {
        var mediaControl: MediaControl? = null

        if (_service != null) mediaControl = _service!!.getAPI(MediaControl::class)

        if (mediaControl != null) mediaControl.seek(position, listener)
        else listener.onError(ServiceCommandError.notSupported())
    }

    override suspend fun getDuration(listener: DurationListener) {
        var mediaControl: MediaControl? = null

        if (_service != null) mediaControl = _service!!.getAPI(MediaControl::class)

        mediaControl?.getDuration(listener)
            ?: listener?.onError(ServiceCommandError.notSupported())
    }

    override suspend fun getPosition(listener: PositionListener) {
        var mediaControl: MediaControl? = null

        if (_service != null) mediaControl = _service!!.getAPI(MediaControl::class)

        mediaControl?.getPosition(listener)
            ?: listener.onError(ServiceCommandError.notSupported())
    }

    override suspend fun getPlayState(listener: PlayStateListener) {
        var mediaControl: MediaControl? = null

        if (_service != null) mediaControl = _service!!.getAPI(MediaControl::class)

        mediaControl?.getPlayState(listener)
            ?: listener?.onError(ServiceCommandError.notSupported())
    }

    override suspend fun subscribePlayState(
        listener: PlayStateListener,
    ): ServiceSubscription<PlayStateListener>? {
        var mediaControl: MediaControl? = null

        if (_service != null) mediaControl = _service!!.getAPI(MediaControl::class)

        if (mediaControl != null) return mediaControl.subscribePlayState(listener)
        else listener?.onError(ServiceCommandError.notSupported())

        return null
    }

    override suspend fun closeMedia(
        launchSession: LaunchSession,
        listener: ResponseListener<Any?>,
    ) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    override suspend fun displayImage(mediaInfo: MediaInfo, listener: MediaPlayer.LaunchListener?) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    override suspend fun playMedia(
        mediaInfo: MediaInfo, shouldLoop: Boolean,
        listener: MediaPlayer.LaunchListener?,
    ) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    override fun getMediaPlayer(): MediaPlayer? = null

    override fun getMediaPlayerCapabilityLevel(): CapabilityPriorityLevel? =
        CapabilityPriorityLevel.VERY_LOW

    override fun getPlaylistControl(): PlaylistControl? = null

    override fun getPlaylistControlCapabilityLevel(): CapabilityPriorityLevel? =
        CapabilityPriorityLevel.VERY_LOW

    override suspend fun jumpToTrack(index: Long, listener: ResponseListener<Any?>) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    override suspend fun setPlayMode(playMode: PlayMode?, listener: ResponseListener<Any?>) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    // @endcond

    /**
     * Success block that is called upon successfully launch of a web app.
     *
     * Passes a WebAppSession Object containing important information about the
     * web app's session. This object is required to perform many functions with
     * the web app, including app-to-app communication, media playback, closing,
     * etc.
     */
    interface LaunchListener : ResponseListener<WebAppSession?>

    /**
     * Success block that is called upon successfully getting a web app's
     * status.
     *
     * Passes a WebAppStatus of the current running & foreground status of the
     * web app
     */
    interface StatusListener : ResponseListener<WebAppStatus?>

    // @cond INTERNAL
    interface MessageListener : ResponseListener<Any?> {
        fun onMessage(message: Any?)
    } // @endcond
}
