package com.example.jtxyz

import java.net.URI
import java.util.*

class Parker(
    private val startingUri: URI,
    private val reporter: SiteMapReporter = ConsoleSiteMapReporter(),
    private val fetcher: PageFetcher = URLConnectionPageFetcher(),
    private val extractor: LinkExtractor = RegexLinkExtractor()
) {
    private val seen = mutableSetOf<URI>()
    private val frontier = LinkedList<URI>() // use a queue to allow breadth-first search

    fun crawl(maxPages: Int) {
        enqueue(startingUri)

        var i = 0
        while (i++ < maxPages && frontier.isNotEmpty()) {
            val uri = frontier.remove()
            try {
                val page = fetcher.fetch(uri)
                val links = extractor.extract(page)
                reporter.reportLinks(uri, links)
                links.forEach(::enqueue)
            } catch (e: Exception) {
                reporter.reportError(uri, e)
            }

            reporter.reportProgress(i, frontier.size)
        }

        reporter.reportEnd(frontier.isEmpty(), i - 1)
    }

    private fun enqueue(uri: URI) {
        val trimmed = URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)
        if (seen.add(trimmed)) {
            frontier.add(trimmed)
        }
    }
}
