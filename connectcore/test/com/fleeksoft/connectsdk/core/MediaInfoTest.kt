package com.fleeksoft.connectsdk.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaInfoTest {
    @Test
    fun testMediaInfoBuilderWithRequiredParameters() {
        val url = "http://127.0.0.1/"
        val mimeType = "video/mp4"
        val mediaInfo: MediaInfo = MediaInfo.Builder(url, mimeType).build()
        assertEquals(url, mediaInfo.url)
        assertEquals(mimeType, mediaInfo.mimeType)
        assertNull(mediaInfo.description)
        assertEquals(0, mediaInfo.duration)
        assertNull(mediaInfo.images)
        assertNull(mediaInfo.subtitleInfo)
        assertNull(mediaInfo.title)
    }

    @Test
    fun testMediaInfoBuilderWithAllParameters() {
        val url = "http://127.0.0.1/"
        val mimeType = "video/mp4"
        val description = "description"
        val iconUrl = "http://iconurl"

        val subtitle: SubtitleInfo = SubtitleInfo.Builder("").build()
        val title = "title"
        val mediaInfo: MediaInfo = MediaInfo.Builder(url, mimeType)
            .setDescription(description)
            .setIcon(iconUrl)
            .setSubtitleInfo(subtitle)
            .setTitle(title)
            .build()

        assertEquals(url, mediaInfo.url)
        assertEquals(mimeType, mediaInfo.mimeType)
        assertEquals(description, mediaInfo.description)
        assertEquals(iconUrl, mediaInfo.images!![0]!!.url)
        assertEquals(1, mediaInfo.images!!.size)
        assertEquals(subtitle, mediaInfo.subtitleInfo)
        assertEquals(title, mediaInfo.title)
    }

    @Test
    fun testMediaInfoBuilderWithNullIconShouldNotReturnNullImagesList() {
        val url = "http://127.0.0.1/"
        val mimeType = "video/mp4"
        val mediaInfo: MediaInfo = MediaInfo.Builder(url, mimeType).build()

        assertEquals(url, mediaInfo.url)
        assertEquals(mimeType, mediaInfo.mimeType)
        assertNull(mediaInfo.description)
        assertEquals(0, mediaInfo.duration)
        assertNull(mediaInfo.images)
        assertNull(mediaInfo.subtitleInfo)
        assertNull(mediaInfo.title)
    }
}
