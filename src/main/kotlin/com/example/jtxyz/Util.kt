package com.example.jtxyz

import java.io.PrintStream

sealed class RetryResult<T>
data class Retry<T>(val reason: Exception) : RetryResult<T>()
data class Complete<T>(val value: T) : RetryResult<T>()

data class RetryConfig(
    val maxAttempts: Int = 5,
    val retryInterval: Long = 250,
    val printStream: PrintStream = System.err
)

fun <T> retry(config: RetryConfig, block: () -> RetryResult<T>): T {
    for (i in 1..config.maxAttempts) {
        val result = block()
        if (result is Complete) return result.value

        if (result is Retry) { // compiler can't infer this yet (only in when expressions)
            if (i == config.maxAttempts) {
                throw result.reason
            }

            config.printStream.println("${result.reason}: Sleeping and retrying")
            Thread.sleep(config.retryInterval)
        }
    }

    error("Unreachable code: Tried more than max attempts")
}
