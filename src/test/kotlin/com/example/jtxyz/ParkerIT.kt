package com.example.jtxyz

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.uri
import io.ktor.response.respondRedirect
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.ServerSocket
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@ObsoleteCoroutinesApi
class ParkerIT {
    private val out = ByteArrayOutputStream()

    @Test
    fun `should use the redirected URL when resolving links`() = runBlocking {
        val host = "http://localhost:$port"
        crawl(URI("$host/"), 0, out = PrintStream(out)).join()

        assertEquals(
            """
            |$host/ -> $host/blog
            |$host/blog -> $host/blog/january
            |""".trimMargin(),
            out.toString(Charsets.UTF_8.name())
        )
    }

    companion object {
        private lateinit var server: NettyApplicationEngine
        var port = 0

        @BeforeClass
        @JvmStatic
        fun startServer() {
            port = ServerSocket(0).use { it.localPort } // find a random available port

            server = embeddedServer(Netty, port, "127.0.0.1") {
                routing {
                    get("/") {
                        call.respondText("""<a href="blog">""", ContentType.Text.Html)
                    }
                    get("/blog/") {
                        when {
                            call.request.uri.endsWith("/") ->
                                call.respondText("""<a href="january">""", ContentType.Text.Html)
                            else -> call.respondRedirect("/blog/")
                        }

                    }
                    get("/blog/*") {
                        call.respondText("""""", ContentType.Text.Html)
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