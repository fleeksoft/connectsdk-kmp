package com.fleeksoft.connectsdk.device

import com.fleeksoft.connectsdk.discovery.DiscoveryManager
import com.fleeksoft.connectsdk.service.DIALService
import com.fleeksoft.connectsdk.service.DLNAService
import com.fleeksoft.connectsdk.service.DeviceService
import com.fleeksoft.connectsdk.service.capability.Launcher
import com.fleeksoft.connectsdk.service.capability.MediaPlayer
import com.fleeksoft.connectsdk.service.config.ServiceConfig
import com.fleeksoft.connectsdk.service.config.ServiceDescription
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import kotlin.test.BeforeTest

class ConnectableDeviceTest {

    private lateinit var device: ConnectableDevice

    @BeforeTest
    fun setUp() {
        DiscoveryManager.init()
        device = ConnectableDevice()
    }

    @Test
    fun testHasCapabilityWithEmptyServices() {
        Assert.assertFalse(device.hasCapability(MediaPlayer.Display_Image))
    }

    @Test
    fun testHasCapabilityWithServices() {
        val service = Mockito.mock(DeviceService::class.java)
        Mockito.`when`(service.hasCapability(MediaPlayer.Display_Image)).thenReturn(true)
        device.services["service"] = service
        Assert.assertTrue(device.hasCapability(MediaPlayer.Display_Image))
    }

    @Test
    fun testHasAnyCapabilities() {
        val service = Mockito.mock(DeviceService::class.java)
        val capabilities = arrayOf(Launcher.Browser, Launcher.YouTube)
        Mockito.`when`(service.hasAnyCapability(*capabilities)).thenReturn(true)
        device.services["service"] = service
        Assert.assertTrue(device.hasAnyCapability(*capabilities))
    }

    @Test
    fun testHasAnyCapabilitiesWithoutServices() {
        val service = Mockito.mock(DeviceService::class.java)
        val capabilities = arrayOf(Launcher.Browser, Launcher.YouTube)
        Mockito.`when`(service.hasAnyCapability(*capabilities)).thenReturn(false)
        device.services["service"] = service
        Assert.assertFalse(device.hasAnyCapability(*capabilities))
    }

    @Test
    fun testHasCapabilities() {
        val service = Mockito.mock(DeviceService::class.java)
        Mockito.`when`(service.hasCapability(Launcher.Browser)).thenReturn(true)
        Mockito.`when`(service.hasCapability(Launcher.YouTube)).thenReturn(true)
        device.services["service"] = service

        val capabilities = mutableListOf(Launcher.Browser, Launcher.YouTube)

        Assert.assertTrue(device.hasCapabilities(capabilities))
    }

    @Test
    fun testSetPromptPairingType() {
        addAllCoreServicesToDevice()

        device.setPairingType(DeviceService.PairingType.FIRST_SCREEN)

        /*Assert.assertEquals(
            DeviceService.PairingType.FIRST_SCREEN,
            device.getServiceByName(WebOSTVService.ID)?.getPairingType()
        )*/
        /*Assert.assertEquals(
            DeviceService.PairingType.PIN_CODE,
            device.getServiceByName(NetcastTVService.ID)?.getPairingType()
        )*/
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DLNAService.ID)?.getPairingType())
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DIALService.ID)?.getPairingType())
//        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(RokuService.ID)?.getPairingType())
//        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(AirPlayService.ID)?.getPairingType())
    }

    @Test
    fun testSetPinPairingType() {
        addAllCoreServicesToDevice()

        device.setPairingType(DeviceService.PairingType.PIN_CODE)

//        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(WebOSTVService.ID)?.getPairingType())
        /*Assert.assertEquals(
            DeviceService.PairingType.PIN_CODE,
            device.getServiceByName(NetcastTVService.ID)?.getPairingType()
        )*/
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DLNAService.ID)?.getPairingType())
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DIALService.ID)?.getPairingType())
//        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(RokuService.ID)?.getPairingType())
//        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(AirPlayService.ID)?.getPairingType())
    }

    @Test
    fun testNonePairingType() {
        addAllCoreServicesToDevice()

        device.setPairingType(DeviceService.PairingType.NONE)

//        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(WebOSTVService.ID)?.getPairingType())
        /*Assert.assertEquals(
            DeviceService.PairingType.PIN_CODE,
            device.getServiceByName(NetcastTVService.ID)?.getPairingType()
        )*/
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DLNAService.ID)?.getPairingType())
        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(DIALService.ID)?.getPairingType())
//        Assert.assertEquals(DeviceService.PairingType.NONE, device.getServiceByName(RokuService.ID)?.getPairingType())
//        Assert.assertEquals(DeviceService.PairingType.PIN_CODE, device.getServiceByName(AirPlayService.ID)?.getPairingType())
    }

    private fun addAllCoreServicesToDevice() {
        /*val webOSService =WebOSTVService(createServiceDescription(WebOSTVService.ID), Mockito.mock(ServiceConfig::class.java))
        val netCastService =NetcastTVService(createServiceDescription(NetcastTVService.ID), Mockito.mock(ServiceConfig::class.java))*/
        val dialService = DIALService(createServiceDescription(DIALService.ID), Mockito.mock(ServiceConfig::class.java))
        val dlnaService = DLNAService(createServiceDescription(DLNAService.ID), Mockito.mock(ServiceConfig::class.java))
//        val rokuService = RokuService(createServiceDescription(RokuService.ID), Mockito.mock(ServiceConfig::class.java))
        /*val airPlayService =
            AirPlayService(createServiceDescription(AirPlayService.ID), Mockito.mock(ServiceConfig::class.java))*/
        /*device.services[WebOSTVService.ID] = webOSService
        device.services[NetcastTVService.ID] = netCastService*/
        device.services[DIALService.ID] = dialService
        device.services[DLNAService.ID] = dlnaService
        /*device.services[RokuService.ID] = rokuService
        device.services[AirPlayService.ID] = airPlayService*/
    }

    private fun createServiceDescription(serviceId: String): ServiceDescription {
        return ServiceDescription().apply {
            friendlyName = ""
            manufacturer = ""
            uuid = ""
            modelDescription = ""
            modelName = ""
            modelNumber = ""
            serviceID = serviceId
        }
    }
}
