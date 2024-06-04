package com.fleeksoft.connectsdk.service.command

class NotSupportedServiceSubscription<T> : ServiceSubscription<T> {
    private val _listeners: MutableList<T> = ArrayList()

    override suspend fun unsubscribe() {
    }

    override fun addListener(listener: T): T {
        _listeners.add(listener)

        return listener
    }

    override fun getListeners(): List<T> {
        return _listeners
    }

    override fun removeListener(listener: T) {
        _listeners.remove(listener)
    }
}
