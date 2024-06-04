package com.fleeksoft.connectsdk.core

import com.fleeksoft.connectsdk.helper.NetworkHelper
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import java.io.IOException
import java.net.URI
import java.util.*

/**
 * Created by oleksii.frolov on 1/30/2015.
 */
object TestUtil {
    @Throws(IOException::class)
    fun getMockUrl(content: String, applicationUrl: String?): Url {

        val mock = MockEngine {
            respond(
                content = ByteReadChannel(content),
                status = HttpStatusCode.OK,
                headers = if (applicationUrl != null) headersOf("Application-URL", applicationUrl) else headersOf()
            )
        }

        NetworkHelper.init(mock)
        return Url("http://hostname")
    }

    /**
     * Compare 2 URLs with custom parameters order
     * @param expectedUrl
     * @param targetUrl
     * @return true if URLs equal
     */
    fun compareUrls(expectedUrl: String?, targetUrl: String?): Boolean {
        val expectedURI = URI.create(expectedUrl).normalize()
        val targetURI = URI.create(targetUrl).normalize()

        val expectedQuery =
            expectedURI.query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val targetQuery: MutableList<String> = LinkedList(
            Arrays.asList(*targetURI.query.split("&".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()))

        for (item in expectedQuery) {
            if (!targetQuery.remove(item)) {
                return false
            }
        }

        if (!targetQuery.isEmpty()) {
            return false
        }

        val schemeExpected = expectedURI.scheme
        val scheme = targetURI.scheme

        val hostExpected = expectedURI.host
        val host = targetURI.host

        val pathExpected = expectedURI.path
        val path = targetURI.path

        val portExpected = expectedURI.port
        val port = targetURI.port

        return schemeExpected == scheme && hostExpected == host && pathExpected == path && portExpected == port
    }
}
