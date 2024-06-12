/*
 * DIALServiceSendCommandTest
 * Connect SDK
 *
 * Copyright (c) 2015 LG Electronics.
 * Created by Oleksii Frolov on 14 May 2015
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

package com.fleeksoft.connectsdk.service

import com.fleeksoft.connectsdk.helper.HttpConnection
import com.fleeksoft.connectsdk.etc.helper.HttpMessage
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener
import com.fleeksoft.connectsdk.service.command.ServiceCommand
import com.fleeksoft.connectsdk.service.command.ServiceCommandError
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import kotlinx.coroutines.test.runTest
import org.junit.Assert

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException


@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DIALServiceSendCommandTest {

    companion object {
        const val COMMAND_URL = "http://host:8080/path"
    }

    private lateinit var service: StubDIALService
    private lateinit var httpConnection: HttpConnection

    inner class StubDIALService(serviceDescription: ServiceDescription, serviceConfig: ServiceConfig) :
        DIALService(serviceDescription, serviceConfig) {
        var connectionTarget: String? = null

        override fun createHttpConnection(target: String): HttpConnection {
            this.connectionTarget = target
            return httpConnection
        }
    }

    @Before
    fun setUp() {
        httpConnection = Mockito.mock(HttpConnection::class.java)
        service = StubDIALService(
            Mockito.mock(ServiceDescription::class.java),
            Mockito.mock(ServiceConfig::class.java)
        )
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
        Mockito.verify(httpConnection, Mockito.times(1)).setMethod(Mockito.eq(HttpConnection.Method.DELETE))
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
            .addHeader(Mockito.eq(HttpMessage.CONTENT_TYPE_HEADER), Mockito.eq("text/plain; charset=\"utf-8\""))
        Mockito.verify(httpConnection, Mockito.times(1)).setMethod(Mockito.eq(HttpConnection.Method.POST))
        Mockito.verify(httpConnection, Mockito.times(1)).setPayload(Mockito.eq(payload))
        Mockito.verify(httpConnection, Mockito.times(1)).execute()
    }

    @Test
    fun testSendPostCommandWithEmptyPayload() = runTest {
        val payload: Any? = null
        val listener = Mockito.mock(ResponseListener::class.java) as ResponseListener<Any?>
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, payload, listener)

        service.sendCommand(command)

        Assert.assertEquals(COMMAND_URL, service.connectionTarget)
        Mockito.verify(httpConnection, Mockito.times(1)).setMethod(Mockito.eq(HttpConnection.Method.POST))
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

        Mockito.verify(listener).onSuccess(Mockito.eq(response))
    }

    @Test
    fun testSendCommand201ShouldInvokeSuccess() = runTest {
        val listener = Mockito.mock(ResponseListener::class.java) as ResponseListener<Any?>
        val command = ServiceCommand<ResponseListener<Any?>>(service, COMMAND_URL, null, listener)
        val response = "responsedata"
        Mockito.`when`(httpConnection.getResponseCode()).thenReturn(201)
        Mockito.`when`(httpConnection.getResponseHeader(Mockito.eq("Location"))).thenReturn(response)

        service.sendCommand(command)

        Mockito.verify(listener).onSuccess(Mockito.eq(response))
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


