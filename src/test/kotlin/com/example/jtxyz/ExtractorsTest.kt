package com.example.jtxyz

import org.junit.Before
import org.junit.Test
import java.net.URI
import kotlin.test.assertEquals


class ExtractorsTest {
    private lateinit var subject: RegexLinkExtractor
    @Before
    fun setup() {
        subject = RegexLinkExtractor()
    }

    @Test
    fun `should return the links from the page`() {
        assertEquals(
            listOf(URI("https://example.com/about")),
            subject.extract(
                Page(
                    URI("https://example.com/home"),
                    """<body><p>hrefs are fun</p><a href="/about">About me</a></body>"""
                )
            )
        )
    }


    @Test
    fun `should ignore links that don't parse as URIs`() {
        assertEquals(
            listOf(),
            subject.extract(
                Page(
                    URI("https://example.com/home"),
                    """<a href="javascript:(function(){alert('Hello!');})()">About me</a>"""
                )
            )
        )

    }

    @Test
    fun `should ignore links that with schemes other than http or https`() {
        assertEquals(
            listOf(),
            subject.extract(
                Page(
                    URI("https://example.com/home/"),
                    """
                    <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>
                    <a href="mailto:santa@example.com">Electronic Mail</a>
                    <a href="tel:311-555-2368">Telephone</a>
                    """
                )
            )
        )
    }

    @Test
    fun `should resolve urls relative to the request url`() {
        assertEquals(
            listOf(
                URI("https://example.com/home/more"),
                URI("https://example.com/about"),
                URI("http://example.com/cats")
            ),
            subject.extract(
                Page(
                    URI("https://example.com/home/"),
                    """
                    <a href="more">More info</a>
                    <a href="/about">About</a>
                    <a href="http://example.com/cats">Cats</a>
                    """
                )
            )
        )
    }
}
