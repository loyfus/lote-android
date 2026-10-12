package br.com.loyfus.data

import br.com.loyfus.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Thin HTTP client for the Loyfus API. */
class LoyfusApiClient(
    private val deviceId: String,
    baseUrl: String = BuildConfig.API_BASE_URL,
) {
    private val base: HttpUrl = baseUrl.toHttpUrl()

    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private fun url(path: String, params: Map<String, String?> = emptyMap()): HttpUrl =
        base.newBuilder()
            .addPathSegments(path.trimStart('/'))
            .apply { params.forEach { (k, v) -> if (v != null) addQueryParameter(k, v) } }
            .build()

    private fun request(url: HttpUrl): Request = Request.Builder()
        .url(url)
        .header("Accept", "application/json")
        .header("X-Device-Id", deviceId)
        .build()

    suspend fun getRaw(path: String, params: Map<String, String?> = emptyMap()): String =
        withContext(Dispatchers.IO) { http.newCall(request(url(path, params))).await() }

    suspend fun post(path: String): String = withContext(Dispatchers.IO) {
        val call = Request.Builder().url(url(path)).header("X-Device-Id", deviceId)
            .post(ByteArray(0).toRequestBody(null)).build()
        http.newCall(call).await()
    }

    suspend fun delete(path: String): String = withContext(Dispatchers.IO) {
        val call = Request.Builder().url(url(path)).header("X-Device-Id", deviceId).delete().build()
        http.newCall(call).await()
    }

    companion object {
        val parser = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
    }
}

private suspend fun Call.await(): String = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if (it.isSuccessful) {
                    continuation.resume(it.body.string())
                } else {
                    continuation.resumeWithException(ApiException(it.code, "HTTP ${it.code}"))
                }
            }
        }
    })
    continuation.invokeOnCancellation { runCatching { cancel() } }
}

class ApiException(val code: Int, message: String) : IOException(message)
