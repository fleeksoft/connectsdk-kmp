package com.fleeksoft.connectsdk

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()


internal actual fun provideHttpClientEngine(): HttpClientEngine {
    return OkHttp.create()
}