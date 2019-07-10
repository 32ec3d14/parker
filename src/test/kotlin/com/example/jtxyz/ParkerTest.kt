package com.example.jtxyz

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ParkerTest {
    private lateinit var subject: Parker

    @Before
    fun setup() {
        subject = Parker("http://www.example.com")
    }

    @Test
    fun `should provide the correct message`() {
        assertEquals("Crawling http://www.example.com", subject.crawl())
    }
}
