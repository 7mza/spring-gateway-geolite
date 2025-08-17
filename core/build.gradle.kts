import org.jreleaser.model.Active

plugins {
    kotlin("plugin.spring")
    id("org.springframework.boot")
    id("maven-publish")
    id("org.jreleaser")
}

val artifact = "spring-gateway-geolite"
group = "io.github.7mza"
version = "0.0.2"

val blockhoundVersion = "1.0.13.RELEASE"
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

    testImplementation("io.projectreactor.tools:blockhound-junit-platform:$blockhoundVersion")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
    }
}

tasks.withType<Jar>().configureEach {
    archiveBaseName.set(artifact)
}

tasks.bootJar {
    enabled = false
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "$group"
            artifactId = artifact
            version = "$version"
            from(components["java"])
            pom {
                name = "spring-gateway-geolite"
                description = "Spring Cloud Gateway GeoLite2 filter"
                url = "https://github.com/7mza/spring-gateway-geolite"
                licenses {
                    license {
                        name = "MIT License"
                        url = "https://opensource.org/license/MIT"
                    }
                }
                developers {
                    developer {
                        id = "7mza"
                        name = "Hamza B"
                        email = "alias.ducky891@passinbox.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/7mza/spring-gateway-geolite.git"
                    developerConnection = "scm:git:ssh://github.com/7mza/spring-gateway-geolite.git"
                    url = "https://github.com/7mza/spring-gateway-geolite"
                }
            }
        }
    }

    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }

    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
    }
}

jreleaser {
    project {
        name = artifact
        languages {
            java {
                artifactId = artifact
            }
        }
    }
    gitRootSearch = true
    strict = true
    signing {
        active = Active.ALWAYS
        armored = true
        verify = true
    }
    deploy {
        maven {
            mavenCentral {
                register("release-deploy") {
                    active = Active.RELEASE
                    url = "https://central.sonatype.com/api/v1/publisher"
                    applyMavenCentralRules = true
                    stagingRepository("build/staging-deploy")
                }
            }
            nexus2 {
                register("snapshot-deploy") {
                    active = Active.SNAPSHOT
                    snapshotUrl = "https://central.sonatype.com/repository/maven-snapshots/"
                    applyMavenCentralRules = true
                    snapshotSupported = true
                    closeRepository = true
                    releaseRepository = true
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}

tasks.matching { it.name.startsWith("publish", ignoreCase = true) }.configureEach {
    dependsOn(project(":scg-webflux-test").tasks.named("check"))
}
