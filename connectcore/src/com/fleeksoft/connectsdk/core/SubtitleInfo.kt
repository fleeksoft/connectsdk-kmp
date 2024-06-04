package com.fleeksoft.connectsdk.core

/**
 * Normalized reference object for information about a subtitle track. It's used in `MediaInfo` class.
 * The only one required parameter is `url`, others can be `null`. This class is immutable and has
 * a builder for easy construction.
 *
 * Different services support specific subtitle formats:
 * - `DLNAService` supports only SRT subtitles. Since there is no official specification for them,
 * subtitles may not work on all DLNA-compatible devices
 * - `NetcastTVService` supports only SRT subtitles and has the same restrictions as `DLNAService`
 * - `CastService` supports only WebVTT subtitles and it has additional requirements
 * @see {@link https://developers.google.com/cast/docs/android_sender.cors-requirements}
 * - `FireTVService` supports only WebVTT subtitles
 * - `WebOSTVService` supports WebVTT subtitles. Server providing subtitles should
 * support CORS headers, similarly to Cast service's requirements.
 */
class SubtitleInfo private constructor(builder: Builder) {
    private val url = builder.url
    val mimeType: String?
    val label: String?
    val language: String?

    class Builder(
        val url: String,
    ) {
        // optional fields
        var mimeType: String? = null
        var label: String? = null
        var language: String? = null

        fun setMimeType(mimeType: String): Builder {
            this.mimeType = mimeType
            return this
        }

        fun setLabel(label: String): Builder {
            this.label = label
            return this
        }

        fun setLanguage(language: String): Builder {
            this.language = language
            return this
        }

        fun build(): SubtitleInfo {
            return SubtitleInfo(this)
        }
    }

    init {
        mimeType = builder.mimeType
        label = builder.label
        language = builder.language
    }

    fun getUrl(): String {
        return url
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || this::class != o::class) return false

        val that = o as SubtitleInfo

        if (getUrl() != that.getUrl()) {
            return false
        }
        return !(if (mimeType != null) mimeType != that.mimeType else that.mimeType != null)
    }

    override fun hashCode(): Int {
        var result = getUrl().hashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        return result
    }
}
