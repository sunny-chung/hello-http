package com.sunnychung.application.multiplatform.hellohttp.exporter.apidoc

internal object ApiDocHtmlTemplate {

    private const val templatePath = "/template/api-doc.html"
    private const val dataJsonPlaceholder = "__HELLO_HTTP_API_DOC_DATA_JSON__"

    private val templateHtml: String by lazy {
        javaClass.getResourceAsStream(templatePath)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: throw IllegalStateException("Cannot load API documentation template at $templatePath")
    }

    fun render(dataJson: String): String {
        return templateHtml.replace(dataJsonPlaceholder, dataJson)
    }
}
