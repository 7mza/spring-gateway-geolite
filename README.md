# Spring Cloud Gateway GeoLite2

SCG auto configured filter for GeoLite2 integration and bot detection

- automatically transforms X-Forwarded-For header to GeoIP2 data and add it to MDC/tracing baggage
  using [MaxMind's local GeoLite2 dbs](https://github.com/P3TERX/GeoLite.mmdb)
- basic bot scoring and detection (WIP)
- reject request if bot score threshold is reached

[data model](./core/src/main/kotlin/io/github/hamza/geolite/Models.kt)

```json
{
  "xForwardedFor": "128.101.101.101",
  "path": "/stub",
  "query": "toto=true&tata=123",
  "city": {
    "name": "Minneapolis",
    "isoCode": "MN",
    "latitude": 44.9696,
    "longitude": -93.2348
  },
  "country": {
    "name": "United States",
    "isoCode": "US"
  },
  "asn": {
    "autonomousSystemNumber": 217,
    "autonomousSystemOrganization": "UMN-SYSTEM",
    "ipAddress": "128.101.101.101",
    "hostAddress": "128.101.0.0",
    "prefixLength": 16
  },
  "additionalHeaders": {
    "user-agent": [
      "..."
    ]
  },
  "botScore": 10,
  "isBot": false
}
```

this model will be accessible to log encoders in MDC & can be prepared by log collectors

## usage

```kotlin
implementation("io.github.7mza:spring-gateway-geolite:$latest")
```

```xml

<dependency>
    <groupId>io.github.7mza</groupId>
    <artifactId>spring-gateway-geolite</artifactId>
    <version>$latest</version>
</dependency>
```

autoconfiguration conditional on

- spring-cloud-starter-gateway-server-webflux
- io.micrometer:micrometer-tracing-bridge-otel
- actuator

micrometer is needed to [pass baggage to
MDC](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#actuator.micrometer-tracing.baggage)

download latest GeoLite dbs from [P3TERX/GeoLite.mmdb](https://github.com/P3TERX/GeoLite.mmdb)

### configure on SCG

```yaml
geolite:
  baggage: visitor_info # MDC field / baggage name
  blockBot: false # block request if bot score threshold is reached, return 429
  botScoreThreshold: 12 # bot score detection threshold
  cached: true # enable reactor cache over database files
  db:
    # spring ResourceLoader relative path to db file
    asn: geolite/GeoLite2-ASN.mmdb
    city: geolite/GeoLite2-City.mmdb
    country: geolite/GeoLite2-Country.mmdb
  exclude: [] # fields to exclude from MDC
#    - asn.ipAddress # or
#    - asn.* # or
  maxTrustedIndex: 1
management:
  tracing:
    baggage:
      correlation:
        fields:
          - ${geolite.baggage}
#        remote-fields: # to forward in headers
#          - ${geolite.baggage}
logging:
  level:
    io.github.hamza.geolite: WARN # log level you are using to send logs to collector
```

true non-proxy X-Forwarded-For is resolved
using [maxTrustedIndex](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/request-predicates-factories.html#modifying-the-way-remote-addresses-are-resolved)

### then apply as any other filter on your routes

#### webflux

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
            # basic usage
            - id: id1
              uri: uri1
              predicates:
                - ...
              filters:
                - ReactiveGeoLite

            # append specific headers
            - id: id2
              uri: uri2
              predicates:
                - ...
              filters:
                - ReactiveGeoLite
                - name: ReactiveGeoLite
                  args:
                    additionalHeaders:
                      - user-agent

            # append all headers (useful to study bots behavior)
            - id: id3
              uri: uri3
              predicates:
                - ...
              filters:
                - ReactiveGeoLite
                - name: ReactiveGeoLite
                  args:
                    additionalHeaders:
                      - "*"
```

#### webmvc

```yml
# TODO
```

**because of how free GeoLite databases are distributed, this filter require increased Xms/Xmx reservation to prevent
OOM**

`-Xms1g -Xmx1536m` is recommend but you should test according to your traffic

### support

- spring boot 3.4+ / spring cloud 2025

### archi

- core: implementation
- scg-\*-test: integration tests on a real SCG / wiremock

### local build

[sdkman](https://sdkman.io)

- jdk 21 for broader support

```shell
sdk env install
```

```yaml
./gradlew clean ktlintFormat ktlintCheck build
```

```yaml
./gradlew publishToMavenLocal
```

in your SCG pom/build

```kotlin
repositories {
    // ...
    mavenLocal()
}

dependencies {
    // ...
    implementation("io.github.7mza:spring-gateway-geolite:${local_build_version}")
}
```

### TODO

- SCG webmvc
- cache auto plug
