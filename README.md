# Parker Web Crawler

Parker is a simple web crawler written in Kotlin. Given a URL, it prints a site
map by following links from that page onwards within the same domain until it
can find no new links.

## Prerequisites

- JDK 8 or newer
- maven

Tested on OS X with JDK 8 and on docker with JDK 11

To start a development environment in docker: `docker run --rm -it -v "$(pwd)":/app -w /app maven:3.6.1-jdk-11 /bin/bash`

## Build and run

```
mvn clean verify
./bin/parker https://www.example.com >results.txt
```

## Usage

```
$ ./bin/parker -h
usage: [-h] [--max-pages MAX_PAGES] [--rate-limit RATE_LIMIT] URL

optional arguments:
  -h, --help                show this help message and exit
  --max-pages MAX_PAGES     the maximum number of pages to crawl before giving up
  --rate-limit RATE_LIMIT   the minimum number of milliseconds between each request

positional arguments:
  URL                       the URL to crawl
```

## Next steps

- [x] Use HTML parsing library rather than regex
- [x] Use more robust HTTP client (with better error handling, charset support, redirects, URL rebuilding, etc.)
- [x] Make concurrent requests, possibly using Kotlin coroutines
- [x] Better arg parsing and validation
- [x] Production-ready features: logging and monitoring
- [ ] Politeness: respect for robots.txt

## Performance

Some rough performance figures using `test-server.js`

```
# 200 pages, average 200ms reponse time
time ./bin/parker http://localhost:9999/random/
real	0m2.204s

# Stops after 500 pages, average response time 100ms
time ./bin/parker http://localhost:9999/
real	0m1.962s
```
