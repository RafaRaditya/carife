package com.carife.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class CafeSourceException(message: String, cause: Throwable? = null) : Exception(message, cause)

class OverpassClient(
    private val client: OkHttpClient = defaultClient(),
) {

    suspend fun fetchCafes(query: String): String = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        for (endpoint in ENDPOINTS) {
            try {
                return@withContext execute(endpoint, query)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw CafeSourceException(
            "Tidak bisa menghubungi server kafe. Periksa koneksimu lalu coba lagi.",
            lastError,
        )
    }

    private fun execute(endpoint: String, query: String): String {
        val body = query.toRequestBody("text/plain; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .header("User-Agent", USER_AGENT)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} dari ${request.url.host}")
            }
            return response.body?.string() ?: throw IOException("Respons kosong")
        }
    }

    companion object {
        private const val USER_AGENT = "Carife/1.0 (Android; demo aplikasi kafe nugas)"

        private val ENDPOINTS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass.private.coffee/api/interpreter",
        )

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(100, TimeUnit.SECONDS)
            .build()
    }
}
