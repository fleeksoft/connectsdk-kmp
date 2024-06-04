/*
 * Service
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

//import com.fleeksoft.connectsdk.core.upnp.parser.Parser;
class Service {
    var baseURL: String? = null

    /* Required. UPnP service type. */
    var serviceType: String? = null

    /* Required. Service identifier. */
    var serviceId: String? = null

    /* Required. Relative URL for service description. */
    var SCPDURL: String? = null

    /* Required. Relative URL for control. */
    var controlURL: String? = null

    /* Relative. Relative URL for eventing. */
    var eventSubURL: String? = null

    var actionList: List<Action>? = null
    var serviceStateTable: List<StateVariable>? = null

    /*
     * We don't get SCPD, control and eventSub descriptions at service creation.
     * So call this method first before you use the service.
     */
    fun init() {
//        Parser parser = Parser.getInstance();
    }

    companion object {
        val TAG: String = "service"
        val TAG_SERVICE_TYPE: String = "serviceType"
        val TAG_SERVICE_ID: String = "serviceId"
        val TAG_SCPD_URL: String = "SCPDURL"
        val TAG_CONTROL_URL: String = "controlURL"
        val TAG_EVENTSUB_URL: String = "eventSubURL"
    }
}