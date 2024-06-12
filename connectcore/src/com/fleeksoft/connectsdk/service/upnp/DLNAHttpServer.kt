package com.fleeksoft.connectsdk.service.upnp

import co.touchlab.stately.collections.ConcurrentMutableList
import com.fleeksoft.connectsdk.core.MediaInfo
import com.fleeksoft.connectsdk.core.Util
import com.fleeksoft.connectsdk.ported.getString
import com.fleeksoft.connectsdk.service.capability.MediaControl.PlayStateStatus
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.URLServiceSubscription
import io.ktor.network.sockets.*
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.RSocketRequestHandler
import io.rsocket.kotlin.core.RSocketServer
import io.rsocket.kotlin.payload.Payload
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import io.rsocket.kotlin.transport.ktor.tcp.TcpServer
import io.rsocket.kotlin.transport.ktor.tcp.TcpServerTransport
import korlibs.io.stream.AsyncStream
import korlibs.io.stream.CharReader
import korlibs.io.stream.openAsync
import korlibs.io.util.CharReaderStrReader
import korlibs.io.util.StrReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.concurrent.Volatile
import kotlin.jvm.Synchronized

class DLNAHttpServer {
    val port: Int = 49291

    @Volatile
    var welcomeSocket: TcpServer? = null

    @Volatile
    var isRunning: Boolean = false

    var subscriptions: ConcurrentMutableList<URLServiceSubscription<*>>

    init {
        subscriptions = ConcurrentMutableList()
    }

    val scope = CoroutineScope(Dispatchers.Default)

    fun start() {
        if (isRunning) {
            return
        }

        isRunning = true


        scope.launch {
            try {
                val transport = TcpServerTransport("0.0.0.0", port)
                val connector = RSocketServer {
                    //configuration goes here
                }
                welcomeSocket = connector.bindIn(scope, transport) {
                    RSocketRequestHandler {
                        //handler for request/response
                        requestResponse { request: Payload ->
                            println(request.data.readText()) //print request payload data
                            delay(500) // work emulation
                            processRequests(request)
                        }
                    }
                }
                welcomeSocket?.handlerJob?.join()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun stop() {
        if (!isRunning) {
            return
        }

        for (sub in subscriptions) {
            sub.unsubscribe()
        }
        subscriptions.clear()

        if (welcomeSocket != null && !welcomeSocket!!.handlerJob.isCancelled) {
            try {
                welcomeSocket!!.serverSocket.cancel()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        welcomeSocket = null
        isRunning = false
    }

    private suspend fun RSocket.processRequests(request: Payload): Payload {

        var body: String = request.data.readText()
        val inFromClient = StrReader(body)
        try {
            var sb = StringBuilder()

            while (inFromClient.available > 0) {
                sb.append(inFromClient.readChar())

                if (sb.toString().endsWith("\r\n\r\n")) break
            }
            sb = StringBuilder()

            while (inFromClient.available > 0) {
                sb.append(inFromClient.readChar())
                body = sb.toString()

                if (body.endsWith("</e:propertyset>")) break
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        val propertySet: JsonArray
        val parser = DLNANotifyParser()

        try {
            propertySet = parser.parse(body.openAsync())

            for (i in 0 until propertySet.size) {
                val property = propertySet[i].jsonObject

                if (property.containsKey("LastChange")) {
                    val lastChange = property["LastChange"]?.jsonObject
                    lastChange?.let { handleLastChange(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return buildPayload {
            data(buildString {
                appendLine("HTTP/1.1 200 OK")
                appendLine("Connection: Close")
                appendLine("Content-Length: 0")
                appendLine()
            })
        }
    }

    private suspend fun handleLastChange(lastChange: JsonObject) {
        if (lastChange.containsKey("InstanceID")) {
            val instanceIDs = lastChange["InstanceID"]?.jsonArray ?: return

            for (i in 0 until instanceIDs.size) {
                val events = instanceIDs[i].jsonArray

                for (j in 0 until events.size) {
                    val entry = events[j].jsonObject
                    handleEntry(entry)
                }
            }
        }
    }

    private suspend fun handleEntry(entry: JsonObject) {
        if (entry.containsKey("TransportState")) {
            val transportState = entry["TransportState"]?.jsonPrimitive?.contentOrNull ?: return
            val status: PlayStateStatus =
                PlayStateStatus.convertTransportStateToPlayStateStatus(transportState)

            for (sub in subscriptions) {
                if (sub.target.equals("playState", ignoreCase = true)) {
                    val listeners = sub.getListeners()
                    for (j in listeners.indices) {
                        val listener = listeners[j] as ResponseListener<PlayStateStatus>
                        Util.postSuccess(listener, status)
                    }
                }
            }
        }

        if ((entry.containsKey("Volume") && !entry.containsKey("channel")) ||
            (entry.containsKey("Volume") &&
                entry["channel"]?.jsonPrimitive?.contentOrNull == "Master")
        ) {
            val intVolume = entry["Volume"]!!.jsonPrimitive.int
            val volume: Float = intVolume.toFloat() / 100

            for (sub in subscriptions) {
                if (sub.target.equals("volume", ignoreCase = true)) {
                    val listeners = sub.getListeners()
                    for (j in listeners.indices) {
                        val listener = listeners[j] as ResponseListener<Float>
                        Util.postSuccess(listener, volume)
                    }
                }
            }
        }

        if ((entry.containsKey("Mute") && !entry.containsKey("channel"))
            || (entry.containsKey("Mute")
                && entry["channel"]?.jsonPrimitive?.contentOrNull == "Master")
        ) {
            val muteStatus = entry["Mute"]!!.jsonPrimitive.content

            val mute: Boolean = try {
                muteStatus.toInt() == 1
            } catch (e: NumberFormatException) {
                muteStatus.toBoolean()
            }

            for (sub in subscriptions) {
                if (sub.target.equals("mute", ignoreCase = true)) {
                    for (j in sub.getListeners().indices) {
                        val listener = sub.getListeners()[j] as ResponseListener<Boolean>
                        Util.postSuccess(listener, mute)
                    }
                }
            }
        }

        if (entry.containsKey("CurrentTrackMetaData")) {
            val trackMetaData = entry.getString("CurrentTrackMetaData")

            val info: MediaInfo = DLNAMediaInfoParser.getMediaInfo(trackMetaData)

            for (sub in subscriptions) {
                if (sub.target.equals("info", ignoreCase = true)) {
                    for (j in sub.getListeners().indices) {
                        val listener = sub.getListeners()[j] as ResponseListener<MediaInfo>
                        Util.postSuccess(listener, info)
                    }
                }
            }
        }
    }

    fun getSubscriptions(): MutableList<URLServiceSubscription<*>> {
        return subscriptions
    }

    fun setSubscriptions(subscriptions: List<URLServiceSubscription<*>>) {
        this.subscriptions = ConcurrentMutableList()
        this.subscriptions.addAll(subscriptions)
    }
}
