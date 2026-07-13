package io.github.mysoundpro.core.http

import io.github.mysoundpro.core.log.Logger
import io.github.mysoundpro.core.log.RuntimeLogger
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import java.util.concurrent.TimeUnit

/** 生产默认 HTTP 栈；测试或特殊站点仍可注入自己的 Call.Factory。 */
object HttpClientFactory {
    @JvmStatic
    fun create(
        logger: Logger = RuntimeLogger,
        retryPolicy: RetryPolicy = RetryPolicy(),
    ): HttpClient {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .cookieJar(AnonymousCookieJar())
            .addInterceptor(BrotliInterceptor)
            .build()
        return OkHttpHttpClient(okHttpClient, retryPolicy, logger)
    }
}
