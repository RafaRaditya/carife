package com.carife.app.data.remote

import com.carife.app.core.model.Cafe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CafeSourceException(message: String, cause: Throwable? = null) : Exception(message, cause)

class OverpassClient(
    private val client: OkHttpClient = defaultClient(),
) {

    suspend fun fetchCafes(query: String, cityId: String): List<Cafe> {
        var lastError: IOException? = null
        for (endpoint in ENDPOINTS) {
            try {
                val body = execute(endpoint, query)
                val cafes = withContext(Dispatchers.Default) {
                    OsmCafeMapper.mapCompleteResponse(body, cityId)
                }
                return cafes ?: throw IOException("Server kafe mengirim data yang tidak lengkap.")
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw CafeSourceException(
            "Tidak bisa menghubungi server kafe. Periksa koneksimu lalu coba lagi.",
            lastError,
        )
    }

    private suspend fun execute(endpoint: String, query: String): String {
        val body = query.toRequestBody("text/plain; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .header("User-Agent", USER_AGENT)
            .post(body)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use {
                            if (!it.isSuccessful) throw IOException("HTTP ${it.code} dari ${request.url.host}")
                            it.body?.string() ?: throw IOException("Respons kosong")
                        }
                        continuation.resume(result)
                    } catch (e: IOException) {
                        continuation.resumeWithException(e)
                    }
                }
            })
        }
    }

    companion object {
        private const val USER_AGENT = "Carife/1.0 (Android; demo aplikasi kafe nugas)"

        private val ENDPOINTS = listOf(
            "https://overpass.private.coffee/api/interpreter",
            "https://overpass-api.de/api/interpreter",
        )

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(100, TimeUnit.SECONDS)
            .build()
    }
}
