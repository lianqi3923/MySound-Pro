package io.github.mysoundpro.core.http

import io.github.mysoundpro.core.log.Logger
import io.github.mysoundpro.core.log.LogEvent
import io.github.mysoundpro.core.log.NoOpLogger
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OkHttpHttpClient(
    private val callFactory: Call.Factory,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val logger: Logger = NoOpLogger,
    private val sleeper: suspend (Long) -> Unit = { delay(it) },
) : HttpClient {
    override suspend fun execute(request: HttpRequest): HttpResponse {
        var retryCount = 0
        while (true) {
            val startedAt = System.nanoTime()
            try {
                val response = callFactory.newCall(request.toOkHttpRequest()).await()
                if (retryPolicy.shouldRetry(response.code, null, retryCount)) {
                    response.close()
                    sleeper(retryPolicy.delayMs(retryCount++))
                    continue
                }
                response.use {
                    val result = HttpResponse(
                        code = it.code,
                        finalUrl = it.request.url.toString(),
                        headers = it.headers.toMultimap(),
                        body = it.body?.bytes() ?: ByteArray(0),
                    )
                    logger.debug(
                        LogEvent(
                            "http_request",
                            mapOf(
                                "url" to request.url,
                                "status" to result.code,
                                "elapsedMs" to (System.nanoTime() - startedAt) / 1_000_000,
                            ),
                        ),
                    )
                    return result
                }
            } catch (failure: IOException) {
                if (!retryPolicy.shouldRetry(null, failure, retryCount)) throw failure
                sleeper(retryPolicy.delayMs(retryCount++))
            }
        }
    }

    private fun HttpRequest.toOkHttpRequest(): Request {
        val builder = Request.Builder().url(url).get().header("User-Agent", userAgent)
        referer?.let { builder.header("Referer", it) }
        headers.forEach { (name, value) -> builder.header(name, value) }
        return builder.build()
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = continuation.resumeFailure(e)

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response) else response.close()
            }
        })
    }

    private fun CancellableContinuation<Response>.resumeFailure(failure: IOException) {
        if (isActive) resumeWithException(failure)
    }
}
