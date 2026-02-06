import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.reporting.ReportGenerator.Format

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.spring") version "2.3.10" apply false
    id("org.springframework.boot") version "4.0.2" apply false
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.ben-manes.versions") version "0.53.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    id("org.owasp.dependencycheck") version "12.2.0"
    jacoco
    id("java-library")
    id("org.jreleaser") version "1.22.0" apply false
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.owasp.dependencycheck")

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "jacoco")

    val mockitoCoreVersion = "5.21.0"
    val mockitoKotlinVersion = "6.2.3"

    val mockitoAgent = configurations.create("mockitoAgent")

    dependencies {
        annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")

        mockitoAgent("org.mockito:mockito-core:$mockitoCoreVersion") { isTransitive = false }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
    }

    kotlin {
        compilerOptions {
            freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.isFork = true
        options.isIncremental = true
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs(
            "--enable-native-access=ALL-UNNAMED",
            "-javaagent:${mockitoAgent.asPath}",
            "-XX:+EnableDynamicAgentLoading",
        )
        // maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(2)
        maxParallelForks = 2
        // forkEvery = 100
        if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_13)) {
            jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
        }
        finalizedBy(tasks.jacocoTestReport)
        configure<JacocoTaskExtension> {
            excludes = listOf("jdk.internal.*")
            isIncludeNoLocationClasses = true
        }
        testLogging {
            events = setOf(FAILED)
            exceptionFormat = FULL
            showCauses = true
            showExceptions = true
            showStackTraces = true
            showStandardStreams = false
        }
        reports {
            html.required = false
            junitXml.required = false
        }
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        classDirectories.setFrom(
            files(
                classDirectories.files.map {
                    fileTree(it) {
                        exclude("**/ApplicationKt.class")
                    }
                },
            ),
        )
        reports {
            csv.required = false
            html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
            xml.required = false
        }
    }

    configure<KtlintExtension> {
        android.set(false)
        coloredOutput.set(true)
        debug.set(true)
        verbose.set(true)
        version.set("1.8.0")
    }

    configure<DependencyCheckExtension> {
        format = Format.HTML.toString()
    }
}

allprojects {
    // https://nvd.nist.gov/developers/request-an-api-key
    dependencyCheck {
        nvd.apiKey = System.getenv("NVD_APIKEY") ?: ""
    }
}
