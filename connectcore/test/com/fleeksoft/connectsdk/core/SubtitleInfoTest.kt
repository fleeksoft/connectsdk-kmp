package com.fleeksoft.connectsdk.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubtitleInfoTest {
    @Test
    fun testCreateSubtitleWithRequiredParameters() {
        val url = "http://127.0.0.1/"
        val subtitle: SubtitleInfo = SubtitleInfo.Builder(url).build()

        assertEquals(url, subtitle.getUrl())
        assertNull(subtitle.mimeType)
        assertNull(subtitle.label)
        assertNull(subtitle.language)
    }

    @Test
    fun testCreateSubtitleWithAllParameters() {
        val url = "http://127.0.0.1/"
        val mimetype = "text/vtt"
        val label = "label"
        val language = "en"
        val subtitle: SubtitleInfo = SubtitleInfo.Builder(url)
            .setMimeType(mimetype)
            .setLabel(label)
            .setLanguage(language)
            .build()

        assertEquals(url, subtitle.getUrl())
        assertEquals(mimetype, subtitle.mimeType)
        assertEquals(label, subtitle.label)
        assertEquals(language, subtitle.language)
    }
}
