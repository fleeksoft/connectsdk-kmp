package com.fleeksoft.connectsdk.service

import com.fleeksoft.connectsdk.service.capability.Launcher
import com.fleeksoft.connectsdk.service.command.ServiceCommand
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import kotlinx.coroutines.test.runTest
import org.junit.Assert

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class DIALServiceTest {

    private val APPLICATION_URL = "http://applicationurl"

    private lateinit var service: DIALService
    private lateinit var serviceDescription: ServiceDescription
    private lateinit var serviceConfig: ServiceConfig
    private lateinit var commandProcessor: ServiceCommand.ServiceCommandProcessor

    @Before
    fun setUp() {
        serviceDescription = mock()
        whenever(serviceDescription.applicationURL).thenReturn(APPLICATION_URL)
        serviceConfig = mock()
        commandProcessor = mock()
        service = DIALService(serviceDescription, serviceConfig)
        service.commandProcessor = commandProcessor
    }

    @Test
    fun testLaunchNetflixWithContentParameter() = runTest {
        val listener: Launcher.AppLaunchListener = mock()
        val content = "123"
        val expectedPayload = """{"v":"$content"}"""

        service.launchNetflix(content, listener)

        verifyNetflixCommand(expectedPayload)
    }

    @Test
    fun testLaunchNetflixWithoutContentParameter() = runTest {
        service.launchNetflix(null, mock())

        verifyNetflixCommand(null)
    }

    @Test
    fun testLaunchNetflixWithEmptyContentParameter() = runTest {
        service.launchNetflix("", mock())

        verifyNetflixCommand(null)
    }

    private fun verifyNetflixCommand(expectedPayload: String?) = runTest {
        val argCommand = argumentCaptor<ServiceCommand<*>>()
        verify(commandProcessor).sendCommand(argCommand.capture())
        val command = argCommand.firstValue

        Assert.assertEquals("$APPLICATION_URL/Netflix", command.target)
        Assert.assertEquals(ServiceCommand.TYPE_POST, command.httpMethod)
        Assert.assertSame(commandProcessor, command.processor)
        if (expectedPayload != null) {
            Assert.assertEquals(expectedPayload, command.payload.toString())
        } else {
            Assert.assertNull(command.payload)
        }
    }
}
