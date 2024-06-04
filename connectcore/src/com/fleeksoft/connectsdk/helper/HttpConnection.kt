package com.fleeksoft.connectsdk.etc.helper

import com.fleeksoft.connectsdk.helper.NetworkHelper
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.content.ByteArrayContent
import io.ktor.utils.io.core.readUTF8Line
import io.ktor.utils.io.core.toByteArray
import io.rsocket.kotlin.RSocket
import io.rsocket.kotlin.core.RSocketConnector
import io.rsocket.kotlin.payload.buildPayload
import io.rsocket.kotlin.payload.data
import io.rsocket.kotlin.transport.ktor.tcp.TcpClientTransport

/**
 * HTTP connection implementation based on this article
 * http://android-developers.blogspot.com/2011/09/androids-http-clients.html
 * Also DefaultHttpClient has been deprecated since Android 5.1
 */
abstract class HttpConnection {
    abstract fun setMethod(method: Method)

    abstract fun getResponseCode(): Int

    abstract suspend fun getResponseString(): String?

    abstract suspend fun execute()

    abstract fun setPayload(payload: String)

    abstract fun setPayload(payload: ByteArray)

    abstract fun addHeader(name: String, value: String)

    abstract fun getResponseHeader(name: String): String?

    enum class Method {
        GET,
        POST,
        PUT,
        DELETE,
        SUBSCRIBE,
        UNSUBSCRIBE
    }

    private open class HttpURLConnectionClient(protected val url: Url) : HttpConnection() {
        protected var _payload: ByteArray? = null
        protected var response: String? = null
        protected var code: Int = 0
        protected lateinit var _method: Method
        protected val requestHeaders: MutableMap<String, String> = mutableMapOf()
        protected val responseHeaders: MutableMap<String, String> = mutableMapOf()

        override fun getResponseCode(): Int {
            return code
        }

        override fun setMethod(method: Method) {
            this._method = method
        }

        override suspend fun getResponseString(): String? {
            return response
        }

        override suspend fun execute() {
            val httpResponse = when (_method) {
                Method.GET -> {
                    NetworkHelper.instance.get(url = url.toString()) {
                        if (_payload?.isNotEmpty() == true) {
                            setBody(
                                ByteArrayContent(
                                    _payload!!,
                                    ContentType.Application.OctetStream
                                )
                            )
                        }
                        if (requestHeaders.isNotEmpty()) {
                            requestHeaders.forEach {
                                this.header(it.key, it.value)
                            }
                        }
                    }
                }

                Method.POST -> {
                    NetworkHelper.instance.post(url = url.toString()) {
                        if (_payload?.isNotEmpty() == true) {
                            setBody(
                                ByteArrayContent(
                                    _payload!!,
                                    ContentType.Application.OctetStream
                                )
                            )
                        }
                        if (requestHeaders.isNotEmpty()) {
                            requestHeaders.forEach {
                                this.header(it.key, it.value)
                            }
                        }
                    }
                }

                Method.PUT -> {
                    NetworkHelper.instance.client.put(url = url) {
                        if (_payload?.isNotEmpty() == true) {
                            setBody(
                                ByteArrayContent(
                                    _payload!!,
                                    ContentType.Application.OctetStream
                                )
                            )
                        }
                        if (requestHeaders.isNotEmpty()) {
                            requestHeaders.forEach {
                                this.header(it.key, it.value)
                            }
                        }
                    }
                }

                Method.DELETE -> {
                    NetworkHelper.instance.client.delete(url = url) {
                        if (_payload?.isNotEmpty() == true) {
                            setBody(
                                ByteArrayContent(
                                    _payload!!,
                                    ContentType.Application.OctetStream
                                )
                            )
                        }
                        if (requestHeaders.isNotEmpty()) {
                            requestHeaders.forEach {
                                this.header(it.key, it.value)
                            }
                        }
                    }
                }

                Method.SUBSCRIBE -> TODO()
                Method.UNSUBSCRIBE -> TODO()
            }

            code = httpResponse.status.value
            response = httpResponse.bodyAsText()
            httpResponse.headers.forEach { key, values ->
                responseHeaders[key] = values.first()
            }
        }

        override fun setPayload(payload: String) {
            this._payload = payload.toByteArray()
        }

        override fun setPayload(payload: ByteArray) {
            this._payload = payload
        }

        override fun addHeader(name: String, value: String) {
            requestHeaders[name] = value
        }

        override fun getResponseHeader(name: String): String? {
            return responseHeaders[name]
        }
    }

    private class CustomConnectionClient(url: Url) : HttpURLConnectionClient(url) {
        override suspend fun execute() {
            val port = if (url.port > 0) url.port else 80
            val transport = TcpClientTransport(url.host, port)
            val connector = RSocketConnector {
                //configuration goes here
            }
            val rsocket: RSocket = connector.connect(transport)
            val payloadString = StringBuilder().apply {
                // send request
                append(_method.name)
                append(" ")
                append(url.encodedPathAndQuery)
                append(" HTTP/1.1\r\n")

                append("Host:")
                append(url.host)
                append(":")
                append(port)
                append("\r\n")

                for ((key, value) in requestHeaders) {
                    append(key)
                    append(":")
                    append(value)
                    append("\r\n")
                }
                append("\r\n")

                if (_payload != null) {
                    append(_payload!!.decodeToString())
                }
            }.toString()
            //use rsocket to do request
            val payloadResponse = rsocket.requestResponse(buildPayload { data(payloadString) })

            // receive response
            val sb = StringBuilder()
            val reader = payloadResponse.data
            var line: String? = reader.readUTF8Line()
            if (line != null) {
                val tokens = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (tokens.size > 2) {
                    code = tokens[1].toInt()
                }
            }

            while (null != (reader.readUTF8Line().also { line = it })) {
                if (line!!.isEmpty()) {
                    break
                }
                val pair = line!!.split(":".toRegex(), limit = 2).toTypedArray()
                if (pair.size == 2) {
                    responseHeaders[pair[0].trim { it <= ' ' }] = pair[1].trim { it <= ' ' }
                }
            }

            while (null != (reader.readUTF8Line().also { line = it })) {
                sb.append(line)
                sb.append("\r\n")
            }
            response = sb.toString()

        }

        override fun setPayload(payload: ByteArray) {
            throw UnsupportedOperationException()
        }

    }

    companion object {
        fun newInstance(url: Url): HttpConnection {
            return HttpURLConnectionClient(url)
        }

        fun newSubscriptionInstance(url: Url): HttpConnection {
            return CustomConnectionClient(url)
        }
    }
}