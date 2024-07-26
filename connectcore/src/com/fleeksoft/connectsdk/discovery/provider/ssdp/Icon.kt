package com.fleeksoft.connectsdk.discovery.provider.ssdp

import korlibs.io.serialization.xml.Xml
import korlibs.io.serialization.xml.text

data class Icon(val url: String, val mimeType: String, val height: Int, val width: Int, val dept: Int) {

    companion object {
        fun fromXml(xml: Xml): Icon {
            return Icon(
                url = xml["url"].text,
                mimeType = xml["mimeType"].text,
                height = xml["height"].text.toIntOrNull() ?: 0,
                width = xml["width"].text.toIntOrNull() ?: 0,
                dept = xml["dept"].text.toIntOrNull() ?: 0,
            )
        }

        val TAG: String = "icon"
        val TAG_MIME_TYPE: String = "mimetype"
        val TAG_WIDTH: String = "width"
        val TAG_HEIGHT: String = "height"
        val TAG_DEPTH: String = "depth"
        val TAG_URL: String = "url"
    }
}
