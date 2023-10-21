import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

group = "com.sunnychung.application"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//    maven("https://jitpack.io")
    mavenLocal()
}

kotlin {
    jvm {
        jvmToolchain(11)
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

                implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")
//                implementation("com.squareup.okhttp3:okhttp:4.11.0")
                implementation("com.squareup.okhttp3:okhttp:4.11.0-patch-1")
//                implementation("com.github.sunny-chung:okhttp:patch~4.11.0-SNAPSHOT")

                implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
                implementation("io.github.sunny-chung:kdatetime-multiplatform:0.4.0")

                implementation("net.harawata:appdirs:1.2.2")

                implementation("org.jetbrains.compose.components:components-splitpane:1.5.2")
            }

            resources.srcDir("$buildDir/resources")
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
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
    dependsOn("jvmProcessResources")

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

tasks.getByName("jvmMainClasses") {
    dependsOn("createBuildProperties")
}

compose.desktop {
    application {
        mainClass = "com.sunnychung.application.multiplatform.hellohttp.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "hello-http"
            packageVersion = "1.0.0"
        }
    }
}
