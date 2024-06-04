/*
 * SSDPDevice
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

import com.fleeksoft.connectsdk.helper.NetworkHelper
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import korlibs.io.serialization.xml.Xml
import korlibs.io.serialization.xml.isText
import korlibs.util.format

data class SSDPDevice private constructor(
    /* Required. UPnP device type. */
    var deviceType: String? = null,

    /* Required. Short description for end user. */
    var friendlyName: String? = null,

    /* Required. Manufacturer's name. */
    var manufacturer: String? = null,

    //    /* Optional. Web site for manufacturer. */
    //    public String manufacturerURL;
    /* Recommended. Long description for end user. */
    var modelDescription: String? = null,

    /* Required. Model name. */
    var modelName: String? = null,

    /* Recommended. Model number. */
    var modelNumber: String? = null,

    //    /* Optional. Web site for model. */
    //    public String modelURL;
    //    /* Recommended. Serial number. */
    //    public String serialNumber;
    /* Required. Unique Device Name. */
    var UDN: String? = null,

    //    /* Optional. Universal Product Code. */
    //    public String UPC;
    /* Required. */ //    List<Icon> iconList = new ArrayList<Icon>();
    var locationXML: String? = null,

    /* Optional. */
    var serviceList: MutableList<Service> = ArrayList(),

    var ST: String? = null,
    var applicationURL: String? = null,

    var serviceURI: String,

    var baseURL: String? = null,
    var ipAddress: String,
    var port: Int,
    var UUID: String?,

    var headers: Map<String, List<String>>? = null,
) {

    override fun toString(): String {
        return (friendlyName)!!
    }

    companion object {
        suspend fun parse(url: String, ST: String?): SSDPDevice {
            return parse(Url(url), ST)
        }

        suspend fun parse(url: Url, ST: String?): SSDPDevice {
            val baseURL = if (url.port == -1) {
                "%s://%s".format(url.protocol, url.host)
            } else {
                "%s://%s:%d".format(
                    url.protocol.name,
                    url.host,
                    url.port
                )
            }


            val urlResponse = NetworkHelper.instance.get(url.toString())

            var applicationURL = urlResponse.headers["Application-URL"]
            if (!applicationURL.isNullOrEmpty() && applicationURL.substring(applicationURL.length - 1) != "/") {
                applicationURL = "$applicationURL/"
            }

            val bodyText = urlResponse.bodyAsText()

            val headers = urlResponse.headers.toMap()

            val ssdpDevice = SSDPDevice(
                baseURL = baseURL,
                ipAddress = url.host,
                port = url.port,
                UUID = null,
                serviceURI = "%s://%s".format(url.protocol.name, url.host),
                applicationURL = applicationURL,
                locationXML = bodyText,
                headers = headers
            )

            val xml = Xml.parse(bodyText)
            if (xml.isText) {
                throw IllegalArgumentException("Invalid xml $bodyText")
            }
            SSDPDeviceDescriptionParser(ssdpDevice).parse(xml)
            return ssdpDevice
        }
    }
}
