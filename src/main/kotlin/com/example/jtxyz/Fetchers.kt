package com.example.jtxyz

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

data class Page(val uri: URI, val content: String)

interface PageFetcher {
    fun fetch(uri: URI): Page
}

const val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/75.0.3770.100 Safari/537.36"

class URLConnectionPageFetcher(
    private val retryConfig: RetryConfig = RetryConfig()
) : PageFetcher {
    override fun fetch(uri: URI) = retry<Page>(retryConfig) {
        val connection = uri.toURL().openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", userAgent)

        when (connection.responseCode) {
            in 200..299 -> Complete(
                Page(
                    connection.url.toURI(), // set this after checking the response code to update based on redirects
                    connection.inputStream
                        .readBytes()
                        .toString(Charsets.UTF_8)
                )
            )
            429 -> Retry(IOException(connection.responseMessage))
            else -> error(connection.responseMessage)
        }
    }
}
