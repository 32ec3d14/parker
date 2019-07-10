package com.example.jtxyz

import java.net.URL

fun main(args: Array<String>) {
    Parker(URL(args.first()).toURI()).crawl(500)
}
