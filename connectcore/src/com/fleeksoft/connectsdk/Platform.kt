package com.fleeksoft.connectsdk

import io.ktor.client.engine.HttpClientEngine

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform


internal expect fun provideHttpClientEngine(): HttpClientEngine