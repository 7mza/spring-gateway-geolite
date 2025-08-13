# Spring Cloud Gateway GeoLite

SGC filter to automatically transform "X-Forwarded-For" header to GeoIP data and add it to MDC/tracing baggage
using [local GeoLite dbs](https://github.com/P3TERX/GeoLite.mmdb)

autoconfiguration require

* org.springframework.cloud:spring-cloud-starter-gateway-server-webflux
* io.micrometer:micrometer-tracing-bridge-otel

micrometer is needed to [pass baggage to
MDC](https://docs.spring.io/spring-boot/reference/actuator/tracing.html#actuator.micrometer-tracing.baggage)

### configure on SCG

```yaml
geolite:
  db:
    asn: # spring ResourceLoader relative path to db file
    city: # example: geolite/GeoLite2-City.mmdb
    country: # ...
  baggage: # MDC or baggage name
  maxTrustedIndex: 1
management:
  tracing:
    baggage:
      correlation:
        fields:
          - ${geolite.baggage}
      remote-fields:
        - ${geolite.baggage}
```

true "X-Forwarded-For" is resolved
using [maxTrustedIndex](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/request-predicates-factories.html#modifying-the-way-remote-addresses-are-resolved)

### then apply as any other filter on your routes

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
                - GeoLite
```

because of how free GeoLite databases are distributed, this filter require increased Xms/Xmx reservation to prevent
OOM

### archi

* core: implementation(file readers, GeoLite service, filters)
* scg-webflux-test: integration tests on a real webflux SCG edging wiremock

### build

reqs [jdk 24](https://sdkman.io)

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
    implementation("com.hamza.geolite:spring-gateway-geolite:0.0.1-SNAPSHOT")
}
```

### TODO

- SGC webmvc
- mvn publish