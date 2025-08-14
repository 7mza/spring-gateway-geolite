# Spring Cloud Gateway GeoLite2

SGC filter to automatically transform "X-Forwarded-For" header to GeoIP2 data and add it to MDC/tracing baggage
using [MaxMind local GeoLite2 dbs](https://github.com/P3TERX/GeoLite.mmdb)

### data model

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

this model will be accessible to log encoder and can be transformed by collector

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

because of how free GeoLite databases are distributed, this filter require increased Xms/Xmx reservation to prevent
OOM

### archi

* core: implementation(file readers, GeoLite service, filters)
* scg-*-test: integration tests on a real SCG / wiremock

### reqs

* spring boot 3.5.+
* spring cloud 2015.+

### build

[sdkman](https://sdkman.io)

* jdk 17 / kotlin 1.9.x for broader support

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
    implementation("io.github.7mza:spring-gateway-geolite:0.0.1-SNAPSHOT")
}
```

### TODO

- SGC webmvc
- cache auto plug
- mvn publish