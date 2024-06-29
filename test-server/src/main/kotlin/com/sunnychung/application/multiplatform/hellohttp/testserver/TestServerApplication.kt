package com.sunnychung.application.multiplatform.hellohttp.testserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TestServerApplication

fun main(args: Array<String>) {
	println(">>>> io.netty.maxDirectMemory = ${System.getProperty("io.netty.maxDirectMemory")}")
	println(">>>> maxDirectMemory = ${io.netty.util.internal.PlatformDependent.maxDirectMemory()}")

	System.setProperty("reactor.netty.http.server.accessLogEnabled", "true")
	runApplication<TestServerApplication>(*args)
}
