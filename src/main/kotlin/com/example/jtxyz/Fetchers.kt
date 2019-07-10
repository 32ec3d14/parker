package com.example.jtxyz

import io.ktor.client.HttpClient
import io.ktor.client.features.BrowserUserAgent
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.toURI
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.URI

data class Page(val uri: URI, val content: String)

interface PageFetcher {
    fun fetch(uri: URI): Page
}

class KtorPageFetcher(
    private val retryConfig: RetryConfig = RetryConfig()
) : PageFetcher {
    private val client = HttpClient {
        BrowserUserAgent()
    }

    override fun fetch(uri: URI): Page = retry<Page>(retryConfig) {
        runBlocking {
            val response = client.get<HttpResponse>(uri.toString())

            when (response.status.value) {
                in 200..299 -> Complete(
                    Page(response.call.request.url.toURI(), response.readText())
                )
                429 -> Retry<Page>(IOException(response.status.description))
                else -> error(response.status.description)
            }
        }
    }
}
