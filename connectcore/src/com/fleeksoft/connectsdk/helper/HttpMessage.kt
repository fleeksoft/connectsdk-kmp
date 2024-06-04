package com.fleeksoft.connectsdk.etc.helper

import com.fleeksoft.connectsdk.helper.NetworkHelper
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.contentType
import net.thauvin.erik.urlencoder.UrlEncoderUtil

object HttpMessage {
    const val CONTENT_TYPE_HEADER: String = "Content-Type"
    const val CONTENT_TYPE_TEXT_XML: String = "text/xml; charset=utf-8"
    const val CONTENT_TYPE_APPLICATION_PLIST: String = "application/x-apple-binary-plist"
    const val UDAP_USER_AGENT: String = "UDAP/2.0"
    const val LG_ELECTRONICS: String = "LG Electronics"
    const val USER_AGENT: String = "User-Agent"
    const val SOAP_ACTION: String = "\"urn:schemas-upnp-org:service:AVTransport:1#%s\""
    const val SOAP_HEADER: String = "Soapaction"
    const val NEW_LINE: String = "\r\n"

    suspend fun getHttpPost(
        url: String,
        reqHeaders: Map<String, String> = emptyMap(),
    ): HttpStatement {
        return NetworkHelper.instance.client.preparePost(urlString = url) {
            contentType(ContentType.Text.Xml)
            if (reqHeaders.isNotEmpty()) {
                reqHeaders.forEach {
                    headers.append(it.key, it.value)
                }
            }
        }
    }

    suspend fun getUDAPHttpPost(url: String): HttpStatement {
        return getHttpPost(url, reqHeaders = mapOf(USER_AGENT to UDAP_USER_AGENT))
    }

    suspend fun getDLNAHttpPost(url: String, action: String): HttpStatement {
        val soapAction = "\"urn:schemas-upnp-org:service:AVTransport:1#$action\""
        return getHttpPost(url, reqHeaders = mapOf(SOAP_HEADER to soapAction))
    }

    suspend fun getDLNAHttpPostRenderControl(url: String, action: String): HttpStatement {
        val soapAction = "\"urn:schemas-upnp-org:service:RenderingControl:1#$action\""

        return getHttpPost(url, reqHeaders = mapOf(SOAP_HEADER to soapAction))
    }

    suspend fun getHttpGet(
        url: String,
        reqHeaders: Map<String, String> = emptyMap(),
    ): HttpStatement {
        return NetworkHelper.instance.client.prepareGet(urlString = url) {
            if (reqHeaders.isNotEmpty()) {
                reqHeaders.forEach {
                    headers.append(it.key, it.value)
                }
            }
        }
    }

    suspend fun getUDAPHttpGet(uri: String): HttpStatement {
        return getHttpGet(uri, reqHeaders = mapOf(USER_AGENT to UDAP_USER_AGENT))
    }

    fun encode(str: String): String? {
        try {
            return UrlEncoderUtil.encode(str)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun decode(str: String): String? {
        try {
            return UrlEncoderUtil.decode(str)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
