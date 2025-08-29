# Spring Cloud Gateway GeoLite2

SCG filter to automatically transform "X-Forwarded-For" header to GeoIP2 data and add it to MDC/tracing baggage
using [MaxMind's local GeoLite2 dbs](https://github.com/P3TERX/GeoLite.mmdb)

[data model](./core/src/main/kotlin/io/github/hamza/geolite/Models.kt)

```json
{
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
  }
}
```

this model will be accessible to log encoders in MDC & can be transformed by log collectors

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

* spring-cloud-starter-gateway-server-webflux
* io.micrometer:micrometer-tracing-bridge-otel
* actuator

micrometer is needed to [pass baggage to
MDC](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#actuator.micrometer-tracing.baggage)

download latest GeoLite dbs from [P3TERX/GeoLite.mmdb](https://github.com/P3TERX/GeoLite.mmdb)

### configure on SCG

```yaml
geolite:
  baggage: # MDC field / baggage name
  db:
    asn: # spring ResourceLoader relative path to db file
    city: # example: geolite/GeoLite2-City.mmdb
    country: # ...
  exclude: # fields to exclude
  # - asn.ipAddress
  # or - asn.*
  maxTrustedIndex: 1
management:
  tracing:
    baggage:
      correlation:
        fields:
          - ${geolite.baggage}
#      remote-fields:
#        - ${geolite.baggage}
logging:
  level:
    io.github.hamza.geolite: # LEVEL
```

true non-proxy "X-Forwarded-For" is resolved
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
            - id: stub
              uri: http://localhost:${wiremock.server.port}
              predicates:
                - Path=/stub/**
              filters:
                - ReactiveGeoLite
#                OR  (if u need to append additional request headers)
#                - name: ReactiveGeoLite
#                  args:
#                    additionalHeaders:
#                      - user-agent
```

#### webmvc

```yml
# TODO
```

**because of how free GeoLite databases are distributed, this filter require increased Xms/Xmx reservation to prevent
OOM**

### support

* spring boot 3.4+.* / spring cloud 2025.*

### archi

* core: implementation
* scg-*-test: integration tests on a real SCG / wiremock

### local build

[sdkman](https://sdkman.io)

* jdk 17 for broader support

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