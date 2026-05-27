plugins {
    id("org.jetbrains.kotlin.jvm") version "2.3.21"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.3.21"
    id("com.google.devtools.ksp") version "2.3.7"
    id("com.gradleup.shadow") version "9.4.1"
    id("io.micronaut.application") version "5.0.0"
    id("io.micronaut.test-resources") version "5.0.0"
    id("io.micronaut.aot") version "5.0.0"
}

fun Project.getBuildNumber(): String {
    val buildVersion = System.getProperty("BUILD_NUMBER")
    if (!buildVersion.isNullOrEmpty()) return buildVersion

    return try {
        val execOutput = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            isIgnoreExitValue = true
        }
        execOutput.standardOutput.asText.get().trim().ifEmpty { "0" }
    } catch (ex: Exception) {
        "0"
    }
}

version = "0.1." + getBuildNumber()

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
    implementation("io.micronaut.reactor:micronaut-reactor")
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
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    // https://dev.to/sineaggi/fix-dynamic-agent-loading-warning-in-gradle-5748
    testAgent("net.bytebuddy:byte-buddy-agent:1.18.8")
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
    sourceCompatibility = JavaVersion.toVersion("25")
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
    jdkVersion = "25"
}
