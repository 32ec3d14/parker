package com.example.jtxyz

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

class Parker(
    private val startingUri: URI,
    private val reporter: SiteMapReporter = ConsoleSiteMapReporter(),
    private val fetcher: PageFetcher = KtorPageFetcher(),
    private val extractor: LinkExtractor = JSoupLinkExtractor()
) {
    fun crawl(maxPages: Int) = runBlocking {
        // just for logging / monitoring
        val frontierSize = AtomicInteger(1)

        // we could use a bloom filter if we were expecting a very large set
        val knownLinks = ConcurrentHashMap.newKeySet<URI>()
        knownLinks.add(startingUri)

        val startedPageCount = AtomicInteger()
        val finishedPageCount = AtomicInteger()

        fun crawl(uri: URI): Job = launch {
            if (startedPageCount.getAndUpdate { x -> min(x + 1, maxPages) } == maxPages) {
                return@launch
            }

            val links = try {
                fetcher.fetch(uri)
                    .let { extractor.extract(it) }
                    .also { reporter.reportLinks(uri, it) }
            } catch (e: Exception) {
                reporter.reportError(uri, e)
                emptyList<URI>()
            } finally {
                frontierSize.decrementAndGet()
            }

            links
                // strip off the fragment, these all correspond to a single document
                .map { URI(it.scheme, null, it.host, it.port, it.path, it.query, null) }
                // add it to the list of known links or skip it if we already know about it
                .filter(knownLinks::add)
                // a few of these showed up in the logs. what else might we want to exclude?
                .filterNot { it.toString().endsWith(".pdf") }
                // make a note of the new relevant links for logging / monitoring
                .also { reporter.reportProgress(finishedPageCount.incrementAndGet(), frontierSize.addAndGet(it.size)) }
                // kick off the children coroutines to process the next set of documents
                .map { crawl(it) }
                .joinAll()
        }

        crawl(startingUri).join()

        reporter.reportEnd(frontierSize.get() == 0, startedPageCount.get())
    }
}
