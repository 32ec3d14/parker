package com.example.jtxyz

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.MetricRegistry
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object Metrics {
    private val metrics = MetricRegistry()

    private val requestCount = AtomicInteger()
    private val requestsInFlight = metrics.histogram("request.in-flight")!!
    private val requestsDepth = metrics.histogram("request.depth")!!
    private val requestTiming = metrics.timer("request.duration")!!

    suspend fun <T> measureFetch(depth: Int, block: suspend () -> T): T {
        requestsInFlight.update(requestCount.incrementAndGet())
        requestsDepth.update(depth)
        val startTime = System.nanoTime()
        try {
            return block()
        } finally {
            requestTiming.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
            requestCount.decrementAndGet()
        }
    }

    private val parseTiming = metrics.timer("parse.duration")!!

    fun <T> measureExtract(block: () -> T): T = parseTiming.time<T>(block)

    fun report() = ConsoleReporter
        .forRegistry(metrics)
        .outputTo(System.err)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build()
        .report()
}