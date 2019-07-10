package com.example.jtxyz

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.MetricRegistry
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

object Metrics {
    private val metrics = MetricRegistry()

    private val requestCount = AtomicInteger()
    private val requestsInFlight = metrics.histogram("request.in-flight")!!
    private val requestTiming = metrics.timer("request.duration")!!

    suspend fun <T> measureRequest(block: suspend () -> T): T {
        requestsInFlight.update(requestCount.incrementAndGet())
        val startTime = System.nanoTime()
        try {
            return block()
        } finally {
            requestTiming.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS)
            requestCount.decrementAndGet()
        }
    }


    val parseTiming = metrics.timer("parse.duration")!!

    fun report() = ConsoleReporter
        .forRegistry(metrics)
        .outputTo(System.err)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build()
        .report()
}