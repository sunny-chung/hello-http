import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
//    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("junit:junit:4.13.2")

    testImplementation(project(":test-common"))
    testImplementation(rootProject)
    testImplementation("io.github.sunny-chung:kdatetime-multiplatform:1.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
    testImplementation(compose.desktop.currentOs)
}

tasks.withType<Test> {
//    useJUnitPlatform()
    useJUnit()

    jvmArgs("-Xmx2048m")

    testLogging {
        events = setOf(TestLogEvent.STARTED, TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
    }
}
