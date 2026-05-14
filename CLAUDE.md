# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`spring-gateway-geolite` is a Spring Boot autoconfiguration library published to Maven Central (`io.github.7mza:spring-gateway-geolite`). It provides a Spring Cloud Gateway WebFlux filter (`ReactiveGeoLite`) that enriches requests with GeoIP2 data and basic bot scoring, writing the result as Micrometer/OTel baggage and MDC.

- **v2.x** targets Spring Boot 4.x / Spring Cloud 2025.1.x
- **v1.x** targets Spring Boot 3.x / Spring Cloud 2025.0.0

## Build commands

Requires JDK 21 (via [sdkman](https://sdkman.io)):

```shell
sdk env install
```

Download GeoLite2 `.mmdb` files before running tests (integration tests require real DB files):

```shell
./download_geolite.sh
```

Standard build (lint + test):

```shell
./gradlew clean ktlintFormat ktlintCheck build
```

Run a single test class:

```shell
./gradlew :scg-webflux-test:test --tests "io.github.hamza.geolite.webflux.ReactiveGeoLiteServiceTest"
```

Publish to local Maven repo (for manual integration testing):

```shell
./gradlew publishToMavenLocal
```

Format JSON/YAML/Markdown files (uses prettier via npm):

```shell
npm run format
```

Check for outdated dependencies:

```shell
./gradlew dependencyUpdates
# OWASP CVE check (requires NVD_APIKEY env var):
./gradlew dependencyCheckAnalyze
```

### Publishing to Maven Central

```shell
sdk env install
./gradlew clean ktlintFormat ktlintCheck build --no-build-cache

./gradlew jreleaserConfig   # verify signing/deploy config
./gradlew jreleaserSign     # sign artifacts
./gradlew publish           # stage artifacts to build/staging-deploy
./gradlew jreleaserDeploy   # push to Maven Central
```

## Architecture

### Module layout

| Module             | Role                                                        |
| ------------------ | ----------------------------------------------------------- |
| `core`             | Library source — autoconfiguration, filter, service, models |
| `scg-webflux-test` | Integration test app — real SCG + WireMock, not published   |

`core/src/main/kotlin/…/webmvc/` contains stub interfaces (`IImperativeFileReader`, `IImperativeGeoLiteService`) that are **not yet implemented** — webmvc support is a TODO.

### Autoconfiguration

`core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` registers two configs:

- **`GeoliteSharedConfiguration`** — always active; provides `GeoliteProperties` (`@ConfigurationProperties(prefix = "geolite")`), a dedicated `ObjectMapper` bean (`GeoLiteObjectMapper`), and an `XForwardedRemoteAddressResolver` bean (`GeoLiteForwardedResolver`). Both beans use `defaultCandidate = false` to avoid displacing user-defined beans of the same type.
- **`GeoliteConfiguration`** — conditional on `GatewayAutoConfiguration` and `Tracer` being on the classpath; wires `ReactiveFileReader`, `ReactiveGeoLiteService`, and `ReactiveGeoLiteGatewayFilterFactory` (registered as `ReactiveGeoLite` in SCG routes).

`GeoliteProperties` is `@RefreshScope` — properties reload on Spring Cloud Config refresh without restart.

### Request flow

1. `ReactiveGeoLiteGatewayFilterFactory.apply()` resolves the real client IP via `XForwardedRemoteAddressResolver` (respects `maxTrustedIndex`).
2. Calls `ReactiveGeoLiteService.city()` and `.asn()` concurrently (`Mono.zip`).
3. Builds a `GeoLiteData` object; bot score is computed from missing geo fields and absent `user-agent` header.
4. Runs `Commons.excludedFields()` to strip configured `exclude` paths (dot-notation, supports `field.*`).
5. Serializes to JSON; writes it as Micrometer baggage via `tracer.createBaggageInScope()` and logs at the active log level.
6. If `blockBot=true` and score ≥ `botScoreThreshold`, returns HTTP 429 (unless the `tap-test` Spring profile is active, in which case the chain continues so tests can assert the baggage value before the 429).

### Database loading

`ReactiveGeoLiteService` holds three `Mono<DatabaseReader>` fields (city, country, ASN). Each is read via `ReactiveFileReader` (Spring `ResourceLoader` → classpath path) on `Schedulers.boundedElastic()`. When `geolite.cached=true`, each Mono is wrapped with `.cache()` so the files are read once. On `@PreDestroy` the readers are closed.

All MaxMind `DatabaseReader` calls are blocking and must stay on `boundedElastic` — BlockHound tests in both modules enforce this.

### Bot scoring (WORK IN PROGRESS)

`GeoLiteData.getBotScore()` sums:

- missing city fields (name, isoCode, lat, lon) → up to 4 pts
- missing country fields (name, isoCode) → up to 2 pts
- missing ASN fields (5 fields) → up to 5 pts
- missing `user-agent` header → 1 pt (other header checks are commented out)

`isBot = score >= botScoreThreshold` (default 12).

### Integration tests

Tests in `scg-webflux-test` boot a real SCG and use WireMock Spring Boot as the upstream. There is no mocking of the GeoLite service; real `.mmdb` files in `scg-webflux-test/src/main/resources/geolite/` are required.

`publish*` Gradle tasks depend on `scg-webflux-test:check` — publishing is blocked if integration tests fail.

### Key configuration properties

```yaml
geolite:
  baggage: visitor_info # MDC field / OTel baggage key
  blockBot: false
  botScoreThreshold: 12
  cached: true # wrap DB Monos with .cache()
  db:
    asn: geolite/GeoLite2-ASN.mmdb
    city: geolite/GeoLite2-City.mmdb
    country: geolite/GeoLite2-Country.mmdb
  exclude: [] # dot-notation paths, e.g. asn.ipAddress or asn.*
  maxTrustedIndex: 1
```

Consumer apps need `-Xms1g -Xmx1536m` JVM flags due to MaxMind DB memory requirements.
