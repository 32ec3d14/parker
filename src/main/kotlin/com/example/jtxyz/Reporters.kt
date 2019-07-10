package com.example.jtxyz

import java.net.URI

interface SiteMapReporter {
    fun reportLinks(page: URI, links: List<URI>)
    fun reportError(page: URI, error: Throwable)
    fun reportProgress(pagesProcessed: Int, frontierSize: Int)
    fun reportEnd(complete: Boolean, pagesCrawled: Int)
}

class ConsoleSiteMapReporter : SiteMapReporter {
    override fun reportLinks(page: URI, links: List<URI>) =
        links.forEach { println("$page  -> $it") }

    override fun reportError(page: URI, error: Throwable) =
        println("$page -> ${error.message ?: error}")

    override fun reportProgress(pagesProcessed: Int, frontierSize: Int)  =
        System.err.println("Total pages visited: $pagesProcessed. Frontier size currently: $frontierSize")

    override fun reportEnd(complete: Boolean, pagesCrawled: Int) =
        System.err.println("Total pages visited: $pagesCrawled. Crawled all discovered links: $complete")
}
