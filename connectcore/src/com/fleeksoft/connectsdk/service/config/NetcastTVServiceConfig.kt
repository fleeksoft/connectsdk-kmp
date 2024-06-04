/*
 * NetcastTVServiceConfig
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

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class NetcastTVServiceConfig : ServiceConfig {
    var _pairingKey: String? = null

    constructor(serviceUUID: String?) : super(serviceUUID!!)

    constructor(serviceUUID: String, pairingKey: String?) : super(serviceUUID) {
        this._pairingKey = pairingKey
    }

    constructor(json: JsonObject) : super(json) {
        _pairingKey = json.get(KEY_PAIRING)?.jsonPrimitive?.contentOrNull
    }

    fun getPairingKey(): String? {
        return _pairingKey
    }

    suspend fun setPairingKey(pairingKey: String?) {
        this._pairingKey = pairingKey
        notifyUpdate()
    }

    override fun toJsonObject(): JsonObject {
        val jsonObj = super.toJsonObject().toMutableMap()

        jsonObj[KEY_PAIRING] = JsonPrimitive(_pairingKey)

        return JsonObject(jsonObj)
    }

    companion object {
        const val KEY_PAIRING: String = "pairingKey"
    }
}
