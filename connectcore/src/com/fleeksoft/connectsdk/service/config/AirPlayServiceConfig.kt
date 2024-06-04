/*
 * AirPlayServiceConfig
 * Connect SDK
 * 
 * Copyright (c) 2020 LG Electronics.
 * Created by Seokhee Lee on 28 Aug 2020
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
import kotlinx.serialization.json.jsonPrimitive

class AirPlayServiceConfig(json: JsonObject) : ServiceConfig(json) {
    private var authToken: String

    init {
        authToken = json.getValue(KEY_AUTH_TOKEN).jsonPrimitive.content
    }

    fun getAuthToken(): String {
        return authToken
    }

    suspend fun setAuthToken(authToken: String) {
        this.authToken = authToken
        notifyUpdate()
    }

    override fun toJsonObject(): JsonObject {
        val jsonObj = super.toJsonObject().toMutableMap()

        jsonObj[KEY_AUTH_TOKEN] = JsonPrimitive(authToken)

        return JsonObject(jsonObj)
    }

    companion object {
        const val KEY_AUTH_TOKEN: String = "authToken"
    }
}
