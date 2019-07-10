package com.example.jtxyz

import com.example.jtxyz.Metrics.measureRequest
import io.ktor.client.HttpClient
import io.ktor.client.features.BrowserUserAgent
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.toURI
import java.io.IOException
import java.net.URI

data class Page(val uri: URI, val content: String)

interface PageFetcher {
    suspend fun fetch(uri: URI): Page
}

class KtorPageFetcher(
    private val retryConfig: RetryConfig = RetryConfig()
) : PageFetcher {
    private val client = HttpClient {
        BrowserUserAgent()
    }

    override suspend fun fetch(uri: URI): Page = retry<Page>(retryConfig) {
        val response = measureRequest { client.get<HttpResponse>(uri.toString()) }

        when (response.status.value) {
            in 200..299 -> Complete(
                Page(response.call.request.url.toURI(), response.readText())
            )
            429 -> Retry<Page>(IOException(response.status.description))
            else -> error(response.status.description)
        }
    }

}
