package com.fleeksoft.connectsdk.helper

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*

internal class NetworkHelper(val client: HttpClient) {
    companion object {
        lateinit var instance: NetworkHelper
            private set

        fun init(engine: HttpClientEngine) {
            instance = NetworkHelper(
                HttpClient(engine) {
                    this.followRedirects = true
                }
            )
        }
    }

    suspend fun get(
        url: String,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return client.get(url) {
            httpRequestBuilder()
        }
    }

    suspend fun submitForm(
        url: String,
        params: Map<String, String>,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return client.submitForm(
            url = url,
            formParameters =
            parameters {
                params.forEach { (key, value) ->
                    append(key, value)
                }
            },
        ) {
            httpRequestBuilder()
        }
    }

    suspend fun post(
        url: String,
        httpRequestBuilder: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        return client.post(url) {
            httpRequestBuilder()
        }
    }
}
