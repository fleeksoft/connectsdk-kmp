package com.fleeksoft.connectsdk.service.capability

import com.fleeksoft.connectsdk.service.capability.CapabilityMethods.CapabilityPriorityLevel
import com.fleeksoft.connectsdk.service.capability.listeners.ResponseListener

interface KeyControl : CapabilityMethods {
    enum class KeyCode(val code: Int) {
        NUM_0(0),
        NUM_1(1),
        NUM_2(2),
        NUM_3(3),
        NUM_4(4),
        NUM_5(5),
        NUM_6(6),
        NUM_7(7),
        NUM_8(8),
        NUM_9(9),

        DASH(10),
        ENTER(11);

        companion object {
            private val codes: Array<KeyCode> = arrayOf(
                NUM_0, NUM_1, NUM_2, NUM_3, NUM_4, NUM_5, NUM_6, NUM_7, NUM_8, NUM_9, DASH, ENTER
            )

            fun createFromInteger(keyCode: Int): KeyCode? {
                if (keyCode >= 0 && keyCode < codes.size) {
                    return codes.get(keyCode)
                }
                return null
            }
        }
    }

    val keyControl: KeyControl
    val keyControlCapabilityLevel: CapabilityPriorityLevel?

    fun up(listener: ResponseListener<Any?>)
    fun down(listener: ResponseListener<Any?>)
    fun left(listener: ResponseListener<Any?>)
    fun right(listener: ResponseListener<Any?>)
    fun ok(listener: ResponseListener<Any?>)
    fun back(listener: ResponseListener<Any?>)
    fun home(listener: ResponseListener<Any?>)
    fun sendKeyCode(keycode: KeyCode, listener: ResponseListener<Any?>)

    companion object {
        val Any: String = "KeyControl.Any"

        val Up: String = "KeyControl.Up"
        val Down: String = "KeyControl.Down"
        val Left: String = "KeyControl.Left"
        val Right: String = "KeyControl.Right"
        val OK: String = "KeyControl.OK"
        val Back: String = "KeyControl.Back"
        val Home: String = "KeyControl.Home"
        val Send_Key: String = "KeyControl.SendKey"
        val KeyCode: String = "KeyControl.KeyCode"

        val Capabilities: Array<String> = arrayOf(
            Up,
            Down,
            Left,
            Right,
            OK,
            Back,
            Home,
            KeyCode,
        )
    }
}
