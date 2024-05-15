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

    onlyIf {
        // In GitHub Actions CI, GUI windows cannot be created in Ubuntu instances
        // don't run UX tests in those instances

        val os = System.getProperty("os.name")
        if (project.hasProperty("isCI")) {
            println("CI = ${project.property("isCI") != "false"}")
        } else {
            println("No CI")
        }
        !(project.hasProperty("isCI")
            && project.property("isCI") != "false"
            && os != "Mac OS X"
            && !os.startsWith("Win")
        )
    }

    testLogging {
        events = setOf(TestLogEvent.STARTED, TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
    }
}
