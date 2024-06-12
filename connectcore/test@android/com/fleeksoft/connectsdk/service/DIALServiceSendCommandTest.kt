package com.fleeksoft.connectsdk.service

import com.fleeksoft.connectsdk.MainDispatcherRule
import com.fleeksoft.connectsdk.helper.HttpConnection
import com.fleeksoft.connectsdk.etc.helper.HttpMessage
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceCommand
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import kotlinx.coroutines.test.runTest
import org.junit.Assert

import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.io.IOException

class DIALServiceSendCommandTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    companion object {
        const val COMMAND_URL = "http://host:8080/path"
    }

    private val service: StubDIALService = StubDIALService(mock(), mock())
    private val httpConnection: HttpConnection = mock()

    inner class StubDIALService(serviceDescription: ServiceDescription, serviceConfig: ServiceConfig) :
        DIALService(serviceDescription, serviceConfig) {
        var connectionTarget: String? = null

        override fun createHttpConnection(target: String): HttpConnection {
            this.connectionTarget = target
            return httpConnection
        }
    }

    @Test
    fun testSendSimpleGetCommand() = runTest {
        val listener = Mockito.mock(ResponseListener::class.java) as ResponseListener<Any?>
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, null, listener)
        command.httpMethod = ServiceCommand.TYPE_GET

        service.sendCommand(command)

        Assert.assertEquals(COMMAND_URL, service.connectionTarget)
        Mockito.verify(httpConnection, Mockito.times(0)).setMethod(any<HttpConnection.Method>())
        Mockito.verify(httpConnection, Mockito.times(1)).execute()
    }

    @Test
    fun testSendSimpleDeleteCommand() = runTest {
        val listener = Mockito.mock(ResponseListener::class.java) as ResponseListener<Any?>
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, null, listener)
        command.httpMethod = ServiceCommand.TYPE_DEL

        service.sendCommand(command)

        Assert.assertEquals(COMMAND_URL, service.connectionTarget)
        Mockito.verify(httpConnection, Mockito.times(1)).setMethod(eq(HttpConnection.Method.DELETE))
        Mockito.verify(httpConnection, Mockito.times(1)).execute()
    }

    @Test
    fun testSendSimplePostCommand() = runTest {
        val payload = "postdata"
        val listener = Mockito.mock(ResponseListener::class.java) as ResponseListener<Any?>
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, payload, listener)

        service.sendCommand(command)

        Assert.assertEquals(COMMAND_URL, service.connectionTarget)
        Mockito.verify(httpConnection, Mockito.times(1))
            .addHeader(eq(HttpMessage.CONTENT_TYPE_HEADER), eq("text/plain; charset=\"utf-8\""))
        Mockito.verify(httpConnection, Mockito.times(1)).setMethod(eq(HttpConnection.Method.POST))
        Mockito.verify(httpConnection, Mockito.times(1)).setPayload(eq(payload))
        Mockito.verify(httpConnection, Mockito.times(1)).execute()
    }

    @Test
    fun testSendPostCommandWithEmptyPayload() = runTest {
        val payload: Any? = null
        val listener = Mockito.mock(ResponseListener::class.java) as ResponseListener<Any?>
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, payload, listener)

        service.sendCommand(command)

        Assert.assertEquals(COMMAND_URL, service.connectionTarget)
        Mockito.verify(httpConnection, Mockito.times(1)).setMethod(eq(HttpConnection.Method.POST))
        Mockito.verify(httpConnection, Mockito.times(0)).setPayload(Mockito.anyString())
        Mockito.verify(httpConnection, Mockito.times(1)).execute()
    }

    @Test
    fun testSendCommand200ShouldInvokeSuccess() = runTest {
        val listener = Mockito.mock(ResponseListener::class.java) as ResponseListener<Any?>
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, null, listener)
        val response = "responsedata"
        Mockito.`when`(httpConnection.getResponseCode()).thenReturn(200)
        Mockito.`when`(httpConnection.getResponseString()).thenReturn(response)

        service.sendCommand(command)

        Mockito.verify(listener).onSuccess(eq(response))
    }

    @Test
    fun testSendCommand201ShouldInvokeSuccess() = runTest {
        val listener = Mockito.mock(ResponseListener::class.java) as ResponseListener<Any?>
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, null, listener)
        val response = "responsedata"
        Mockito.`when`(httpConnection.getResponseCode()).thenReturn(201)
        Mockito.`when`(httpConnection.getResponseHeader(eq("Location"))).thenReturn(response)

        service.sendCommand(command)

        Mockito.verify(listener).onSuccess(eq(response))
    }

    @Test
    fun testSendCommand400ShouldInvokeError() {
        verifyFailedConnection(400)
    }

    @Test
    fun testSendCommand404ShouldInvokeError() {
        verifyFailedConnection(404)
    }

    @Test
    fun testSendCommand500ShouldInvokeError() {
        verifyFailedConnection(500)
    }

    @Throws(IOException::class)
    private fun verifyFailedConnection(code: Int) = runTest {
        val listener = Mockito.mock(ResponseListener::class.java) as ResponseListener<Any?>
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, null, listener)
        val response = "responsedata"
        Mockito.`when`(httpConnection.getResponseCode()).thenReturn(code)
        Mockito.`when`(httpConnection.getResponseString()).thenReturn(response)

        service.sendCommand(command)

        Mockito.verify(listener).onError(any())
    }
}


