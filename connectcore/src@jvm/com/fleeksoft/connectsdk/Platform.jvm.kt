package com.fleeksoft.connectsdk

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

class JvmPlatform : Platform {
    override val name: String = "Jvm"
}

actual fun getPlatform(): Platform = JvmPlatform()


internal actual fun provideHttpClientEngine(): HttpClientEngine {
    return OkHttp.create()
}