package com.fleeksoft.connectsdk

import com.fleeksoft.connectsdk.ported.multicastsocket.MulticastSocketKmp
import kotlin.test.Test
import kotlin.test.assertEquals


class MulticastSocketTest {

    @Test
    fun createInstanceTest() {
        val mSocket = MulticastSocketKmp(80)
        assertEquals(mSocket.port, 80)
    }
}

