/*
 * Argument
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Copyright (c) 2011 stonker.lee@gmail.com https://code.google.com/p/android-dlna/
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
package com.fleeksoft.connectsdk.discovery.provider.ssdp

class Argument() {
    /* Required. Name of formal parameter. */
    var mName: String? = null

    /* Required. Defines whether argument is an input or output paramter. */
    var mDirection: String? = null

    /* Optional. Identifies at most one output argument as the return value. */
    var mRetval: String? = null

    /* Required. Must be the same of a state variable. */
    var mRelatedStateVariable: String? = null

    companion object {
        val TAG: String = "argument"
        val TAG_NAME: String = "name"
        val TAG_DIRECTION: String = "direction"
        val TAG_RETVAL: String = "retval"
        val TAG_RELATED_STATE_VARIABLE: String = "relatedStateVariable"
    }
}