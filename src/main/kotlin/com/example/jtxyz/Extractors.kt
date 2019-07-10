package com.example.jtxyz

import org.jsoup.Jsoup
import java.net.URI


interface LinkExtractor {
    fun extract(page: Page): List<URI>
}

class JSoupLinkExtractor : LinkExtractor {
    override fun extract(page: Page) =
        Jsoup
            .parse(page.content, page.uri.toString())
            .select("a[href]")
            // abs:href resolves relative links according to the uri passed in to #parse
            .eachAttr("abs:href")
            .map { URI(it) }
            // we only want to crawl internal links
            .filter { it.host == page.uri.host }
}