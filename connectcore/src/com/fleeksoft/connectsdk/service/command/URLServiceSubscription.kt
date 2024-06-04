package com.fleeksoft.connectsdk.service.command

import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import kotlinx.serialization.json.JsonObject

/**
 * Internal implementation of ServiceSubscription for URL-based commands
 */
class URLServiceSubscription<T : ResponseListener<*>> : ServiceCommand<T>, ServiceSubscription<T> {
    private val _listeners: MutableList<T> = ArrayList()

    constructor(
        processor: ServiceCommandProcessor?,
        uri: String,
        payload: JsonObject?,
        listener: ResponseListener<Any?>?,
    ) : super(processor, uri, payload, listener)

    constructor(
        processor: ServiceCommandProcessor?,
        uri: String,
        payload: JsonObject?,
        isWebOS: Boolean,
        listener: ResponseListener<Any?>?,
    ) : super(processor, uri, payload, isWebOS, listener) {
        if (isWebOS) httpMethod = "subscribe"
    }

    override suspend fun send() {
        this.subscribe()
    }

    suspend fun subscribe() {
        if (!(httpMethod.equals(ServiceCommand.Companion.TYPE_GET, ignoreCase = true)
                || httpMethod.equals(ServiceCommand.Companion.TYPE_POST, ignoreCase = true))
        ) {
            httpMethod = "subscribe"
        }
        processor?.sendCommand(this)
    }

    override suspend fun unsubscribe() {
        processor?.unsubscribe(this)
    }

    override fun addListener(listener: T): T {
        _listeners.add(listener)

        return listener
    }

    override fun removeListener(listener: T) {
        _listeners.remove(listener)
    }

    fun removeListeners() {
        _listeners.clear()
    }

    override fun getListeners(): List<T> {
        return _listeners
    }
}
