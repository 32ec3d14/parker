package com.example.jtxyz

import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.stubbing.Answer
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.RuntimeException
import java.net.URI
import kotlin.test.assertEquals

private inline fun <reified T> mock(answer: Answer<Any>) = mock(T::class.java, answer)

@ObsoleteCoroutinesApi
class ParkerTest {
    private val uris = ('a'..'e').map { URI(it.toString()) }
    private fun toPage(uri: URI) = Page(URI("$uri/"), "$uri content")

    private val pageFetcher: suspend (URI) -> Page = { toPage(it) }
    private val linkExtractor = mock<(Page) -> List<URI>>(Answer { emptyList<URI>() })
    private val out = ByteArrayOutputStream()

    private val firstUri = uris.first()
    private val firstPage = toPage(firstUri)
    private val secondPage = toPage(uris[1])
    private val thirdPage = toPage(uris[2])

    private fun run(rateLimit: Long = 0) =
        crawl(uris.first(), rateLimit, pageFetcher, linkExtractor, PrintStream(out))

    private fun sortedOutput() = out
        .toString(Charsets.UTF_8.name())
        .trim()
        .split("\n")
        .sorted()
        .joinToString("\n", postfix = "\n")

    @Test
    fun `should fetch the page and extract the links from it`() = runBlocking {
        run().join()

        verify(linkExtractor, times(1)).invoke(firstPage)
    }.let { }

    @Test
    fun `should print the links`() = runBlocking {
        `when`(linkExtractor.invoke(firstPage)).thenReturn(uris.drop(1))

        run().join()

        assertEquals(
            """
            |a -> b
            |a -> c
            |a -> d
            |a -> e
            |""".trimMargin(),
            out.toString(Charsets.UTF_8.name())
        )
    }

    @Test
    fun `should fetch the linked pages`() = runBlocking {
        `when`(linkExtractor.invoke(firstPage)).thenReturn(uris.drop(1))

        run().join()

        uris.map(::toPage).forEach { verify(linkExtractor, times(1)).invoke(it) }
    }

    @Test
    fun `should display but not fetch duplicates`() = runBlocking {
        `when`(linkExtractor.invoke(firstPage)).thenReturn(uris.drop(1))
        `when`(linkExtractor.invoke(secondPage)).thenReturn(uris.drop(2))

        run().join()

        assertEquals(
            """
            |a -> b
            |a -> c
            |a -> d
            |a -> e
            |b -> c
            |b -> d
            |b -> e
            |""".trimMargin(),
            out.toString(Charsets.UTF_8.name())
        )

        uris.map(::toPage).forEach { verify(linkExtractor, times(1)).invoke(it) }
    }

    @Test
    fun `should display fragments but strip them before de-duplicating`() = runBlocking {
        `when`(linkExtractor.invoke(firstPage)).thenReturn(listOf("a#x", "a#y").map(::URI))

        run().join()

        assertEquals(
            """
            |a -> a#x
            |a -> a#y
            |""".trimMargin(),
            out.toString(Charsets.UTF_8.name())
        )

        verify(linkExtractor, times(1)).invoke(firstPage)
        verifyNoMoreInteractions(linkExtractor)
    }

    @Test
    fun `should display but not fetch pdfs`() = runBlocking {
        `when`(linkExtractor.invoke(firstPage)).thenReturn(listOf("x.pdf", "y.pdf").map(::URI))

        run().join()

        assertEquals(
            """
            |a -> x.pdf
            |a -> y.pdf
            |""".trimMargin(),
            out.toString(Charsets.UTF_8.name())
        )

        verify(linkExtractor, times(1)).invoke(firstPage)
        verifyNoMoreInteractions(linkExtractor)
    }

    @Test
    fun `should display errors processing a page`() = runBlocking {
        `when`(linkExtractor.invoke(firstPage)).thenReturn(uris.drop(1))
        `when`(linkExtractor.invoke(secondPage)).thenThrow(RuntimeException("Example error"))
        `when`(linkExtractor.invoke(thirdPage)).thenReturn(uris.take(2))

        run().join()

        assertEquals(
            """
            |a -> b
            |a -> c
            |a -> d
            |a -> e
            |b -> Example error
            |c -> a
            |c -> b
            |""".trimMargin(),
            sortedOutput() // the output is nondeterministic because of the parallel requests
        )

        uris.map(::toPage).forEach { verify(linkExtractor, times(1)).invoke(it) }
    }

    @Test
    fun `should limit requests per second`() = runBlocking {
        `when`(linkExtractor.invoke(firstPage)).thenReturn(uris.drop(1))
        uris.drop(1).map(::toPage)
            .forEach { `when`(linkExtractor.invoke(it)).thenReturn(listOf(firstUri)) }

        val job = run(50)
        delay(125)
        job.cancel()

        val visitedPages = out.toString(Charsets.UTF_8.name())
            .trim()
            .split("\n")
            .map { it[0] }
            .distinct()

        assertEquals(3, visitedPages.size, "should only have time to make 3 requests")
    }
}
