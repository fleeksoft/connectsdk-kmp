package com.fleeksoft.connectsdk.service.upnp

import com.fleeksoft.connectsdk.core.ImageInfo
import com.fleeksoft.connectsdk.core.MediaInfo
import com.fleeksoft.connectsdk.helper.NetworkHelper
import korlibs.io.serialization.xml.Xml
import net.thauvin.erik.urlencoder.UrlEncoderUtil

object DLNAMediaInfoParser {
    private const val APOS = "&amp;apos;"
    private const val LT = "&lt;"
    private const val GT = "&gt;"
    private const val TITLE = "dc:title"
    private const val CREATOR = "dc:creator"
    private const val ARTIST = "r:albumArtist"
    private const val THUMBNAIL = "upnp:albumArtURI"
    private const val ALBUM = "upnp:album"
    private const val GENRE = "upnp:genre"
    private const val RADIOTITLE = "r:streamContent"

    private fun getData(str: String, data: String): String {
        if (str.contains(toEndTag(data))) {
            val startInd = (str.indexOf(toStartTag(data))
                    + toStartTag(data).length)
            val endInd = str.indexOf(toEndTag(data))
            return (toString(str.substring(startInd, endInd)))
        }

        if (str.contains(LT)) return ""

        try {
            val xml = Xml.parse(str)
            return xml.getString(data) ?: ""
            /*while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val name = parser.name
                    if (name == data) {
                        eventType = parser.next()
                        if (eventType == XmlPullParser.TEXT) return parser.text
                    }
                }
                eventType = parser.next()
            }*/
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ""
    }

    fun getMediaInfo(str: String): MediaInfo {
        val url = getURL(str)
        val title = getTitle(str)
        val mimeType = getMimeType(str)
        val description = """
             ${getArtist(str)}
             ${getAlbum(str)}
             """.trimIndent()
        val iconUrl = getThumbnail(str)

        val list = mutableListOf<ImageInfo>()
        list.add(ImageInfo(iconUrl))
        return MediaInfo.Builder(url, mimeType)
            .setTitle(title)
            .setDescription(description)
            .setImages(list)
            .build()
    }

    suspend fun getMediaInfo(str: String, baseUrl: String): MediaInfo {
        val url = getURL(str)
        val title = getTitle(str)
        val mimeType = getMimeType(str)
        val description = """
             ${getArtist(str)}
             ${getAlbum(str)}
             """.trimIndent()
        var iconUrl = getThumbnail(str)

        try {
            // TODO: replace with head
            NetworkHelper.instance.get(iconUrl)
        } catch (e: Exception) {
            iconUrl = baseUrl + iconUrl
        }

        val list = mutableListOf<ImageInfo>()
        list.add(ImageInfo(iconUrl))
        return MediaInfo.Builder(url, mimeType)
            .setTitle(title)
            .setDescription(description)
            .setImages(list)
            .build()
    }

    fun getTitle(str: String): String {
        if (getData(str, RADIOTITLE) != "") return getData(str, RADIOTITLE)
        return getData(str, TITLE)
    }

    fun getArtist(str: String): String {
        return getData(str, CREATOR)
    }

    fun getAlbum(str: String): String {
        return getData(str, ALBUM)
    }

    fun getGenre(str: String): String {
        return getData(str, GENRE)
    }

    @Suppress("deprecation")
    fun getThumbnail(str: String): String {
        var res = getData(str, THUMBNAIL)
        res = UrlEncoderUtil.decode(res)
        return res
    }

    fun getMimeType(str: String): String {
        if (str.contains("protocolInfo")) {
            val startInd = str.indexOf("*:") + 2
            val endInd = str.substring(startInd).indexOf(":") + startInd
            return str.substring(startInd, endInd)
        }
        return ""
    }

    @Suppress("deprecation")
    fun getURL(str: String): String {
        if (str.contains(LT)) {
            if (str.contains(toEndTag("res"))) {
                val startInd = (str.substring(str.indexOf(LT + "res")).indexOf(GT)
                        + str.indexOf(LT + "res") + GT.length)
                val endInd = str.indexOf(toEndTag("res"))
                return UrlEncoderUtil.decode(str.substring(startInd, endInd))
            }
            return ""
        } else return getData(str, "res")
    }

    private fun toStartTag(str: String): String {
        return (LT + str + GT)
    }

    private fun toEndTag(str: String): String {
        return toStartTag("/$str")
    }

    private fun toString(text: String): String {
        val sb = StringBuilder()
        if (text.contains(APOS)) {
            sb.append(text.substring(0, text.indexOf(APOS)))
            sb.append("'")
            sb.append(text.substring(text.indexOf(APOS) + APOS.length))
        } else return text

        return sb.toString()
    }
}
