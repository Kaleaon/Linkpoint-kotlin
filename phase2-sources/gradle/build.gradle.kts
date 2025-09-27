/**
 * build.gradle.kts
 * 
 * Gradle build configuration for Kotlin translations of SecondLife viewer components
 * This builds the modern Kotlin implementations with proper dependency management
 * and integration with existing Kotlin ecosystem tools.
 */

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    id("maven-publish")
    application
}

group = "com.secondlife.viewer"
version = "2.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
    
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xcontext-receivers",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.DelicateCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
}

dependencies {
    // Kotlin Coroutines for async programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    
    // Serialization for message formats
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.6.2")
    
    // Date/Time handling
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    
    // Atomic operations
    implementation("org.jetbrains.kotlinx:kotlinx-atomicfu:0.23.2")
    
    // Collections
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")
    
    // Networking
    implementation("io.ktor:ktor-network:2.3.7")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Cryptography
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.77")
    
    // UUID generation
    implementation("com.benasher44:uuid:0.8.2")
    
    // Configuration
    implementation("com.typesafe:config:1.4.3")
    
    // Math and geometry
    implementation("org.joml:joml:1.10.5")
    
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.assertj:assertj-core:3.24.2")
    
    // Benchmarking
    testImplementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.10")
}

// Source sets configuration
sourceSets {
    main {
        kotlin {
            srcDirs("../kotlin")
        }
        resources {
            srcDirs("../resources")
        }
    }
    
    test {
        kotlin {
            srcDirs("../test/kotlin")
        }
        resources {
            srcDirs("../test/resources")
        }
    }
    
    create("reference") {
        kotlin {
            srcDirs("../reference/kotlin")
        }
    }
    
    create("demo") {
        kotlin {
            srcDirs("../demo/kotlin")
        }
    }
}

// Application configuration
application {
    mainClass.set("com.secondlife.viewer.ViewerMainKt")
    applicationDefaultJvmArgs = listOf(
        // Memory settings
        "-Xms512m",
        "-Xmx2g",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=200",
        
        // Network settings
        "-Djava.net.preferIPv4Stack=true",
        "-Dsun.net.useExclusiveBind=false",
        
        // Security settings
        "-Djava.security.policy=all.policy",
        
        // Debugging (development only)
        "-Dkotlinx.coroutines.debug"
    )
}

// Fat JAR for standalone distribution
tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Create a fat JAR with all dependencies"
    
    archiveClassifier.set("all")
    from(sourceSets.main.get().output)
    
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    manifest {
        attributes("Main-Class" to "com.secondlife.viewer.ViewerMainKt")
    }
}

// Testing configuration
tasks.test {
    useJUnitPlatform()
    
    // Memory settings for tests
    minHeapSize = "256m"
    maxHeapSize = "1g"
    
    // Test execution settings
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    
    // Parallel execution
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    
    // Timeout settings
    timeout.set(Duration.ofMinutes(10))
}

// Development tasks
tasks.register("runDemo") {
    group = "application"
    description = "Run the demo application with sample data"
    
    dependsOn(tasks.classes)
    
    doLast {
        javaexec {
            classpath = sourceSets.main.get().runtimeClasspath
            mainClass.set("com.secondlife.viewer.demo.DemoMainKt")
            args = listOf("--demo-mode", "--verbose")
        }
    }
}

tasks.register("devRun") {
    group = "application"
    description = "Run application in development mode with debugging enabled"
    
    dependsOn(tasks.classes)
    
    doLast {
        javaexec {
            classpath = sourceSets.main.get().runtimeClasspath
            mainClass.set("com.secondlife.viewer.ViewerMainKt")
            jvmArgs = listOf(
                "-Dkotlinx.coroutines.debug",
                "-Dlogback.configurationFile=logback-dev.xml",
                "-Xdebug",
                "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
            )
            systemProperty("development", "true")
        }
    }
}