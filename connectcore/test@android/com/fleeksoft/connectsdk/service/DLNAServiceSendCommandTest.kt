package com.fleeksoft.connectsdk.service

import com.fleeksoft.connectsdk.MainDispatcherRule
import com.fleeksoft.connectsdk.helper.HttpConnection
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceCommand
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.io.IOException

class DLNAServiceSendCommandTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    companion object {
        const val COMMAND_URL = "http://host:8080/path"
    }

    private lateinit var service: StubDLNAService
    private lateinit var httpConnection: HttpConnection

    inner class StubDLNAService(serviceDescription: ServiceDescription, serviceConfig: ServiceConfig) :
        DLNAService(serviceDescription, serviceConfig) {
        var connectionTarget: String? = null

        @Throws(IOException::class)
        override fun createHttpConnection(targetURL: String): HttpConnection {
            this.connectionTarget = targetURL
            return httpConnection
        }
    }

    @Before
    fun setUp() {
        httpConnection = mock()
        service = StubDLNAService(mock(), mock())
    }

    @Test
    fun testSendSimplePostCommand() = runTest {
        val payload = DLNAService.AV_TRANSPORT_URN
        val listener: ResponseListener<Any?> = mock()
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, payload, listener)
        service.avTransportURL = COMMAND_URL

        service.sendCommand(command)

        Assert.assertEquals(COMMAND_URL, service.connectionTarget)

        verify(httpConnection, times(1)).setMethod(eq(HttpConnection.Method.POST))
        verify(httpConnection, times(1)).setPayload(eq(payload))
        verify(httpConnection, times(1)).execute()
    }

    @Test
    fun testSendPostCommandWithNullPayload() = runTest {
        val payload: Any? = null
        val listener: ResponseListener<Any?> = mock()
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, payload, listener)
        service.avTransportURL = COMMAND_URL

        service.sendCommand(command)

        verify(httpConnection, times(0)).execute()
        verify(listener).onError(any<ServiceCommandError>())
    }

    @Test
    fun testSendPostCommandWithWrongPayload() = runTest {
        val payload: Any = "payload"
        val listener: ResponseListener<Any?> = mock()
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, payload, listener)
        service.avTransportURL = COMMAND_URL

        service.sendCommand(command)

        verify(httpConnection, times(0)).execute()
        verify(listener).onError(any<ServiceCommandError>())
    }
}

