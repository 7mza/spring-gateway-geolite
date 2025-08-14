plugins {
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

group = "io.github.7mza.geolite.tests"
version = "0.0.1-SNAPSHOT"

val blockhoundVersion = "1.0.13.RELEASE"
val geoip2Version = "4.3.1"
val springCloudVersion = "2025.0.0"

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation(project(":core"))

    testImplementation("com.maxmind.geoip2:geoip2:$geoip2Version")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
    testImplementation("io.projectreactor.tools:blockhound-junit-platform:$blockhoundVersion")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

tasks.jar {
    enabled = false
}

tasks.bootJar {
    enabled = false
}
