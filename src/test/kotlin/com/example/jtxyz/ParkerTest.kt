package com.example.jtxyz

import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.net.URI

class ParkerTest {
    private lateinit var pageFetcher: PageFetcher
    private lateinit var linkExtractor: LinkExtractor
    private lateinit var siteMapReporter: SiteMapReporter
    private lateinit var seedUri: URI
    private lateinit var subject: Parker

    @Before
    fun setup() {
        linkExtractor = mock(LinkExtractor::class.java)
        pageFetcher = mock(PageFetcher::class.java)
        siteMapReporter = mock(SiteMapReporter::class.java)
        seedUri = URI("https://www.example.com")
        subject = Parker(
            seedUri,
            siteMapReporter,
            pageFetcher,
            linkExtractor
        )
    }

    @Test
    fun `should fetch the page, extract the links and report using the original url`() {
        val page = Page(URI("https://www.example.com/"), "root-page")
        `when`(pageFetcher.fetch(seedUri)).thenReturn(page)
        `when`(linkExtractor.extract(page)).thenReturn(listOf())

        subject.crawl(5)

        verify(siteMapReporter).reportLinks(URI("https://www.example.com"), listOf())
        verify(siteMapReporter).reportEnd(true, 1)
    }

    @Test
    fun `should handle circular references`() {
        val seedPage = Page(seedUri, "a")
        val pageB = Page(URI("https://www.example.com/b"), "a")

        `when`(pageFetcher.fetch(seedUri)).thenReturn(seedPage)
        `when`(pageFetcher.fetch(pageB.uri)).thenReturn(pageB)

        `when`(linkExtractor.extract(seedPage)).thenReturn(listOf(pageB.uri))
        `when`(linkExtractor.extract(pageB)).thenReturn(listOf(seedPage.uri))

        subject.crawl(3)

        verify(siteMapReporter).reportLinks(seedUri, listOf(pageB.uri))
        verify(siteMapReporter).reportLinks(pageB.uri, listOf(seedPage.uri))
        verify(siteMapReporter).reportEnd(true, 2)
        verify(siteMapReporter).reportProgress(1, 1)
        verify(siteMapReporter).reportProgress(2, 0)
        verifyNoMoreInteractions(siteMapReporter)
    }
}
