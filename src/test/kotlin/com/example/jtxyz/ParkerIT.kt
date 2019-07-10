package com.example.jtxyz

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.ServerSocket
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

class TestReporter : SiteMapReporter {
    val links = mutableMapOf<URI, List<URI>>()
    val errors = mutableMapOf<URI, String>()
    var pagesCrawled: Int? = null
    var complete: Boolean? = null

    override fun reportLinks(page: URI, links: List<URI>) {
        this.links[page] = links
    }

    override fun reportError(page: URI, error: Throwable) {
        this.errors[page] = error.message.toString()
    }

    override fun reportProgress(pagesProcessed: Int, frontierSize: Int) {
        // Do nothing
    }

    override fun reportEnd(complete: Boolean, pagesCrawled: Int) {
        this.complete = complete
        this.pagesCrawled = pagesCrawled
    }

}

@ObsoleteCoroutinesApi
class ParkerIT {
    private lateinit var reporter: TestReporter

    @Before
    fun setup() {
        reporter = TestReporter()
    }

    @Test
    fun `should fetch the page, extract the links and report using the original url`() {
        Parker(URI("http://localhost:$port"), 2, reporter).crawl(3)

        assertEquals(
            mutableMapOf(URI("http://localhost:$port") to listOf(URI("http://localhost:$port/home"))),
            reporter.links
        )

        assertEquals(
            mutableMapOf(URI("http://localhost:$port/home") to "Not Found"),
            reporter.errors
        )

        assertEquals(2, reporter.pagesCrawled)
        assertEquals(true, reporter.complete)
    }

    @Test
    fun `should retry but eventually fail`() {
        val out = ByteArrayOutputStream()

        Parker(
            URI("http://localhost:$port/too-many"),
            2,
            reporter,
            KtorPageFetcher(RetryConfig(5, 250, PrintStream(out)))
        ).crawl(5)

        assertEquals(
            mutableMapOf(),
            reporter.links
        )

        assertEquals(
            mutableMapOf(URI("http://localhost:$port/too-many") to "Too Many Requests"),
            reporter.errors
        )

        assertEquals(1, reporter.pagesCrawled)
        assertEquals(true, reporter.complete)

        assertEquals(
            (0..3).map { "java.io.IOException: Too Many Requests: Sleeping and retrying" }.toList(),
            out.toString(Charsets.UTF_8.name()).trim().lines()
        )
    }

    @Test
    fun `should retry and eventually succeed`() {
        val out = ByteArrayOutputStream()

        Parker(
            URI("http://localhost:$port/after-three"),
            2,
            reporter,
            KtorPageFetcher(RetryConfig(5, 250, PrintStream(out)))
        ).crawl(1)

        assertEquals(
            mutableMapOf(URI("http://localhost:$port/after-three") to listOf(URI("http://localhost:$port/"))),
            reporter.links
        )

        assertEquals(
            mutableMapOf(),
            reporter.errors
        )

        assertEquals(1, reporter.pagesCrawled)
        assertEquals(false, reporter.complete)

        assertEquals(
            (0..2).map { "java.io.IOException: Too Many Requests: Sleeping and retrying" }.toList(),
            out.toString(Charsets.UTF_8.name()).trim().lines()
        )
    }

    companion object {
        private lateinit var server: NettyApplicationEngine
        private var afterThreeCount: Int = 0
        var port = 0

        @BeforeClass
        @JvmStatic
        fun startServer() {

            port = ServerSocket(0).use { it.localPort }

            afterThreeCount = 0
            server = embeddedServer(Netty, port, "127.0.0.1") {
                routing {
                    get("/") {
                        call.respondText("""<a href="/home">""", ContentType.Text.Html)
                    }

                    get("/too-many") {
                        call.respond(HttpStatusCode.TooManyRequests)
                    }

                    get("/after-three") {
                        when (afterThreeCount) {
                            in 0..2 -> call.respond(HttpStatusCode.TooManyRequests)
                            else -> call.respondText("""<a href="/">""", ContentType.Text.Html)
                        }
                        afterThreeCount = (afterThreeCount + 1) % 4
                    }
                }
            }
            server.start()
        }

        @AfterClass
        @JvmStatic
        fun stopServer() {
            server.stop(1, 2, TimeUnit.SECONDS)
        }

    }
}