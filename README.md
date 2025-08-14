# Spring Cloud Gateway GeoLite2

SGC filter to automatically transform "X-Forwarded-For" header to GeoIP2 data and add it to MDC/tracing baggage
using [MaxMind's local GeoLite2 dbs](https://github.com/P3TERX/GeoLite.mmdb)

[data model](./core/src/main/kotlin/io/github/hamza/geolite/Models.kt)

```json
{
  "city": {
    "name": "Minneapolis",
    "isoCode": "MN"
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

this model will be accessible to log encoder in MDC & can be transformed by log collectors

example with fluentd

```text
...
<filter $tag.**>
  @type parser
  key_name ${geolite.baggage}
  reserve_data true
  remove_key_name_field true
  emit_invalid_record_to_error false
  <parse>
    @type json
  </parse>
</filter>
...
```

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
* actuator
* io.micrometer:micrometer-tracing-bridge-otel

micrometer is needed to [pass baggage to
MDC](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#actuator.micrometer-tracing.baggage)

download latest GeoLite dbs from [P3TERX/GeoLite.mmdb](https://github.com/P3TERX/GeoLite.mmdb)

### configure on SCG

```yaml
geolite:
  db:
    asn: # spring ResourceLoader relative path to db file
    city: # example: geolite/GeoLite2-City.mmdb
    country: # ...
  baggage: # MDC field / baggage name
  maxTrustedIndex: 1
management:
  tracing:
    baggage:
      correlation:
        fields:
          - ${geolite.baggage}
      remote-fields:
        - ${geolite.baggage}
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
```

#### webmvc

```yml
# TODO
```

**because of how free GeoLite databases are distributed, this filter require increased Xms/Xmx reservation to prevent
OOM**

### support

* spring boot 3.5.* / spring cloud 2025.*
* spring boot 3.4.* / spring cloud 2024.*

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

- SGC webmvc
- cache auto plug