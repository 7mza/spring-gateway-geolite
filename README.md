# spring gateway geolite

spring cloud gateway filter to extract geoip2 data from x-forwarded-for and add them to mdc/tracing context

using [local geolite dbs](https://github.com/P3TERX/GeoLite.mmdb)

autoconfigured with conditions on

* org.springframework.cloud:spring-cloud-starter-gateway-server-webflux
* io.micrometer:micrometer-tracing-bridge-otel

why ? -> https://docs.spring.io/spring-boot/reference/actuator/tracing.html#actuator.micrometer-tracing.baggage

configure :

```yaml
geoip2:
  db:
    asn: geoip2/GeoLite2-ASN.mmdb # whre
    city: geoip2/GeoLite2-City.mmdb
    country: geoip2/GeoLite2-Country.mmdb
  loadOnStartUp: false
  mdcKey: visitor_infos
  maxTrustedIndex: 1
management:
  tracing:
    baggage:
      correlation:
        fields:
          - ${geoip2.mdcKey}
      remote-fields:
        - ${geoip2.mdcKey}
```

then appy filter on your routes

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
                - GeoIP2
```

build

```yaml
./gradlew clean ktlintFormat ktlintCheck build publishToMavenLocal
```

in gateway pom

```kotlin
repositories {
    // ...
    mavenLocal()
}

implementation("com.hamza.geolite:spring-gateway-geolite:0.0.1-SNAPSHOT")
```

### TODO

- webmvc
- mvn publish