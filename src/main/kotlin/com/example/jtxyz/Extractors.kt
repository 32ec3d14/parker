package com.example.jtxyz

import java.net.URI


interface LinkExtractor {
    fun extract(page: Page): List<URI>
}

class RegexLinkExtractor : LinkExtractor {
    private val linkHrefRegex = """<a href=(?:"|')([^"']+)""".toRegex()

    override fun extract(page: Page) = linkHrefRegex
        .findAll(page.content)
        .mapNotNull { runCatching { URI(it.groupValues[1].trim()) }.getOrNull() }
        .filter { it.scheme == null || it.scheme == "http" || it.scheme == "https" }
        .map(page.uri::resolve)
        .filter { it.host == page.uri.host }
        .toList()
}
