/*
 * ServiceConfig
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
package com.fleeksoft.connectsdk.service.config

import com.fleeksoft.connectsdk.core.Util
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

open class ServiceConfig {
    var _serviceUUID: String

    private var lastDetected: Long = Long.MAX_VALUE

    var connected: Boolean = false
    var wasConnected: Boolean = false

    var listener: ServiceConfigListener? = null

    constructor(serviceUUID: String) {
        this._serviceUUID = serviceUUID
    }

    constructor(desc: ServiceDescription) {
        this._serviceUUID = desc.uuid
        this.connected = false
        this.wasConnected = false
        this.lastDetected = Util.getTime()
    }

    constructor(config: ServiceConfig) {
        this._serviceUUID = config._serviceUUID
        this.connected = config.connected
        this.wasConnected = config.wasConnected
        this.lastDetected = config.lastDetected

        this.listener = config.listener
    }

    constructor(json: JsonObject) {
        _serviceUUID = json.getValue(KEY_UUID).jsonPrimitive.content
        lastDetected = json.getValue(KEY_LAST_DETECT).jsonPrimitive.long
    }

    fun getServiceUUID(): String {
        return _serviceUUID
    }

    suspend fun setServiceUUID(serviceUUID: String) {
        this._serviceUUID = serviceUUID
        notifyUpdate()
    }

    override fun toString(): String {
        return _serviceUUID
    }

    fun getLastDetected(): Long {
        return lastDetected
    }

    suspend fun setLastDetected(value: Long) {
        lastDetected = value
        notifyUpdate()
    }

    suspend fun detect() {
        setLastDetected(Util.getTime())
    }

    open fun toJsonObject(): JsonObject {
        val jsonObj = JsonObject(
            mapOf(
                KEY_CLASS to JsonPrimitive(this@ServiceConfig::class.simpleName),
                KEY_LAST_DETECT to JsonPrimitive(lastDetected),
                KEY_UUID to JsonPrimitive(_serviceUUID)
            )
        )

        return jsonObj
    }

    protected suspend fun notifyUpdate() {
        if (listener != null) {
            listener!!.onServiceConfigUpdate(this)
        }
    }

    interface ServiceConfigListener {
        suspend fun onServiceConfigUpdate(serviceConfig: ServiceConfig)
    }

    companion object {
        const val KEY_CLASS: String = "class"
        const val KEY_LAST_DETECT: String = "lastDetection"
        const val KEY_UUID: String = "UUID"
        fun getConfig(json: JsonObject): ServiceConfig {
            // TODO: test it
            return ServiceConfig(json)
        }
    }
}
