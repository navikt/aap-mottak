plugins {
    kotlin("jvm") version "1.9.23"
    id("io.ktor.plugin") version "2.3.9"
}

val aapLibVersion = "5.0.5"
val ktorVersion = "2.3.9"

application {
    mainClass.set("mottak.AppKt")
}

dependencies {
    implementation("com.github.navikt.aap-libs:ktor-auth:$aapLibVersion")
    implementation("com.github.navikt.aap-libs:kafka-streams:$aapLibVersion")
    implementation("com.github.navikt.aap-libs:kafka-avroserde:$aapLibVersion")

    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:08271806")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")

    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.4")
    implementation("ch.qos.logback:logback-classic:1.5.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")

    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.4")

    testImplementation(kotlin("test"))
    testImplementation("com.github.navikt.aap-libs:kafka-streams-test:$aapLibVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.0")
    testImplementation("uk.org.webcompere:system-stubs-jupiter:2.1.6")
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    maven("https://packages.confluent.io/maven/")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
    withType<Test> {
        useJUnitPlatform()
    }
}

kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")