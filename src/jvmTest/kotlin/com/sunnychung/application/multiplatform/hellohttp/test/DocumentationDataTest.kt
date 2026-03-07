package com.sunnychung.application.multiplatform.hellohttp.test

import com.sunnychung.application.multiplatform.hellohttp.model.PostFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.PreFlightSpec
import com.sunnychung.application.multiplatform.hellohttp.model.ProtocolApplication
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestExample
import com.sunnychung.application.multiplatform.hellohttp.model.UserRequestTemplate
import com.sunnychung.application.multiplatform.hellohttp.util.uuidString
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.parseMarkdownTree
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.typeName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DocumentationDataTest {

    @Test
    fun `UserRequestExample deep copy keeps documentation and override flag`() {
        val subject = UserRequestExample(
            id = uuidString(),
            name = "Example",
            preFlight = PreFlightSpec(),
            postFlight = PostFlightSpec(),
            documentation = "# Heading\\nBody",
            overrides = UserRequestExample.Overrides(
                isOverrideDocumentation = false,
            ),
        )

        val copied = subject.deepCopyWithNewId()

        assertNotEquals(subject.id, copied.id)
        assertEquals(subject.documentation, copied.documentation)
        assertEquals(subject.overrides?.isOverrideDocumentation, copied.overrides?.isOverrideDocumentation)
    }

    @Test
    fun `UserRequestTemplate deep copy keeps documentation for all examples`() {
        val baseExample = UserRequestExample(
            id = uuidString(),
            name = "Base",
            documentation = "base doc",
            overrides = null,
        )
        val secondExample = UserRequestExample(
            id = uuidString(),
            name = "Secondary",
            documentation = "secondary doc",
            overrides = UserRequestExample.Overrides(
                isOverrideDocumentation = false,
            ),
        )
        val subject = UserRequestTemplate(
            id = uuidString(),
            name = "Request",
            application = ProtocolApplication.Http,
            method = "GET",
            url = "https://example.com",
            examples = listOf(baseExample, secondExample),
        )

        val copied = subject.deepCopyWithNewId()

        assertNotEquals(subject.id, copied.id)
        assertEquals("base doc", copied.examples[0].documentation)
        assertEquals("secondary doc", copied.examples[1].documentation)
        assertEquals(false, copied.examples[1].overrides?.isOverrideDocumentation)
    }

    @Test
    fun `markdown parser creates gfm nodes for documentation features`() {
        val source = """
| col-1 | col-2 |
| --- | --- |
| v1 | v2 |

![image alt](data:image/png;base64,aGVsbG8=)
        """.trimIndent()

        val root = parseMarkdownTree(source)
        val typeNames = buildSet {
            fun visit(node: org.intellij.markdown.ast.ASTNode) {
                add(node.typeName())
                node.children.forEach { visit(it) }
            }
            visit(root)
        }

        assertTrue(typeNames.contains("TABLE"))
        assertTrue(typeNames.contains("IMAGE"))
    }
}
