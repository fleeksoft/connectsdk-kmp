package com.fleeksoft.connectsdk.service.upnp;

import com.fleeksoft.connectsdk.service.command.URLServiceSubscription
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class DLNAHttpServerTest {

    private lateinit var server: DLNAHttpServer

    @Before
    fun setUp() {
        server = DLNAHttpServer()
    }

    @Test
    fun testUnsubscribeOnDisconnect() = runTest {
        val subscription: URLServiceSubscription<*> = Mockito.mock(URLServiceSubscription::class.java)
        server.subscriptions.add(subscription)
        server.isRunning = true

        server.stop()
        Mockito.verify(subscription).unsubscribe()
    }
}

