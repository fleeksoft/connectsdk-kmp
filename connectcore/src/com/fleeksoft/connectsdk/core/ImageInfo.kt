/*
 * ImageInfo
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

import kotlinx.serialization.Serializable

/**
 * Normalized reference object for information about an image file. This object can be used to represent a media file (ex. icon, poster)
 *
 */
@Serializable
data class ImageInfo(
    var url: String?,
    var type: ImageType? = null,
    var width: Int = 0,
    var height: Int = 0,
) {

    enum class ImageType {
        Thumb, Video_Poster, Album_Art, Unknown
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || this::class != other::class) {
            return false
        }

        val imageInfo = other as ImageInfo

        return (if (url != null) url == imageInfo.url else imageInfo.url == null)
    }

    override fun hashCode(): Int {
        return if (url != null) url.hashCode() else 0
    }
}