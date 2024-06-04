/*
 * LaunchSession
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

import com.fleeksoft.connectsdk.core.JSONDeserializable
import com.fleeksoft.connectsdk.core.JSONSerializable
import com.fleeksoft.connectsdk.ported.optString
import com.fleeksoft.connectsdk.service.DeviceService
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Any time anything is launched onto a first screen device, there will be important session information that needs to be tracked. LaunchSession will track this data, and must be retained to perform certain actions within the session.
 */
open class LaunchSession : JSONSerializable, JSONDeserializable {
    // @cond INTERNAL
    protected var _appId: String? = null
    /** User-friendly name of the app (ex. YouTube, Browser, Hulu)  */
    /**
     * Sets the user-friendly name of the app (ex. YouTube, Browser, Hulu)
     *
     * @param appName Name of the app
     */
    var appName: String? = null
    /** Unique ID for the session (only provided by certain protocols)  */
    /**
     * Sets the session id (only provided by certain protocols)
     *
     * @param sessionId Id of the current session
     */
    var sessionId: String? = null
    /** Raw data from the first screen device about the session. In most cases, this is a JsonObject.  */
    /**
     * Sets the raw data from the first screen device about the session. In most cases, this is a JsonObject.
     *
     * @param rawData Sets the raw data
     */
    var rawData: JsonElement? = null

    /** DeviceService responsible for launching the session.  */
    /**
     * DeviceService responsible for launching the session.
     *
     * @param service Sets the DeviceService
     */
    var service: DeviceService? = null
    /**
     * When closing a LaunchSession, the DeviceService relies on the sessionType to determine the method of closing the session.
     */
    /**
     * Sets the LaunchSessionType of this LaunchSession.
     *
     * @param sessionType The type of LaunchSession
     */
    var sessionType: LaunchSessionType? = null

    // @endcond
    /**
     * LaunchSession type is used to help DeviceService's know how to close a LunchSession.
     */
    enum class LaunchSessionType {
        /** Unknown LaunchSession type, may be unable to close this launch session  */
        Unknown,

        /** LaunchSession represents a launched app  */
        App,

        /** LaunchSession represents an external input picker that was launched  */
        ExternalInputPicker,

        /** LaunchSession represents a media app  */
        Media,

        /** LaunchSession represents a web app  */
        WebApp
    }

    // @endcond
    /** System-specific, unique ID of the app (ex. youtube.leanback.v4, 0000134, hulu)  */
    fun getAppId(): String? {
        return _appId
    }

    /**
     * Close the app/media associated with the session.
     * @param listener
     */
    open suspend fun close(listener: ResponseListener<Any?>) {
        service!!.closeLaunchSession(this, listener)
    }

    override fun toJsonObject(): JsonObject {
        val objMap: MutableMap<String, JsonElement> = mutableMapOf(
            "appId" to JsonPrimitive(_appId),
            "sessionId" to JsonPrimitive(sessionId),
            "name" to JsonPrimitive(appName),
            "sessionType" to JsonPrimitive(sessionType!!.name),
        )





        if (service != null) objMap["serviceName"] = JsonPrimitive(service!!.serviceName)

        if (rawData != null) {
            objMap["rawData"] = rawData!!
        }

        return JsonObject(objMap)
    }

    override fun fromJsonObject(obj: JsonObject) {
        this._appId = obj.optString("appId")
        this.sessionId = obj.optString("sessionId")
        this.appName = obj.optString("name")
        this.sessionType = LaunchSessionType.valueOf(obj.optString("sessionType"))
        this.rawData = obj["rawData"]
    }

    // @endcond
    /**
     * Compares two LaunchSession objects.
     *
     * @param other LaunchSession object to compare.
     *
     * @return true if both LaunchSession id and sessionId values are equal
     */
    override fun equals(other: Any?): Boolean {
        // TODO Auto-generated method stub
        return super.equals(other)
    }

    companion object {
        /**
         * Instantiates a LaunchSession object for a given app ID.
         *
         * @param appId System-specific, unique ID of the app
         */
        fun launchSessionForAppId(appId: String?): LaunchSession {
            val launchSession = LaunchSession()
            launchSession._appId = appId

            return launchSession
        }

        // @cond INTERNAL
        fun launchSessionFromJsonObject(json: JsonObject): LaunchSession {
            val launchSession = LaunchSession()
            try {
                launchSession.fromJsonObject(json)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return launchSession
        }
    }
}
