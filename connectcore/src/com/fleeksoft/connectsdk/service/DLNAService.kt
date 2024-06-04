/*
 * DLNAService
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
package com.fleeksoft.connectsdk.service

import com.fleeksoft.connectsdk.core.MediaInfo
import com.fleeksoft.connectsdk.core.SubtitleInfo
import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.discovery.DiscoveryFilter
import com.fleeksoft.connectsdk.discovery.provider.ssdp.Service
import com.fleeksoft.connectsdk.etc.helper.DeviceServiceReachability
import com.fleeksoft.connectsdk.etc.helper.HttpConnection
import com.fleeksoft.connectsdk.ported.DeviceServiceProvider
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods
import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.MediaControl
import com.fleeksoft.connectsdk.service.capability.MediaControl.DurationListener
import com.fleeksoft.connectsdk.service.capability.MediaControl.PlayStateListener
import com.fleeksoft.connectsdk.service.capability.MediaControl.PlayStateStatus
import com.fleeksoft.connectsdk.service.capability.MediaControl.PositionListener
import com.fleeksoft.connectsdk.service.capability.MediaPlayer
import com.fleeksoft.connectsdk.service.capability.MediaPlayer.MediaInfoListener
import com.fleeksoft.connectsdk.service.capability.MediaPlayer.MediaLaunchObject
import com.fleeksoft.connectsdk.service.capability.PlaylistControl
import com.fleeksoft.connectsdk.service.capability.PlaylistControl.PlayMode
import com.fleeksoft.connectsdk.service.capability.VolumeControl
import com.fleeksoft.connectsdk.service.capability.VolumeControl.MuteListener
import com.fleeksoft.connectsdk.service.capability.VolumeControl.VolumeListener
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceCommand
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import com.fleeksoft.connectsdk.service.command.ServiceSubscription
import com.fleeksoft.connectsdk.service.command.URLServiceSubscription
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import com.fleeksoft.connectsdk.service.sessions.LaunchSession
import com.fleeksoft.connectsdk.service.sessions.LaunchSession.LaunchSessionType
import com.fleeksoft.connectsdk.service.upnp.DLNAHttpServer
import com.fleeksoft.connectsdk.service.upnp.DLNAMediaInfoParser
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import korlibs.io.lang.IOException
import korlibs.io.serialization.xml.Xml
import korlibs.io.serialization.xml.buildXml
import korlibs.io.serialization.xml.text
import korlibs.util.format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.JsonObject
import net.thauvin.erik.urlencoder.UrlEncoderUtil
import kotlin.reflect.KClass

class DLNAService constructor(
    serviceDescription: ServiceDescription?, serviceConfig: ServiceConfig,
    dlnaServer: DLNAHttpServer = DLNAHttpServer(),
) : DeviceService(serviceDescription, serviceConfig), PlaylistControl, MediaControl, MediaPlayer,
    VolumeControl {
    var avTransportURL: String? = null
    var renderingControlURL: String? = null
    var connectionControlURL: String? = null

    var httpServer: DLNAHttpServer

    var SIDList: MutableMap<String?, String?> = HashMap()
    var resubscriptionTimer: Job? = null
    val scope = CoroutineScope(Dispatchers.Default)

    internal interface PositionInfoListener {
        fun onGetPositionInfoSuccess(positionInfoXml: String)
        fun onGetPositionInfoFailed(error: ServiceCommandError)
    }

    init {
        updateControlURL()
        httpServer = dlnaServer
    }

    override fun getPriorityLevel(clazz: KClass<out CapabilityMethods>): CapabilityPriorityLevel? {
        if ((clazz == MediaPlayer::class)) {
            return getMediaPlayerCapabilityLevel()
        } else if ((clazz == MediaControl::class)) {
            return getMediaControlCapabilityLevel()
        } else if ((clazz == VolumeControl::class)) {
            return getVolumeControlCapabilityLevel()
        } else if ((clazz == PlaylistControl::class)) {
            return getPlaylistControlCapabilityLevel()
        }
        return CapabilityPriorityLevel.NOT_SUPPORTED
    }


    override suspend fun setServiceDescription(serviceDescription: ServiceDescription) {
        super.setServiceDescription(serviceDescription)

        updateControlURL()
    }

    private fun updateControlURL() {
        val serviceList: List<Service>? = _serviceDescription?.serviceList

        if (serviceList != null) {
            for (i in serviceList.indices) {
                if (!serviceList[i].baseURL!!.endsWith("/")) {
                    serviceList[i].baseURL += "/"
                }

                if (serviceList[i].serviceType!!.contains(AV_TRANSPORT)) {
                    avTransportURL = makeControlURL(
                        serviceList[i].baseURL, serviceList[i].controlURL
                    )
                } else if ((serviceList[i].serviceType!!.contains(RENDERING_CONTROL)) && !(serviceList[i].serviceType!!.contains(
                        GROUP_RENDERING_CONTROL
                    ))
                ) {
                    renderingControlURL = makeControlURL(
                        serviceList[i].baseURL, serviceList[i].controlURL
                    )
                } else if ((serviceList[i].serviceType!!.contains(CONNECTION_MANAGER))) {
                    connectionControlURL = makeControlURL(
                        serviceList[i].baseURL, serviceList[i].controlURL
                    )
                }
            }
        }
    }

    fun makeControlURL(base: String?, path: String?): String? {
        if (base == null || path == null) {
            return null
        }
        if (path.startsWith("/")) {
            return base + path.substring(1)
        }
        return base + path
    }

    /******************
     * MEDIA PLAYER
     */
    override fun getMediaPlayer(): MediaPlayer? {
        return this
    }

    override fun getMediaPlayerCapabilityLevel(): CapabilityPriorityLevel? {
        return CapabilityPriorityLevel.NORMAL
    }

    override suspend fun getMediaInfo(listener: MediaInfoListener) {
        getPositionInfo(object : PositionInfoListener {
            override fun onGetPositionInfoSuccess(positionInfoXml: String) {
                scope.launch {
                    Util.runInBackground {
                        val baseUrl: String =
                            "http://" + (getServiceDescription()?.ipAddress) + ":" + (getServiceDescription()?.port)
                        val trackMetaData: String = parseData(positionInfoXml, "TrackMetaData")
                        val info: MediaInfo? = DLNAMediaInfoParser.getMediaInfo(trackMetaData, baseUrl)
                        Util.postSuccess(listener, info)
                    }
                }
            }

            override fun onGetPositionInfoFailed(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        })
    }

    override suspend fun subscribeMediaInfo(listener: MediaInfoListener): ServiceSubscription<MediaInfoListener> {
        val request: URLServiceSubscription<MediaInfoListener> =
            URLServiceSubscription(this, "info", null, null)
        request.addListener(listener)
        addSubscription(request)
        return request
    }

    private suspend fun displayMedia(
        url: String,
        subtitle: SubtitleInfo?,
        mimeType: String?,
        title: String?,
        description: String?,
        iconSrc: String?,
        listener: MediaPlayer.LaunchListener?,
    ) {
        val instanceId: String = "0"
        val mediaElements: Array<String> =
            mimeType!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val mediaType: String? = mediaElements[0]
        var mediaFormat: String? = mediaElements[1]

        if ((mediaType == null) || (mediaType.length == 0) || (mediaFormat == null) || (mediaFormat.length == 0)) {
            Util.postError(
                listener, ServiceCommandError(
                    0, "You must provide a valid mimeType (audio/*,  video/*, etc)", null
                )
            )
            return
        }

        mediaFormat = if (("mp3" == mediaFormat)) "mpeg" else mediaFormat
        val mMimeType: String = "$mediaType/$mediaFormat"

        val responseListener: ResponseListener<Any?> = object : ResponseListener<Any?> {
            override suspend fun onSuccess(response: Any?) {
                val method: String = "Play"

                val parameters: MutableMap<String, String> = HashMap()
                parameters["Speed"] = "1"

                val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, "0", parameters)

                val playResponseListener: ResponseListener<Any?> = object : ResponseListener<Any?> {
                    override suspend fun onSuccess(response: Any?) {
                        val launchSession: LaunchSession = LaunchSession()
                        launchSession.service = this@DLNAService
                        launchSession.sessionType = LaunchSessionType.Media

                        Util.postSuccess(
                            listener,
                            MediaLaunchObject(launchSession, this@DLNAService, this@DLNAService)
                        )
                    }

                    override fun onError(error: ServiceCommandError) {
                        Util.postError(listener, error)
                    }
                }

                val request: ServiceCommand<ResponseListener<Any>> =
                    ServiceCommand(this@DLNAService, method, payload, playResponseListener)
                request.send()
            }

            override fun onError(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        }

        val method: String = "SetAVTransportURI"
        val metadata: String? =
            getMetadata(url, subtitle, mMimeType, title ?: "", description, iconSrc)
        if (metadata == null) {
            Util.postError(listener, ServiceCommandError.getError(500))
            return
        }

        val params: MutableMap<String, String> = LinkedHashMap()
        try {
            params["CurrentURI"] = encodeURL(url)
        } catch (e: Exception) {
            Util.postError(listener, ServiceCommandError.getError(500))
            return
        }
        params["CurrentURIMetaData"] = metadata

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, params)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this@DLNAService, method, payload, responseListener)
        request.send()
    }

    override suspend fun displayImage(mediaInfo: MediaInfo, listener: MediaPlayer.LaunchListener?) {
        displayMedia(
            url = mediaInfo.url,
            subtitle = null,
            mimeType = mediaInfo.mimeType,
            title = mediaInfo.title,
            description = mediaInfo.description,
            iconSrc = mediaInfo.images?.firstOrNull()?.url,
            listener = listener
        )
    }

    override suspend fun playMedia(
        mediaInfo: MediaInfo, shouldLoop: Boolean,
        listener: MediaPlayer.LaunchListener?,
    ) {
        displayMedia(
            url = mediaInfo.url,
            subtitle = mediaInfo.subtitleInfo,
            mimeType = mediaInfo.mimeType,
            title = mediaInfo.title,
            description = mediaInfo.description,
            iconSrc = mediaInfo.images?.firstOrNull()?.url,
            listener = listener
        )
    }

    override suspend fun closeMedia(launchSession: LaunchSession, listener: ResponseListener<Any?>) {
        if (launchSession.service != null && launchSession.service is DLNAService) {
            (launchSession.service as DLNAService).stop(listener)
        }
    }

    /******************
     * MEDIA CONTROL
     */
    override fun getMediaControl(): MediaControl {
        return this
    }

    override fun getMediaControlCapabilityLevel(): CapabilityPriorityLevel {
        return CapabilityPriorityLevel.NORMAL
    }

    override suspend fun play(listener: ResponseListener<Any?>) {
        val method: String = "Play"
        val instanceId: String = "0"

        val parameters: MutableMap<String, String> = LinkedHashMap()
        parameters["Speed"] = "1"

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, parameters)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, listener)
        request.send()
    }

    override suspend fun pause(listener: ResponseListener<Any?>) {
        val method: String = "Pause"
        val instanceId: String = "0"

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, null)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, listener)
        request.send()
    }

    override suspend fun stop(listener: ResponseListener<Any?>) {
        val method: String = "Stop"
        val instanceId: String = "0"

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, null)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, listener)
        request.send()
    }

    override fun rewind(listener: ResponseListener<Any?>) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    override fun fastForward(listener: ResponseListener<Any?>) {
        Util.postError(listener, ServiceCommandError.notSupported())
    }

    /******************
     * PLAYLIST CONTROL
     */
    override fun getPlaylistControl(): PlaylistControl {
        return this
    }

    override fun getPlaylistControlCapabilityLevel(): CapabilityPriorityLevel? {
        return CapabilityPriorityLevel.NORMAL
    }

    override suspend fun previous(listener: ResponseListener<Any?>) {
        val method: String = "Previous"
        val instanceId: String = "0"

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, null)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, listener)
        request.send()
    }

    override suspend fun next(listener: ResponseListener<Any?>) {
        val method: String = "Next"
        val instanceId: String = "0"

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, null)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, listener)
        request.send()
    }

    override suspend fun jumpToTrack(index: Long, listener: ResponseListener<Any?>) {
        // DLNA requires start index from 1. 0 is a special index which means the end of media.
        var index: Long = index
        ++index
        seek("TRACK_NR", index.toString(), listener)
    }

    override suspend fun setPlayMode(playMode: PlayMode?, listener: ResponseListener<Any?>) {
        val method: String = "SetPlayMode"
        val instanceId: String = "0"
        val mode: String

        when (playMode) {
            PlayMode.RepeatAll -> mode = "REPEAT_ALL"
            PlayMode.RepeatOne -> mode = "REPEAT_ONE"
            PlayMode.Shuffle -> mode = "SHUFFLE"
            else -> mode = "NORMAL"
        }
        val parameters: MutableMap<String, String> = LinkedHashMap()
        parameters["NewPlayMode"] = mode

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, parameters)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, listener)
        request.send()
    }

    override suspend fun seek(position: Long, listener: ResponseListener<Any?>) {
        val second: Long = (position / 1000) % 60
        val minute: Long = (position / (1000 * 60)) % 60
        val hour: Long = (position / (1000 * 60 * 60)) % 24
        val time: String = "%02d:%02d:%02d".format(hour, minute, second)
        seek("REL_TIME", time, listener)
    }

    private suspend fun getPositionInfo(listener: PositionInfoListener?) {
        val method: String = "GetPositionInfo"
        val instanceId: String = "0"

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, null)
        val responseListener: ResponseListener<Any?> = object : ResponseListener<Any?> {
            override suspend fun onSuccess(response: Any?) {
                (response as? String)?.let { listener?.onGetPositionInfoSuccess(it) }
            }

            override fun onError(error: ServiceCommandError) {
                listener?.onGetPositionInfoFailed(error)
            }
        }

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, responseListener)
        request.send()
    }

    override suspend fun getDuration(listener: DurationListener) {
        getPositionInfo(object : PositionInfoListener {
            override fun onGetPositionInfoSuccess(positionInfoXml: String) {
                val strDuration: String = parseData(positionInfoXml, "TrackDuration")

                val trackMetaData: String = parseData(positionInfoXml, "TrackMetaData")
                val info: MediaInfo = DLNAMediaInfoParser.getMediaInfo(trackMetaData)
                // Check if duration we get not equals 0 or media is image, otherwise wait 1 second and try again
                if ((strDuration != "0:00:00") || (info.mimeType.contains("image"))) {
                    val milliTimes: Long = convertStrTimeFormatToLong(strDuration)

                    Util.postSuccess(listener, milliTimes)
                } else {
                    scope.launch {
                        delay(1000)
                        getDuration(listener)
                    }
                }
            }

            override fun onGetPositionInfoFailed(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        })
    }

    override suspend fun getPosition(listener: PositionListener) {
        getPositionInfo(object : PositionInfoListener {
            override fun onGetPositionInfoSuccess(positionInfoXml: String) {
                val strDuration: String = parseData(positionInfoXml, "RelTime")

                val milliTimes: Long = convertStrTimeFormatToLong(strDuration)

                Util.postSuccess(listener, milliTimes)
            }

            override fun onGetPositionInfoFailed(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        })
    }

    protected suspend fun seek(unit: String, target: String, listener: ResponseListener<Any?>) {
        val method: String = "Seek"
        val instanceId: String = "0"

        val parameters: MutableMap<String, String> = LinkedHashMap()
        parameters["Unit"] = unit
        parameters["Target"] = target

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, parameters)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, listener)
        request.send()
    }

    private fun getMessageXml(
        serviceURN: String,
        method: String,
        instanceId: String?,
        params: Map<String, String>?,
    ): String? {
        try {

            val methodElement = buildXml(
                "u:$method", props = arrayOf(
                    // TODO: test set naemspace to serviceURN
                    "xmlns" to serviceURN,
                )
            ) {
                val instanceElement = buildXml("InstanceID") {
                    if (instanceId != null) {
                        text(instanceId)
                    }
                }
                if (instanceId != null) {
                    node(instanceElement)
                }
                if (params != null) {
                    for (entry: Map.Entry<String, String> in params.entries) {
                        val key: String = entry.key
                        val value: String = entry.value
                        val element = buildXml(key) {
                            text(value)
                        }
                        node(element)
                    }
                }
            }
            val bodyElement = buildXml("s:Body") {
                node(methodElement)
            }
            val root = buildXml(
                "s:Envelope", props = arrayOf(
                    "xmlns:s" to "http://schemas.xmlsoap.org/soap/envelope/",
                    "s:encodingStyle" to "http://schemas.xmlsoap.org/soap/encoding/"
                )
            ) {
                node(bodyElement)
            }

            return xmlToString(root, true)
        } catch (e: Exception) {
            return null
        }
    }

    private fun getMetadata(
        mediaURL: String,
        subtitle: SubtitleInfo?,
        mime: String,
        title: String,
        description: String?,
        iconUrl: String?,
    ): String? {
        try {
            var objectClass: String? = ""
            if (mime.startsWith("image")) {
                objectClass = "object.item.imageItem"
            } else if (mime.startsWith("video")) {
                objectClass = "object.item.videoItem"
            } else if (mime.startsWith("audio")) {
                objectClass = "object.item.audioItem"
            }

            val resElAttributes = mutableListOf<Pair<String, String>>()
            val itemElements = mutableListOf<Xml>()

            if (subtitle != null) {
                var mimeType: String =
                    if ((subtitle.mimeType == null)) DEFAULT_SUBTITLE_TYPE else subtitle.mimeType
                val typeParts: Array<String> =
                    mimeType.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = if (typeParts.size == 2) {
                    typeParts[1]
                } else {
                    mimeType = DEFAULT_SUBTITLE_MIMETYPE
                    DEFAULT_SUBTITLE_TYPE
                }


                resElAttributes.add("xmlns:pv" to "http://www.pv.com/pvns/")
                resElAttributes.add("pv:subtitleFileUri" to subtitle.getUrl())
                resElAttributes.add("pv:subtitleFileType" to type)

                val smiResElement = buildXml("res", props = arrayOf("protocolInfo" to "http-get:*:smi/caption")) {
                    text(subtitle.getUrl())
                }
                itemElements.add(smiResElement)

                val srtResElement =
                    buildXml("res", props = arrayOf("protocolInfo" to "http-get:*:$mimeType:")) {
                        text(subtitle.getUrl())
                    }
                itemElements.add(srtResElement)

                val captionInfoExElement = buildXml(
                    "sec:CaptionInfoEx", props = arrayOf("sec:type" to type)
                ) {
                    text(subtitle.getUrl())
                }
                itemElements.add(captionInfoExElement)

                val captionInfoElement =
                    buildXml("sec:CaptionInfo", props = arrayOf("sec:type" to type)) {
                        text(subtitle.getUrl())
                    }
                itemElements.add(captionInfoElement)
            }

            val root = buildXml(
                "DIDL-Lite", props = arrayOf(
                    "xmlns" to "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/",
                    "xmlns:upnp" to "urn:schemas-upnp-org:metadata-1-0/upnp/",
                    "xmlns:dc" to "http://purl.org/dc/elements/1.1/",
                    "xmlns:sec" to "http://www.sec.co.kr/",
                )
            ) {
                node(buildXml(
                    "item", props = arrayOf(
                        "id" to "1000", "parentID" to "0", "restricted" to "0"
                    )
                ) {
                    node(buildXml("dc:title") {
                        text(title)
                    })
                    node(buildXml("dc:description") {
                        text(description ?: "")
                    })
                    node(buildXml(
                        "res",
                        props = arrayOf("protocolInfo" to "http-get:*:$mime:DLNA.ORG_OP=01") + resElAttributes
                    ) {
                        text(encodeURL(mediaURL))
                    })
                    if (!iconUrl.isNullOrEmpty()) {
                        node(buildXml("upnp:albumArtURI") {
                            text(encodeURL(iconUrl))
                        })
                    }
                    node(buildXml("upnp:class") {
                        text(objectClass ?: "")
                    })

                    itemElements.forEach {
                        node(it)
                    }
                })
            }

            return xmlToString(root, false)
        } catch (e: Exception) {
            return null
        }
    }

    fun encodeURL(mediaURL: String): String {
        if (mediaURL.isEmpty()) {
            return ""
        }
        val decodedURL: String = UrlEncoderUtil.decode(mediaURL)
        if ((decodedURL == mediaURL)) {
            // TODO: convert to asccii string
//            return uri.toASCIIString()
        }
        return mediaURL
    }

    fun xmlToString(source: Xml, xmlDeclaration: Boolean): String {
        return source.toString()
    }


    override suspend fun sendCommand(mCommand: ServiceCommand<*>) {
        Util.runInBackground {

            val command: ServiceCommand<ResponseListener<Any>> =
                mCommand as ServiceCommand<ResponseListener<Any>>

            val method: String = command.target
            val payload: String? = command.payload as? String

            var targetURL: String? = null
            var serviceURN: String? = null

            if (payload == null) {
                Util.postError(
                    command.responseListener, ServiceCommandError(
                        0, "Cannot process the command, \"payload\" is missed", null
                    )
                )
                return@runInBackground
            }

            if (payload.contains(AV_TRANSPORT_URN)) {
                targetURL = avTransportURL
                serviceURN = AV_TRANSPORT_URN
            } else if (payload.contains(RENDERING_CONTROL_URN)) {
                targetURL = renderingControlURL
                serviceURN = RENDERING_CONTROL_URN
            } else if (payload.contains(CONNECTION_MANAGER_URN)) {
                targetURL = connectionControlURL
                serviceURN = CONNECTION_MANAGER_URN
            }

            if (serviceURN == null) {
                Util.postError(
                    command.responseListener, ServiceCommandError(
                        0, "Cannot process the command, \"serviceURN\" is missed", null
                    )
                )
                return@runInBackground
            }

            if (targetURL == null) {
                Util.postError(
                    command.responseListener, ServiceCommandError(
                        0, "Cannot process the command, \"targetURL\" is missed", null
                    )
                )
                return@runInBackground
            }

            try {
                val connection: HttpConnection = createHttpConnection(targetURL)
                connection.addHeader("Content-Type", "text/xml; charset=utf-8")
                connection.addHeader(
                    "SOAPAction", "\"%s#%s\"".format(serviceURN, method)
                )
                connection.setMethod(HttpConnection.Method.POST)
                connection.setPayload(payload)
                connection.execute()
                val code: Int = connection.getResponseCode()
                if (code == 200) {
                    Util.postSuccess(
                        command.responseListener, connection.getResponseString()
                    )
                } else {
                    Util.postError(
                        command.responseListener, ServiceCommandError.getError(code)
                    )
                }
            } catch (e: IOException) {
                Util.postError(
                    command.responseListener, ServiceCommandError(0, e.message, null)
                )
            }
        }
    }

    @Throws(IOException::class)
    fun createHttpConnection(targetURL: String): HttpConnection {
        return HttpConnection.newInstance(Url(targetURL))
    }

    override suspend fun updateCapabilities() {
        val capabilities: MutableList<String> = ArrayList()

        capabilities.add(MediaPlayer.Display_Image)
        capabilities.add(MediaPlayer.Play_Video)
        capabilities.add(MediaPlayer.Play_Audio)
        capabilities.add(MediaPlayer.Play_Playlist)
        capabilities.add(MediaPlayer.Close)
        capabilities.add(MediaPlayer.Subtitle_SRT)

        capabilities.add(MediaPlayer.MetaData_Title)
        capabilities.add(MediaPlayer.MetaData_MimeType)
        capabilities.add(MediaPlayer.MediaInfo_Get)
        capabilities.add(MediaPlayer.MediaInfo_Subscribe)

        capabilities.add(MediaControl.Play)
        capabilities.add(MediaControl.Pause)
        capabilities.add(MediaControl.Stop)
        capabilities.add(MediaControl.Seek)
        capabilities.add(MediaControl.Position)
        capabilities.add(MediaControl.Duration)
        capabilities.add(MediaControl.PlayState)
        capabilities.add(MediaControl.PlayState_Subscribe)

        // playlist capabilities
        capabilities.add(PlaylistControl.Next)
        capabilities.add(PlaylistControl.Previous)
        capabilities.add(PlaylistControl.JumpToTrack)
        capabilities.add(PlaylistControl.SetPlayMode)

        capabilities.add(VolumeControl.Volume_Set)
        capabilities.add(VolumeControl.Volume_Get)
        capabilities.add(VolumeControl.Volume_Up_Down)
        capabilities.add(VolumeControl.Volume_Subscribe)
        capabilities.add(VolumeControl.Mute_Get)
        capabilities.add(VolumeControl.Mute_Set)
        capabilities.add(VolumeControl.Mute_Subscribe)

        setCapabilities(capabilities)
    }

    override fun decodeLaunchSession(type: String, sessionObj: JsonObject): LaunchSession? {
        if ((type == "dlna")) {
            val launchSession: LaunchSession = LaunchSession.launchSessionFromJsonObject(sessionObj)
            launchSession.service = this

            return launchSession
        }
        return null
    }

    private fun isXmlEncoded(xml: String?): Boolean {
        if (xml == null || xml.length < 4) {
            return false
        }
        return (xml.trim { it <= ' ' }.substring(0, 4) == "&lt;")
    }

    fun parseData(response: String, key: String): String {/*if (isXmlEncoded(response)) {
            response = Html.fromHtml(response).toString()
        }*/
        val parser = Xml.parse(response ?: "")
        try {
            return parser[key].text/*var event: Int
            var isFound: Boolean = false
            do {
                event = parser.next()
                if (event == XmlPullParser.START_TAG) {
                    val tag: String = parser.getName()
                    if ((key == tag)) {
                        isFound = true
                    }
                } else if (event == XmlPullParser.TEXT && isFound) {
                    return parser.getText()
                }
            } while (event != XmlPullParser.END_DOCUMENT)*/
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun convertStrTimeFormatToLong(strTime: String): Long {
        val time1 = LocalDateTime.parse(strTime)
        val time2 = LocalDateTime.parse("00:00:00")
        return time1.toInstant(TimeZone.UTC).toEpochMilliseconds() - time2.toInstant(TimeZone.UTC)
            .toEpochMilliseconds()

        /*val df: SimpleDateFormat = SimpleDateFormat("HH:mm:ss")
        try {
            val d: Date = df.parse(strTime)
            val d2: Date = df.parse("00:00:00")
            time = d.getTime() - d2.getTime()
        } catch (e: ParseException) {
            Log.w(Util.T, "Invalid Time Format: " + strTime)
        } catch (e: NullPointerException) {
            Log.w(Util.T, "Null time argument")
        }

        return time*/
    }

    override suspend fun getPlayState(listener: PlayStateListener) {
        val method: String = "GetTransportInfo"
        val instanceId: String = "0"

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, null)

        val responseListener: ResponseListener<Any?> = object : ResponseListener<Any?> {
            override suspend fun onSuccess(response: Any?) {
                val transportState: String = parseData(response as String, "CurrentTransportState")
                val status: PlayStateStatus =
                    PlayStateStatus.convertTransportStateToPlayStateStatus(transportState)

                Util.postSuccess(listener, status)
            }

            override fun onError(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        }

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, responseListener)
        request.send()
    }

    override suspend fun subscribePlayState(listener: PlayStateListener): ServiceSubscription<PlayStateListener> {
        val request: URLServiceSubscription<PlayStateListener> =
            URLServiceSubscription(this, PLAY_STATE, null, null)
        request.addListener(listener)
        addSubscription(request)
        return request
    }

    private suspend fun addSubscription(subscription: URLServiceSubscription<*>) {
        if (!httpServer.isRunning) {
            Util.runInBackground {
                httpServer.start()
            }
            subscribeServices()
        }

        httpServer.getSubscriptions().add(subscription)
    }

    override suspend fun unsubscribe(subscription: URLServiceSubscription<*>?) {
        httpServer.getSubscriptions().remove(subscription)

        if (httpServer.getSubscriptions().isEmpty()) {
            unsubscribeServices()
        }
    }

    override fun isConnectable(): Boolean {
        return true
    }

    override fun isConnected(): Boolean {
        return connected
    }

    override suspend fun connect() {
        //  TODO:  Fix this for roku.  Right now it is using the InetAddress reachable function.  Need to use an HTTP Method.
//        mServiceReachability = DeviceServiceReachability.getReachability(serviceDescription.getIpAddress(), this);
//        mServiceReachability.start();

        connected = true

        reportConnected(true)
    }

    private suspend fun getDeviceCapabilities(listener: PositionInfoListener?) {
        val method: String = "GetDeviceCapabilities"
        val instanceId: String = "0"

        val payload: String? = getMessageXml(AV_TRANSPORT_URN, method, instanceId, null)
        val responseListener: ResponseListener<Any?> = object : ResponseListener<Any?> {
            override suspend fun onSuccess(response: Any?) {
                (response as? String)?.let { listener?.onGetPositionInfoSuccess(it) }
            }

            override fun onError(error: ServiceCommandError) {
                listener?.onGetPositionInfoFailed(error)
            }
        }

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, responseListener)
        request.send()
    }

    private suspend fun getProtocolInfo(listener: PositionInfoListener?) {
        val method: String = "GetProtocolInfo"
        val instanceId: String? = null

        val payload: String? = getMessageXml(CONNECTION_MANAGER_URN, method, instanceId, null)
        val responseListener: ResponseListener<Any?> = object : ResponseListener<Any?> {
            override suspend fun onSuccess(response: Any?) {
                (response as? String)?.let { listener?.onGetPositionInfoSuccess(it) }
            }

            override fun onError(error: ServiceCommandError) {
                listener?.onGetPositionInfoFailed(error)
            }
        }

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, responseListener)
        request.send()
    }

    override suspend fun disconnect() {
        connected = false

        if (mServiceReachability != null) mServiceReachability!!.stop()

        Util.runOnUI {
            listener?.onDisconnect(this@DLNAService, null)
        }

        Util.runInBackground {
            httpServer.stop()
        }
    }

    override suspend fun onLoseReachability(reachability: DeviceServiceReachability?) {
        if (connected) {
            disconnect()
        } else {
            mServiceReachability!!.stop()
        }
    }

    suspend fun subscribeServices() {
        Util.runInBackground {
            var myIpAddress: String? = null
            try {
                myIpAddress = Util.getHostAddress()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val serviceList: List<Service?>? = _serviceDescription?.serviceList

            if (serviceList != null) {
                for (i in serviceList.indices) {
                    val eventSubURL: String =
                        makeControlURL("/", serviceList[i]!!.eventSubURL) ?: continue

                    try {
                        val connection: HttpConnection = HttpConnection.newSubscriptionInstance(
                            URLBuilder(
                                protocol = URLProtocol.Companion.HTTP,
                                host = _serviceDescription!!.ipAddress,
                                port = _serviceDescription!!.port,
                                pathSegments = listOf(eventSubURL),
                            ).build()
                        )
                        connection.setMethod(HttpConnection.Method.SUBSCRIBE)
                        connection.addHeader(
                            "CALLBACK",
                            "<http://" + myIpAddress + ":" + httpServer.port + eventSubURL + ">"
                        )
                        connection.addHeader("NT", "upnp:event")
                        connection.addHeader("TIMEOUT", "Second-$TIMEOUT")
                        connection.addHeader("Connection", "close")
                        connection.addHeader("Content-length", "0")
                        connection.addHeader("USER-AGENT", "Android UPnp/1.1 ConnectSDK")
                        connection.execute()
                        if (connection.getResponseCode() == 200) {
                            SIDList[serviceList[i]!!.serviceType] =
                                connection.getResponseHeader("SID")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        resubscribeServices()
    }

    fun resubscribeServices() {
        resubscriptionTimer = scope.launch {
            withContext(Dispatchers.IO) {
                while (true) {
                    delay((TIMEOUT / 2 * 1000).toLong())
                    val serviceList: List<Service>? = _serviceDescription?.serviceList

                    if (serviceList != null) {
                        for (i in serviceList.indices) {
                            val eventSubURL: String =
                                makeControlURL("/", serviceList[i].eventSubURL) ?: continue

                            val SID: String? = SIDList[serviceList[i].serviceType]
                            try {
                                val connection: HttpConnection =
                                    HttpConnection.newSubscriptionInstance(
                                        URLBuilder(
                                            protocol = URLProtocol.HTTP,
                                            host = _serviceDescription!!.ipAddress,
                                            port = _serviceDescription!!.port,
                                            pathSegments = listOf(eventSubURL)
                                        ).build()
                                    )
                                connection.setMethod(HttpConnection.Method.SUBSCRIBE)
                                connection.addHeader("TIMEOUT", "Second-$TIMEOUT")
                                connection.addHeader("SID", SID ?: "")
                                connection.execute()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun unsubscribeServices() {
        if (resubscriptionTimer != null) {
            resubscriptionTimer!!.cancel()
        }

        Util.runInBackground {

            val serviceList: List<Service>? = _serviceDescription?.serviceList

            if (serviceList != null) {
                for (i in serviceList.indices) {
                    val eventSubURL: String =
                        makeControlURL("/", serviceList[i].eventSubURL) ?: continue

                    val sid: String? = SIDList[serviceList[i].serviceType]
                    try {
                        val connection: HttpConnection = HttpConnection.newSubscriptionInstance(
                            URLBuilder(
                                protocol = URLProtocol.HTTP,
                                host = _serviceDescription!!.ipAddress,
                                port = _serviceDescription!!.port,
                                pathSegments = listOf(eventSubURL),
                            ).build()
                        )
                        connection.setMethod(HttpConnection.Method.UNSUBSCRIBE)
                        connection.addHeader("SID", sid ?: "")
                        connection.execute()
                        if (connection.getResponseCode() == 200) {
                            SIDList.remove(serviceList[i].serviceType)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun getVolumeControl(): VolumeControl {
        return this
    }

    override fun getVolumeControlCapabilityLevel(): CapabilityPriorityLevel {
        return CapabilityPriorityLevel.NORMAL
    }

    override suspend fun volumeUp(listener: ResponseListener<Any?>) {
        getVolume(object : VolumeListener {
            override suspend fun onSuccess(volume: Float) {
                if (volume >= 1.0) {
                    Util.postSuccess(listener, null)
                } else {
                    var newVolume: Float = (volume + 0.01).toFloat()

                    if (newVolume > 1.0) newVolume = 1.0.toFloat()

                    setVolume(newVolume, listener)

                    Util.postSuccess(listener, null)
                }
            }

            override fun onError(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        })
    }

    override suspend fun volumeDown(listener: ResponseListener<Any?>) {
        getVolume(object : VolumeListener {
            override suspend fun onSuccess(volume: Float) {
                if (volume <= 0.0) {
                    Util.postSuccess(listener, null)
                } else {
                    var newVolume: Float = (volume - 0.01).toFloat()

                    if (newVolume < 0.0) newVolume = 0.0.toFloat()

                    setVolume(newVolume, listener)

                    Util.postSuccess(listener, null)
                }
            }

            override fun onError(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        })
    }

    override suspend fun setVolume(volume: Float, listener: ResponseListener<Any?>) {
        val method: String = "SetVolume"
        val instanceId: String = "0"
        val channel: String = "Master"
        val value: String = ((volume * 100).toInt()).toString()

        val params: MutableMap<String, String> = LinkedHashMap()
        params["Channel"] = channel
        params["DesiredVolume"] = value

        val payload: String? = getMessageXml(RENDERING_CONTROL_URN, method, instanceId, params)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, listener)
        request.send()
    }

    override suspend fun getVolume(listener: VolumeListener) {
        val method: String = "GetVolume"
        val instanceId: String = "0"
        val channel: String = "Master"

        val params: MutableMap<String, String> = LinkedHashMap()
        params["Channel"] = channel

        val payload: String? = getMessageXml(RENDERING_CONTROL_URN, method, instanceId, params)

        val responseListener: ResponseListener<Any?> = object : ResponseListener<Any?> {
            override suspend fun onSuccess(response: Any?) {
                val currentVolume: String = parseData(response as String, "CurrentVolume")
                val iVolume: Int = 0
                try {
                    currentVolume.toInt()
                } catch (ex: RuntimeException) {
                    ex.printStackTrace()
                }
                val fVolume: Float = (iVolume / 100.0).toFloat()

                Util.postSuccess(listener, fVolume)
            }

            override fun onError(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        }

        val request: ServiceCommand<VolumeListener> =
            ServiceCommand(this, method, payload, responseListener)
        request.send()
    }

    override suspend fun setMute(isMute: Boolean, listener: ResponseListener<Any?>) {
        val method: String = "SetMute"
        val instanceId: String = "0"
        val channel: String = "Master"
        val muteStatus: Int = if ((isMute)) 1 else 0

        val params: MutableMap<String, String> = LinkedHashMap()
        params["Channel"] = channel
        params["DesiredMute"] = muteStatus.toString()

        val payload: String? = getMessageXml(RENDERING_CONTROL_URN, method, instanceId, params)

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, listener)
        request.send()
    }

    override suspend fun getMute(listener: MuteListener?) {
        val method: String = "GetMute"
        val instanceId: String = "0"
        val channel: String = "Master"

        val params: MutableMap<String, String> = LinkedHashMap()
        params["Channel"] = channel

        val payload: String? = getMessageXml(RENDERING_CONTROL_URN, method, instanceId, params)

        val responseListener: ResponseListener<Any?> = object : ResponseListener<Any?> {
            override suspend fun onSuccess(response: Any?) {
                val currentMute: String = parseData(response as String, "CurrentMute")
                val isMute: Boolean = currentMute.toBoolean()

                Util.postSuccess(listener, isMute)
            }

            override fun onError(error: ServiceCommandError) {
                Util.postError(listener, error)
            }
        }

        val request: ServiceCommand<ResponseListener<Any>> =
            ServiceCommand(this, method, payload, responseListener)
        request.send()
    }

    override suspend fun subscribeVolume(listener: VolumeListener): ServiceSubscription<VolumeListener> {
        val request: URLServiceSubscription<VolumeListener> =
            URLServiceSubscription(this, "volume", null, null)
        request.addListener(listener)
        addSubscription(request)
        return request
    }

    override suspend fun subscribeMute(listener: MuteListener): ServiceSubscription<MuteListener> {
        val request: URLServiceSubscription<MuteListener> =
            URLServiceSubscription(this, "mute", null, null)
        request.addListener(listener)
        addSubscription(request)
        return request
    }

    companion object {
        val ID: String = "DLNA"

        protected val SUBSCRIBE: String = "SUBSCRIBE"
        protected val UNSUBSCRIBE: String = "UNSUBSCRIBE"

        val AV_TRANSPORT_URN: String = "urn:schemas-upnp-org:service:AVTransport:1"
        val CONNECTION_MANAGER_URN: String = "urn:schemas-upnp-org:service:ConnectionManager:1"
        val RENDERING_CONTROL_URN: String = "urn:schemas-upnp-org:service:RenderingControl:1"

        protected val AV_TRANSPORT: String = "AVTransport"
        protected val CONNECTION_MANAGER: String = "ConnectionManager"
        protected val RENDERING_CONTROL: String = "RenderingControl"
        protected val GROUP_RENDERING_CONTROL: String = "GroupRenderingControl"

        val PLAY_STATE: String = "playState"
        val DEFAULT_SUBTITLE_MIMETYPE: String = "text/srt"
        val DEFAULT_SUBTITLE_TYPE: String = "srt"

        private val TIMEOUT: Int = 300

        fun discoveryFilter(): DiscoveryFilter {
            return DiscoveryFilter(ID, "urn:schemas-upnp-org:device:MediaRenderer:1")
        }

        fun getServiceProvider() =
            DeviceServiceProvider(kClass = DLNAService::class, constructor = { serviceDescription, serviceConfig ->
                DLNAService(serviceDescription, serviceConfig)
            }, discoverFilter = {
                discoveryFilter()
            })
    }
}
