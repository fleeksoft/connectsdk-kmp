/*
 * SSDPDeviceDescriptionParser
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 6 Jan 2015
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

import korlibs.io.serialization.xml.Xml
import korlibs.io.serialization.xml.isNode
import korlibs.util.format

class SSDPDeviceDescriptionParser(var device: SSDPDevice) {
    var currentIcon: Icon? = null
    var currentService: Service? = null

    var data: MutableMap<String, String?> = HashMap()

    fun parse(xml: Xml) {
        xml.allChildren.forEach { child ->
            val currentValue = child.text
            val qName = child.name
            if ((Icon.TAG == qName)) {
                currentIcon = Icon()
            } else if ((Service.TAG == qName)) {
                currentService = Service()
                currentService!!.baseURL = device.baseURL
            } else if ((TAG_SEC_CAPABILITY == qName)) {      // Samsung MultiScreen Capability
                var port: String? = null
                var location: String? = null

                for (i in 0 until xml.attributes.size) {
                    if (xml.attributes.containsKey(TAG_PORT)) {
                        port = xml.attributes.getValue(TAG_PORT)
                    } else if (xml.attributes.containsKey(TAG_LOCATION)) {
                        location = xml.attributes.getValue(TAG_LOCATION)
                    }
                }

                if (port == null) {
                    device.serviceURI = "%s%s".format(device.serviceURI, location ?: "")
                } else {
                    device.serviceURI = "%s:%s%s".format(device.serviceURI, port, location ?: "")
                }
            } else if ((TAG_DEVICE_TYPE == qName)) {
                device.deviceType = currentValue
            } else if ((TAG_FRIENDLY_NAME == qName)) {
                device.friendlyName = currentValue
            } else if ((TAG_MANUFACTURER == qName)) {
                device.manufacturer = currentValue
                //        } else if (TAG_MANUFACTURER_URL.equals(qName)) {
//            device.manufacturerURL = currentValue;
            } else if ((TAG_MODEL_DESCRIPTION == qName)) {
                device.modelDescription = currentValue
            } else if ((TAG_MODEL_NAME == qName)) {
                device.modelName = currentValue
            } else if ((TAG_MODEL_NUMBER == qName)) {
                device.modelNumber = currentValue
                //        } else if (TAG_MODEL_URL.equals(qName)) {
//            device.modelURL = currentValue;
//        } else if (TAG_SERIAL_NUMBER.equals(qName)) {
//            device.serialNumber = currentValue;
            } else if ((TAG_UDN == qName)) {
                device.UDN = currentValue
                //        } else if (TAG_UPC.equals(qName)) {
//            device.UPC = currentValue;
            } else if ((Service.TAG_SERVICE_TYPE == qName)) {
                currentService!!.serviceType = currentValue
            } else if ((Service.TAG_SERVICE_ID == qName)) {
                currentService!!.serviceId = currentValue
            } else if ((Service.TAG_SCPD_URL == qName)) {
                currentService!!.SCPDURL = currentValue
            } else if ((Service.TAG_CONTROL_URL == qName)) {
                currentService!!.controlURL = currentValue
            } else if ((Service.TAG_EVENTSUB_URL == qName)) {
                currentService!!.eventSubURL = currentValue
            } else if ((Service.TAG == qName)) {
                currentService?.let { device.serviceList.add(it) }
            }

            data[qName] = currentValue

            if (child.allNodeChildren.isNotEmpty()) {
                parse(child)
            }
        }
    }

    companion object {
        val TAG_DEVICE_TYPE: String = "deviceType"
        val TAG_FRIENDLY_NAME: String = "friendlyName"
        val TAG_MANUFACTURER: String = "manufacturer"
        val TAG_MANUFACTURER_URL: String = "manufacturerURL"
        val TAG_MODEL_DESCRIPTION: String = "modelDescription"
        val TAG_MODEL_NAME: String = "modelName"
        val TAG_MODEL_NUMBER: String = "modelNumber"
        val TAG_MODEL_URL: String = "modelURL"
        val TAG_SERIAL_NUMBER: String = "serialNumber"
        val TAG_UDN: String = "UDN"
        val TAG_UPC: String = "UPC"
        val TAG_ICON_LIST: String = "iconList"
        val TAG_SERVICE_LIST: String = "serviceList"

        val TAG_SEC_CAPABILITY: String = "sec:Capability"
        val TAG_PORT: String = "port"
        val TAG_LOCATION: String = "location"
    }
}
