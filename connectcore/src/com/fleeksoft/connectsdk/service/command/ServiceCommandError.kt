/*
 * ServiceCommandError
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
package com.fleeksoft.connectsdk.service.command

/**
 * This class implements base service error which is based on HTTP response codes
 */
open class ServiceCommandError : Error {
    var code: Int = 0
        protected set

    var payload: Any? = null
        protected set

    constructor()

    constructor(detailMessage: String?) : super(detailMessage)

    constructor(code: Int, detailMessage: String?) : super(detailMessage) {
        this.code = code
    }

    constructor(code: Int, desc: String?, payload: Any?) : super(desc) {
        this.code = code
        this.payload = payload
    }

    companion object {
        private const val serialVersionUID = 4232138682873631468L

        /**
         * Create an error which indicates that feature is not supported by a service
         * @return NotSupportedServiceCommandError
         */
        fun notSupported(): ServiceCommandError {
            return NotSupportedServiceCommandError()
        }

        /**
         * Create an error from HTTP response code
         * @param code HTTP response code
         * @return ServiceCommandError
         */
        fun getError(code: Int): ServiceCommandError {
            var desc: String? = null
            desc = if (code == 400) {
                "Bad Request"
            } else if (code == 401) {
                "Unauthorized"
            } else if (code == 500) {
                "Internal Server Error"
            } else if (code == 503) {
                "Service Unavailable"
            } else {
                "Unknown Error"
            }

            return ServiceCommandError(code, desc, null)
        }
    }
}
