package com.fleeksoft.connectsdk.service.command

import com.fleeksoft.connectsdk.helper.NetworkHelper
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import io.ktor.client.request.prepareDelete
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.preparePut
import io.ktor.client.statement.HttpStatement
import kotlinx.serialization.json.JsonObject

/**
 * Internal implementation of ServiceCommand for URL-based commands
 */
open class ServiceCommand<T : ResponseListener<*>> {
    var processor: ServiceCommandProcessor?
    var httpMethod: String // WebOSTV: {request, subscribe}, NetcastTV: {GET, POST}
    var payload: Any?
    var target: String

    var requestId: Int = 0

    var responseListener: ResponseListener<Any?>?

    constructor(
        processor: ServiceCommandProcessor?,
        targetURL: String,
        payload: Any?,
        listener: ResponseListener<Any?>?,
    ) {
        this.processor = processor
        this.target = targetURL
        this.payload = payload
        this.responseListener = listener
        this.httpMethod = TYPE_POST
    }

    constructor(
        processor: ServiceCommandProcessor?,
        uri: String,
        payload: JsonObject?,
        isWebOS: Boolean,
        listener: ResponseListener<Any?>?,
    ) {
        this.processor = processor
        target = uri
        this.payload = payload
        requestId = -1
        httpMethod = "request"
        responseListener = listener
    }

    open suspend fun send() {
        processor!!.sendCommand(this)
    }

    suspend fun getRequest(): HttpStatement? {

        return if (httpMethod.equals(TYPE_GET, ignoreCase = true)) {
            NetworkHelper.instance.client.prepareGet(urlString = target)
        } else if (httpMethod.equals(TYPE_POST, ignoreCase = true)) {
            NetworkHelper.instance.client.preparePost(urlString = target)
        } else if (httpMethod.equals(TYPE_DEL, ignoreCase = true)) {
            NetworkHelper.instance.client.prepareDelete(urlString = target)
        } else if (httpMethod.equals(TYPE_PUT, ignoreCase = true)) {
            NetworkHelper.instance.client.preparePut(urlString = target)
        } else {
            null
        }
    }

    interface ServiceCommandProcessor {
        suspend fun unsubscribe(subscription: URLServiceSubscription<*>?)
        fun unsubscribe(subscription: ServiceSubscription<*>?)
        suspend fun sendCommand(command: ServiceCommand<*>)
    }

    companion object {
        const val TYPE_REQ: String = "request"
        const val TYPE_SUB: String = "subscribe"
        const val TYPE_GET: String = "GET"
        const val TYPE_POST: String = "POST"
        const val TYPE_DEL: String = "DELETE"
        const val TYPE_PUT: String = "PUT"
    }
}