plugins {
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("maven-publish")
}

val artifact = "spring-gateway-geolite"
group = "com.hamza.geolite"
version = "0.0.1-SNAPSHOT"

val geoip2Version = "4.3.1"
val springCloudVersion = "2025.0.0"

dependencies {
    implementation("com.maxmind.geoip2:geoip2:$geoip2Version")

    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    compileOnly("io.micrometer:micrometer-tracing-bridge-otel")
    compileOnly("io.projectreactor.kotlin:reactor-kotlin-extensions")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    compileOnly("org.springframework.boot:spring-boot-starter-actuator")
    compileOnly("org.springframework.cloud:spring-cloud-starter-gateway-server-webflux")
    compileOnly("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

tasks.jar {
    archiveBaseName.set(artifact)
}

tasks.bootJar {
    enabled = false
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "$group"
            artifactId = artifact
            version = "$version"
            from(components["java"])
        }
    }
}
