plugins {
    kotlin("plugin.spring")
    id("org.springframework.boot")
}

group = "io.github.7mza.geolite.tests"
version = "0.0.1-SNAPSHOT"

val blockhoundVersion = "1.0.15.RELEASE"
val geoip2Version = "5.0.0"
val springCloudVersion = "2025.1.0"
val wiremockSpringBootVersion = "4.0.8"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    implementation(project(":core"))

    testImplementation("com.maxmind.geoip2:geoip2:$geoip2Version")
    testImplementation("io.projectreactor.tools:blockhound-junit-platform:$blockhoundVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-opentelemetry-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webclient-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.wiremock.integrations:wiremock-spring-boot:$wiremockSpringBootVersion")
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
