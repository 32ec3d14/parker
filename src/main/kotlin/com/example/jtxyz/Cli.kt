package com.example.jtxyz

import kotlinx.coroutines.ObsoleteCoroutinesApi
import java.net.URL

@ObsoleteCoroutinesApi
fun main(args: Array<String>) {
    Parker(URL(args.first()).toURI()).crawl(500)
}
