import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

group = "com.sunnychung.application"
version = "1.6.0-SNAPSHOT" // must be in 'x.y.z' for native distributions

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//    maven("https://jitpack.io")
    mavenLocal()
}

val grpcVersion = "1.59.1"

kotlin {
    jvm {
        jvmToolchain(17)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(kotlin("reflect"))
                implementation(kotlin("stdlib"))

                implementation("co.touchlab:kermit:1.0.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.6.0")

                implementation("io.github.sunny-chung:httpclient5:5.2.1-inspect-patch5")
//                implementation("com.squareup.okhttp3:okhttp:4.11.0")
                implementation("io.github.sunny-chung:okhttp:4.11.0-patch-1")
//                implementation("com.github.sunny-chung:okhttp:patch~4.11.0-SNAPSHOT")
                implementation("com.squareup.okhttp3:logging-interceptor:4.11.0") {
                    exclude(group = "com.squareup.okhttp3", module = "okhttp")
                }
                implementation("io.github.sunny-chung:Java-WebSocket:1.5.4-inspect-patch2")
                implementation("com.graphql-java:graphql-java:21.3")

                // grpc
//                runtimeOnly("io.grpc:grpc-netty-shaded:$grpcVersion-patch1")
                implementation("io.github.sunny-chung:grpc-netty-shaded:$grpcVersion-patch1")
//                implementation("io.grpc:grpc-netty:$grpcVersion-patch1")
                implementation("io.grpc:grpc-protobuf:$grpcVersion")
//    implementation("io.grpc:grpc-stub:$grpcVersion")
                implementation("io.grpc:grpc-services:$grpcVersion")
                implementation("io.grpc:grpc-kotlin-stub:1.4.1")
                compileOnly("org.apache.tomcat:annotations-api:6.0.53")
                implementation("com.google.protobuf:protobuf-java-util:3.25.1")
                implementation("com.google.protobuf:protobuf-kotlin:3.25.1")


                implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
                implementation("com.jayway.jsonpath:json-path:2.8.0")

                implementation("io.github.sunny-chung:kdatetime-multiplatform:1.0.0")

                implementation("net.harawata:appdirs:1.2.2")
                implementation("com.darkrockstudios:mpfilepicker:2.1.0")

                implementation("org.jetbrains.compose.components:components-splitpane:1.5.2")

                implementation("io.github.sunny-chung:kotlite-interpreter:1.1.0-snapshot.2")
                implementation("io.github.sunny-chung:kotlite-stdlib:1.1.0-snapshot.1")
            }

            resources.srcDir("$buildDir/resources")
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.15.2")
            }
        }
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group in setOf("io.github.sunny-chung", "io.grpc") && requested.name in setOf("grpc-core", "grpc-api", "grpc-netty", "grpc-netty-shaded")) {
            if (requested.version == grpcVersion) {
                useTarget("io.github.sunny-chung:${requested.name}:$grpcVersion-patch1")
                because("transport inspection")
            }
        } else if (requested.group == "io.grpc" && requested.name.startsWith("grpc-") && requested.version?.startsWith("$grpcVersion-patch") == true) {
            useVersion(grpcVersion)
            because("not built")
        }
    }
}

fun getGitCommitHash(): String {
    val stdout = ByteArrayOutputStream()
    rootProject.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

tasks.create("createBuildProperties") {
    doFirst {
        val file = File("$buildDir/resources/build.properties")
        file.parentFile.mkdirs()
        file.writer().use { writer ->
            val p = Properties()
            p["version"] = project.version.toString()
            p["git.commit"] = getGitCommitHash()
            p.store(writer, null)
        }
    }
}

tasks.getByName("jvmProcessResources") {
    dependsOn("createBuildProperties")
}

tasks.getByName("jvmMainClasses") {
    dependsOn("createBuildProperties")
}

tasks.withType<Test> {
    testLogging {
        events = setOf(TestLogEvent.STARTED, TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
    }
}

compose.desktop {
    application {
        val distributionVersion = "^(\\d+\\.\\d+\\.\\d+).*".toRegex().matchEntire(project.version.toString())!!.groupValues[1]

        mainClass = "com.sunnychung.application.multiplatform.hellohttp.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Hello HTTP"
            vendor = "Sunny Chung"
            copyright = "© 2023 Sunny Chung"
            packageVersion = distributionVersion

            macOS {
                iconFile.set(project.file("appicon/appicon.icns"))
                infoPlist {
                    extraKeysRawXml = """
                        <key>LSMinimumSystemVersion</key>
                        <string>10</string>
                    """.trimIndent()
                }
            }
            windows {
                iconFile.set(project.file("appicon/appicon.ico"))
            }
            linux {
                iconFile.set(project.file("appicon/appicon.png"))
            }
        }
    }
}
