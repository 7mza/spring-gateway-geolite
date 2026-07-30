import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.reporting.ReportGenerator.Format

plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.spring") version "2.4.10" apply false
    id("org.springframework.boot") version "4.1.0" apply false
    id("io.spring.dependency-management") version "1.1.7"
    id("io.github.ben-manes.versions") version "0.56.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("org.owasp.dependencycheck") version "12.2.2"
    jacoco
    id("java-library")
    id("org.jreleaser") version "1.25.0" apply false
}

allprojects {
    plugins.apply("org.jetbrains.kotlin.jvm")
    plugins.apply("io.spring.dependency-management")
    plugins.apply("io.github.ben-manes.versions")
    plugins.apply("org.jlleitschuh.gradle.ktlint")
    plugins.apply("org.owasp.dependencycheck")

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    plugins.apply("jacoco")

    val mockitoCoreVersion = "5.23.0"
    val mockitoKotlinVersion = "6.3.0"

    val mockitoAgent = configurations.create("mockitoAgent")

    dependencies {
        testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
        testImplementation("org.mockito.kotlin:mockito-kotlin:$mockitoKotlinVersion")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")

        mockitoAgent("org.mockito:mockito-core:$mockitoCoreVersion") { isTransitive = false }
    }

    java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

    kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

    tasks {
        withType<JavaCompile>().configureEach {
            options.encoding = Charsets.UTF_8.name()
            options.isFork = true
        }

        withType<Test>().configureEach {
            useJUnitPlatform()
            jvmArgumentProviders += CommandLineArgumentProvider { listOf("-javaagent:${mockitoAgent.asPath}") }
            maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
            forkEvery = 0
            jvmArgs("-XX:+EnableDynamicAgentLoading")
            if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_13)) {
                jvmArgs("-XX:+AllowRedefinitionToAddDeleteMethods")
            }
            finalizedBy(jacocoTestReport)
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

        jacocoTestReport {
            dependsOn(test)
            classDirectories.setFrom(
                classDirectories.files.map { fileTree(it) { exclude("**/ApplicationKt.class") } },
            )
            reports {
                csv.required = true
                html.outputLocation = layout.buildDirectory.dir("jacocoHtml")
                xml.required = false
            }
        }
    }

    configure<KtlintExtension> {
        android.set(false)
        coloredOutput.set(true)
        debug.set(true)
        verbose.set(true)
        version.set("1.8.0")
    }

    configure<DependencyCheckExtension> { format = Format.HTML.toString() }
}

allprojects { dependencyCheck { nvd.apiKey = System.getenv("NVD_APIKEY") ?: "" } }
