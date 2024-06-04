package com.fleeksoft.connectsdk.discovery.provider.ssdp

class Icon() {
    /* Required. Icon's MIME type. */
    var mimetype: String? = null

    /* Required. Horizontal dimension of icon in pixels. */
    var width: String? = null

    /* Required. Vertical dimension of icon in pixels. */
    var height: String? = null

    /* Required. Number of color bits per pixel. */
    var depth: String? = null

    /* Required. Pointer to icon image. */
    var url: String? = null

    companion object {
        val TAG: String = "icon"
        val TAG_MIME_TYPE: String = "mimetype"
        val TAG_WIDTH: String = "width"
        val TAG_HEIGHT: String = "height"
        val TAG_DEPTH: String = "depth"
        val TAG_URL: String = "url"
    }
}
