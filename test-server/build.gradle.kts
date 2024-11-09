import com.google.protobuf.gradle.id
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.2.5"
	id("io.spring.dependency-management") version "1.1.4"
	kotlin("jvm")
	kotlin("plugin.spring")

	// grpc
	id("com.google.protobuf") version "0.9.4"
}

group = "com.sunnychung.application.multiplatform.hellohttp"
//version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

val grpcVersion = "1.63.0"
val grpcKotlinVersion = "1.4.1"

// don't use 4.x until below issues are resolved
// https://github.com/grpc/grpc-java/issues/11015
// https://github.com/protocolbuffers/protobuf/issues/16452
val protocVersion = "3.25.3" // "4.26.1"

dependencies {
	implementation(project(":test-common"))
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-graphql")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("io.projectreactor.addons:reactor-extra")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
//	testImplementation("org.springframework.boot:spring-boot-starter-test")
//	testImplementation("io.projectreactor:reactor-test")
//	testImplementation("org.springframework.graphql:spring-graphql-test")

	// grpc
	implementation("net.devh:grpc-server-spring-boot-starter:3.1.0.RELEASE")
	implementation("io.grpc:grpc-protobuf:$grpcVersion")
	implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
	implementation("com.google.protobuf:protobuf-kotlin:$protocVersion")

	// for ssl/mtls
	implementation("org.bouncycastle:bcprov-jdk18on:1.79")
	implementation("org.bouncycastle:bcpkix-jdk18on:1.79")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

protobuf {
	protoc { artifact = "com.google.protobuf:protoc:$protocVersion" }
	plugins {
		id("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
		}
		id("grpckt") {
			artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
		}
	}
	generateProtoTasks {
		all().forEach {
			it.plugins {
				id("grpc")
				id("grpckt")
			}
			it.builtins {
				id("kotlin")
			}
		}
	}
}
