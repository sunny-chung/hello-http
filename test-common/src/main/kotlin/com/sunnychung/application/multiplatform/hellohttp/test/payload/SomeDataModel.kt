package com.sunnychung.application.multiplatform.hellohttp.test.payload

data class SomeDataModels(val data: List<SomeDataModel>)

data class SomeDataModel(
    val id: String,
    val index: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val tags: List<String>,
    val input: List<SomeInput>,
    val output: SomeOutput,
    val description: String,
)
