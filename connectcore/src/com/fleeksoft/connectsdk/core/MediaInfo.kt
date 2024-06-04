/*
 * MediaInfo
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Simon Gladkoskok on 14 August 2014
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
package com.fleeksoft.connectsdk.core

/**
 * Normalized reference object for information about a media to display. This object can be used
 * to pass as a parameter to displayImage or playMedia.
 */
class MediaInfo private constructor(builder: Builder) {
    /**
     * Gets URL address of a media file.
     */
    /**
     * Sets URL address of a media file.
     * This method is deprecated
     */
    // @cond INTERNAL
    var url: String
        private set
    var subtitleInfo: SubtitleInfo? = null
        private set
    /**
     * Gets type of a media file.
     */
    /**
     * Sets type of a media file.
     *
     * This method is deprecated
     */
    var mimeType: String
        private set
    /**
     * Gets description for a media.
     */
    /**
     * Sets description for a media.
     * This method is deprecated
     */
    var description: String?
        private set
    /**
     * Gets title for a media file.
     */
    /**
     * Sets title of a media file.
     *
     * This method is deprecated
     */
    var title: String?
        private set

    /**
     * Gets list of ImageInfo objects for images representing a media (ex. icon, poster).
     * Where first ([0]) is icon image, and second ([1]) is poster image.
     */
    /**
     * Sets list of ImageInfo objects for images representing a media (ex. icon, poster).
     * Where first ([0]) is icon image, and second ([1]) is poster image.
     *
     * This method is deprecated
     */
    /**
     * list of imageInfo objects where [0] is icon, [1] is poster
     */
    var images: List<ImageInfo?>? = null
        private set

    /**
     * Gets duration of a media file.
     */
    /**
     * Sets duration of a media file.
     * This method is deprecated
     */
    var duration: Long = 0
        private set

    class Builder(val url: String, val mimeType: String) {
        // optional parameters
        var title: String? = null
        var description: String? = null
        var allImages: MutableList<ImageInfo>? = null
        var subtitleInfo: SubtitleInfo? = null

        fun setTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun setDescription(description: String): Builder {
            this.description = description
            return this
        }

        fun setIcon(iconUrl: String): Builder {
            return setIcon(ImageInfo(iconUrl))
        }

        fun setIcon(icon: ImageInfo): Builder {
            // Currently only one image is used by all services with index 0
            if (allImages == null) {
                allImages = mutableListOf(icon)
            } else if (allImages!!.isEmpty()) {
                allImages!!.add(icon)
            } else {
                allImages!![0] = icon
            }
            return this
        }

        fun setSubtitleInfo(subtitleInfo: SubtitleInfo): Builder {
            this.subtitleInfo = subtitleInfo
            return this
        }

        fun setImages(images: List<ImageInfo>): Builder {
            this.allImages = images.toMutableList()
            return this
        }

        fun build(): MediaInfo {
            return MediaInfo(this)
        }
    }

    init {
        url = builder.url
        mimeType = builder.mimeType
        title = builder.title
        description = builder.description
        subtitleInfo = builder.subtitleInfo
        images = builder.allImages
    }
}
