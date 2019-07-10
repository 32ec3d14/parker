package com.example.jtxyz

import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

class Parker(
    private val startingUri: URI,
    private val reporter: SiteMapReporter = ConsoleSiteMapReporter(),
    private val fetcher: PageFetcher = KtorPageFetcher(),
    private val extractor: LinkExtractor = JSoupLinkExtractor()
) {
    private val dop = 20
    private val seen = ConcurrentHashMap.newKeySet<URI>()
    private val frontier = LinkedList<URI>() // use a queue to allow breadth-first search

    // just for logging, ConcurrentLinkedQueue#size has linear performance
    private val frontierSize = AtomicInteger()


    fun crawl(maxPages: Int) = runBlocking {
        enqueue(startingUri)

        val count = AtomicInteger()
        val active = AtomicInteger()

        (0 until dop).map {
            launch {
                while (true) {
                    val ix = count.updateAndGet { x -> min(x + 1, maxPages + 1) }
                    if (ix == maxPages + 1) {
                        count.decrementAndGet()
                        break
                    }


                    active.incrementAndGet()
                    val uri = frontier.poll()
                    if (uri == null) {
                        count.decrementAndGet()
                        if (active.decrementAndGet() == 0) {
                            break
                        }
                        delay(50)
                        continue
                    }

                    frontierSize.decrementAndGet()

                    try {
                        val page = fetcher.fetch(uri)
                        val links = extractor.extract(page)

                        reporter.reportLinks(uri, links)
                        links.forEach(::enqueue)
                    } catch (e: Exception) {
                        reporter.reportError(uri, e)
                    } finally {
                        active.decrementAndGet()
                    }
                    reporter.reportProgress(ix, frontierSize.get())
                }
            }
        }.joinAll()

        reporter.reportEnd(frontierSize.get() == 0, count.get())
    }

    private fun enqueue(uri: URI) {
        val trimmed = URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)
        if (seen.add(trimmed)) {
            frontier.add(trimmed)
            frontierSize.incrementAndGet()
        }
    }
}
