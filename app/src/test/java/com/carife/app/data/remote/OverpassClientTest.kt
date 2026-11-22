package com.carife.app.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class OverpassClientTest {

    @Test
    fun `partial response is retried against another Overpass server`() = runBlocking {
        val requestedHosts = mutableListOf<String>()
        val httpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                requestedHosts.add(request.url.host)
                val body = if (requestedHosts.size == 1) {
                    """{"remark":"timeout","elements":[]}"""
                } else {
                    """
                    {"elements":[{"type":"node","id":42,"lat":-6.2,"lon":106.8,"tags":{"name":"Kafe Uji"}}]}
                    """.trimIndent()
                }
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body.toResponseBody())
                    .build()
            }
            .build()

        val cafes = OverpassClient(httpClient).fetchCafes("[out:json];node(42);out;", "bekasi")

        assertEquals(listOf("overpass.private.coffee", "overpass-api.de"), requestedHosts)
        assertEquals("osm:node/42", cafes.single().id)
        assertEquals("bekasi", cafes.single().cityId)
    }
}
