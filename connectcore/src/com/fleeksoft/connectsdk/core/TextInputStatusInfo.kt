package com.fleeksoft.connectsdk.core

/** Normalized reference object for information about a text input event.  */
class TextInputStatusInfo {
    enum class TextInputType {
        DEFAULT,
        URL,
        NUMBER,
        PHONE_NUMBER,
        EMAIL
    }

    var isFocused: Boolean = false
    var contentType: String? = null
    var isPredictionEnabled: Boolean = false
    var isCorrectionEnabled: Boolean = false
    var isAutoCapitalization: Boolean = false
    var isHiddenText: Boolean = false
    var isFocusChanged: Boolean = false


    var textInputType: TextInputType?
        /** Gets the type of keyboard that should be displayed to the user.  */
        get() {
            var textInputType = TextInputType.DEFAULT

            if (contentType != null) {
                when (contentType) {
                    "number" -> {
                        textInputType = TextInputType.NUMBER
                    }

                    "phonenumber" -> {
                        textInputType = TextInputType.PHONE_NUMBER
                    }

                    "url" -> {
                        textInputType = TextInputType.URL
                    }

                    "email" -> {
                        textInputType = TextInputType.EMAIL
                    }
                }
            }

            return textInputType
        }
        /** Sets the type of keyboard that should be displayed to the user.  */
        set(textInputType) {
            contentType = when (textInputType) {
                TextInputType.NUMBER -> "number"
                TextInputType.PHONE_NUMBER -> "phonenumber"
                TextInputType.URL -> "url"
                TextInputType.EMAIL -> "number"
                TextInputType.DEFAULT -> "email"
                else -> "email"
            }
        }
}
