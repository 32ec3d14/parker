package com.example.jtxyz

import com.example.jtxyz.Metrics.measureExtract
import com.example.jtxyz.Metrics.measureFetch
import com.example.jtxyz.Metrics.report
import io.ktor.client.HttpClient
import io.ktor.client.features.BrowserUserAgent
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.toURI
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ticker
import org.jsoup.Jsoup
import java.io.PrintStream
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

data class Page(val uri: URI, val content: String)

private val client = HttpClient { BrowserUserAgent() }

suspend fun fetchPage(uri: URI) = client
    .get<HttpResponse>(uri.toString())
    .run { Page(call.request.url.toURI(), readText()) }

fun extractLinks(page: Page) = Jsoup
    .parse(page.content, page.uri.toString())
    .select("a[href]")
    .eachAttr("abs:href") // abs:href resolves relative links according to the uri passed in to #parse
    .map { URI(it) }
    .filter { it.host == page.uri.host } // we only want to crawl internal links

fun stripFragment(uri: URI) =
    URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)

@ObsoleteCoroutinesApi
fun crawl(
    startingUri: URI,
    rateLimitMillis: Long,
    fetchPage: suspend (URI) -> Page = ::fetchPage,
    extractLinks: (Page) -> List<URI> = ::extractLinks,
    out: PrintStream = System.out
) = GlobalScope.launch {
    val rateLimit = ticker(delayMillis = rateLimitMillis, initialDelayMillis = 0)
    val knownLinks = ConcurrentHashMap.newKeySet<URI>().apply { add(startingUri) }

    fun crawl(uri: URI, depth: Int): Job = launch {
        rateLimit.receive() // wait until a token is available
        try {
            val page = measureFetch(depth) { fetchPage(uri) }
            val links = measureExtract { extractLinks(page) }
            links.forEach { out.println("$uri -> $it") }

            links
                .map(::stripFragment) // different fragments all correspond to the same document
                .filter(knownLinks::add) // add it to the list of known links or skip it if we already know about it
                .filterNot { it.toString().endsWith(".pdf") } // exclude some known bad file types
                .forEach { crawl(it, depth + 1) } // recursive crawl the filtered links
        } catch (e: Exception) {
            out.println("$uri -> ${e.message ?: e}")
        }
    }

    crawl(startingUri, 0)
}

const val defaultRateLimit: Long = 2

@ObsoleteCoroutinesApi
fun main(args: Array<String>) = runBlocking {
    crawl(
        URI(args.elementAtOrNull(0) ?: error("No URL provided")).apply { toURL() },
        if (args.size > 1) args[1].toLong() else defaultRateLimit
    ).join()
    report()
}