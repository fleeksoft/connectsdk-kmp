/*
 * SSDPPacket
 * Connect SDK
 * 
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 6 Jan 2015
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
package com.fleeksoft.connectsdk.discovery.provider.ssdp

import io.ktor.network.sockets.Datagram
import io.ktor.utils.io.core.readBytes
import korlibs.io.lang.Charset
import korlibs.io.lang.toString

class SSDPPacket(var datagramPacket: Datagram) {
    private var _data: MutableMap<String, String?> = HashMap()
    private var _type: String? = null

    init {
        init()
    }

    private fun init() {
        val text: String = datagramPacket.packet.readBytes().toString(charset = ASCII_CHARSET)

        var pos: Int = 0

        var eolPos: Int

        if ((text.indexOf(CRLF).also { eolPos = it }) != -1) {
            pos = eolPos + CRLF.length
        } else if ((text.indexOf(LF).also { eolPos = it }) != -1) {
            pos = eolPos + LF.length
        } else return

        // Get first line
        _type = text.substring(0, eolPos)

        while (pos < text.length) {
            var line: String
            if ((text.indexOf(CRLF, pos).also { eolPos = it }) != -1) {
                line = text.substring(pos, eolPos)
                pos = eolPos + CRLF.length
            } else if ((text.indexOf(LF, pos).also { eolPos = it }) != -1) {
                line = text.substring(pos, eolPos)
                pos = eolPos + LF.length
            } else break

            val index: Int = line.indexOf(':')
            if (index == -1) {
                continue
            }

            val key: String = asciiUpper(line.substring(0, index))
            val value: String = line.substring(index + 1).trim { it <= ' ' }

            _data.put(key, value)
        }
    }

    fun getDatagram(): Datagram {
        return datagramPacket
    }

    fun getData(): Map<String, String?> {
        return _data
    }

    fun getType(): String? {
        return _type
    }

    companion object {
        val ASCII_CHARSET: Charset = Charset.forName("US-ASCII")
        val CRLF: String = "\r\n"
        val LF: String = "\n"

        // Fast toUpperCase for ASCII strings
        private fun asciiUpper(text: String): String {
            val chars: CharArray = text.toCharArray()

            for (i in chars.indices) {
                val c: Char = chars[i]
                chars[i] = if ((c.code in 97..122)) (c.code - 32).toChar() else c
            }
            return chars.concatToString()
        }
    }
}
