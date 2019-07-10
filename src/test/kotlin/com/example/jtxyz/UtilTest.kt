package com.example.jtxyz

import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class UtilTest {
    @Test
    fun `retry should retry up the max number of attempts`() {
        val out = ByteArrayOutputStream()

        val expectedError = "java.lang.Exception: Error doing task"
        val error = assertFailsWith<Exception> {
            retry(RetryConfig(3, 1, PrintStream(out))) {
                Retry<Any>(Exception("Error doing task"))
            }
        }

        assertEquals(expectedError, error.toString())
        assertEquals(2, out.toString(Charsets.UTF_8.name()).trim().lines().size)
    }
}
