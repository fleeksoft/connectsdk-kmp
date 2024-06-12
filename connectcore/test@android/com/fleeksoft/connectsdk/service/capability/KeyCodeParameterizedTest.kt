/*
 * KeyCodeParameterizedTest
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Oleksii Frolov on 20 Aug 2015
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
package com.fleeksoft.connectsdk.service.capability

import com.fleeksoft.connectsdk.service.capability.KeyControl.KeyCode
import org.junit.Assert

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class KeyCodeParameterizedTest(private val index: Int) {

    private val value: Int = data[index][0] as Int
    private val keyCode: KeyCode? = data[index][1] as KeyCode?

    companion object {
        @JvmStatic
        val data: Array<Array<Any?>> = arrayOf(
            arrayOf(0, KeyCode.NUM_0),
            arrayOf(1, KeyCode.NUM_1),
            arrayOf(2, KeyCode.NUM_2),
            arrayOf(3, KeyCode.NUM_3),
            arrayOf(4, KeyCode.NUM_4),
            arrayOf(5, KeyCode.NUM_5),
            arrayOf(6, KeyCode.NUM_6),
            arrayOf(7, KeyCode.NUM_7),
            arrayOf(8, KeyCode.NUM_8),
            arrayOf(9, KeyCode.NUM_9),
            arrayOf(10, KeyCode.DASH),
            arrayOf(11, KeyCode.ENTER),
            arrayOf(12, null),
            arrayOf(-1, null),
            arrayOf(999, null)
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters
        fun data(): Collection<Array<Int>> = listOf(
            arrayOf(0),
            arrayOf(1),
            arrayOf(2),
            arrayOf(3),
            arrayOf(4),
            arrayOf(5),
            arrayOf(6),
            arrayOf(7),
            arrayOf(8),
            arrayOf(9),
            arrayOf(10),
            arrayOf(11),
            arrayOf(12),
            arrayOf(13),
            arrayOf(14)
        )
    }

    @Test
    fun testGetKeyCodeFromInt() {
        Assert.assertEquals(keyCode, KeyCode.createFromInteger(value))
    }
}
