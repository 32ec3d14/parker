package com.example.jtxyz

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.ObsoleteCoroutinesApi
import java.net.URI


class Args(parser: ArgParser) {
    val maxPages by parser.storing("the maximum number of pages to crawl before giving up") { toInt() }
        .default(10000)

    val rateLimit by parser.storing("the minimum number of milliseconds between each request") { toLong() }
        .default(2)

    val url by parser.positional("URL", "the URL to crawl") { URI(this) }
        .addValidator {
            try {
                value.toURL()
            } catch (e: Exception) {
                throw InvalidArgumentException(
                    "Please specify a full URL to index, e.g. https://example.com"
                )
            }
        }
}

@ObsoleteCoroutinesApi
fun main(args: Array<String>) = mainBody {
    ArgParser(args).parseInto(::Args)
        .run {
            Parker(
                url,
                rateLimit,
                ConsoleSiteMapReporter(),
                KtorPageFetcher(RetryConfig()),
                JSoupLinkExtractor()
            )
                .crawl(maxPages)
        }
}