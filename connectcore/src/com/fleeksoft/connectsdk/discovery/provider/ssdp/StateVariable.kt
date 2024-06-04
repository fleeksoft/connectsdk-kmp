/*
 * StateVariable
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

class StateVariable() {
    /* Optional. Defines whether event messages will be generated when the value
     * of this state variable changes. Defaut value is "yes".
     */
    var mSendEvents: String = "yes"

    /* Optional. Defines whether event messages will be delivered using 
     * multicast eventing. Default value is "no".
     */
    var mMulticast: String = "no"

    /* Required. Name of state variable. */
    var mName: String? = null

    /* Required. Same as data types defined by XML Schema. */
    var mDataType: String? = null

    companion object {
        val TAG: String = "stateVariable"
        val TAG_NAME: String = "name"
        val TAG_DATA_TYPE: String = "dataType"
    }
}