import java.io.ByteArrayOutputStream

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.micronaut.application") version "4.4.4"
    id("io.micronaut.test-resources") version "4.4.4"
    id("io.micronaut.aot") version "4.4.4"
}

fun getCommitHash(): String {
    try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = stdout
            errorOutput = ByteArrayOutputStream()
            isIgnoreExitValue = true
        }
        val hash = stdout.toString().trim()
        return hash
    } catch (ex: Exception) {
        return "0"
    }
}

// Use it to build the version string
version = "0.1." + getCommitHash()

group = "io.cyborgsquirrel"

val kotlinVersion = project.properties["kotlinVersion"]
repositories {
    mavenCentral()
}

val testAgent by configurations.creating

dependencies {
    ksp("io.micronaut.data:micronaut-data-processor")
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.validation:micronaut-validation-processor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-websocket")
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("com.h2database:h2")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("jakarta.persistence:jakarta.persistence-api")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.yaml:snakeyaml")
    testImplementation("io.mockk:mockk")
    // https://dev.to/sineaggi/fix-dynamic-agent-loading-warning-in-gradle-5748
    testAgent("net.bytebuddy:byte-buddy-agent:1.15.10")
}

// Add Java agent
// Dynamic loading currently warns during test runs, but will probably break in future JDK releases.
tasks.test {
    val testAgentFiles = testAgent.incoming.files
    jvmArgumentProviders.add {
        testAgentFiles.map { "-javaagent:${it.absolutePath}" }
    }
}

application {
    mainClass = "io.cyborgsquirrel.ApplicationKt"
}
java {
    sourceCompatibility = JavaVersion.toVersion("21")
}


graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("kotest5")

    processing {
        incremental(true)
        annotations("io.cyborgsquirrel.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}


tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}
