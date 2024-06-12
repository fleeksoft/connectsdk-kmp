package com.fleeksoft.connectsdk.service.capability.upnp;

import com.fleeksoft.connectsdk.service.command.URLServiceSubscription
import com.fleeksoft.connectsdk.service.upnp.DLNAHttpServer
import kotlinx.coroutines.test.runTest

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Created by oleksii on 4/27/15.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
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

