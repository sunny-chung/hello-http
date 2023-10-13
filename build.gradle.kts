import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "hello-http"
            packageVersion = "1.0.0"
        }
    }
}
