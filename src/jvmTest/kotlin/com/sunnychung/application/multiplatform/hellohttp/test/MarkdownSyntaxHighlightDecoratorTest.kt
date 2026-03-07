package com.sunnychung.application.multiplatform.hellohttp.test

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.MarkdownInlineCustomDecorationType
import com.sunnychung.application.multiplatform.hellohttp.ux.markdown.parseMarkdownCustomizedAst
import com.sunnychung.application.multiplatform.hellohttp.ux.local.darkColorScheme
import com.sunnychung.application.multiplatform.hellohttp.ux.transformation.incremental.MarkdownSyntaxHighlightDecorator
import com.sunnychung.lib.multiplatform.bigtext.core.BigText
import com.sunnychung.lib.multiplatform.bigtext.core.createFromTinyString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownSyntaxHighlightDecoratorTest {

    @Test
    fun `highlight should support markdown features for documentation editor`() {
        val markdown = """
# Release Plan

## Preparation Checklist

### Build Verification

Document with `session-token` inline code.

-----

## Deployment Notes

```HTML
<section>
  <p>Release status board</p>
</section>
```

> Keep metrics visible.
> Confirm alert routing.
> > Escalate to on-call if latency spikes.

Use **critical** checks and *carefully* monitor ~~deprecated path~~ while keeping __underlined note__ visible.

This guide links to the [documentation portal](https://www.example.com/docs).

| Area | Owner |
|------|-------|
| API gateway status summary | Platform Team |
| Frontend rollout window | UI Team |

Image from web should stay plain markdown:
![weather icon](https://example.com/weather.png)

Inline base64 image:
![deployment badge](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACgAAAAoCAMAAAC7IEhfAAAA81BMVEX////qQzU0qFNChfT7vAW7z/o0f/Q8gvR4o/b7ugDqQTLqPS37twArpk3pOSjrTkIjpEgSoT9ArFxvu4DvenPoKhPpNCPvf3j0q6bpMR785uX/9+r92I/8wAD80HP8y1/s9e6l06/3wL3zoJvwh4H97e3yl5LtamHucWntZFv519XrVUnxjnTpMi7xfybpNDj2nhnrUDL4zMrvbCfrTkz95Lf706vQ3fxilvVOi/To3a2ArUA5rmaTtPjwuxFOqk7J5M/Ptifx9f6esToMplenwfmSwYCTv9Eqd/w3mptetXM1o3A+jNgzqkA3lbB/wo+Ty6DEnl0KAAABrUlEQVQ4jZ3Ra0OCMBQG4KGIMQRBGIhGeSPT7nftamU3y/D//5oGGzgHavV+kuPDztkBgH/Gb7U7ltVpt2orVVtAtqbgaDaCO8us7yANCkmghiw/y1WRInBRUDXtui7Pwri7vCtrWU6AiBu0zLSFihKPCl3OdROnuHbPcXqaG1agzbmqm3Ry+qTUdxBMnecj6rQ95p9amXdg/4A421qs864uHx5F46VWwWVT9o6xhMIaB0RZ9Eongnu6xtVLoih63tn5ugMvZDGMdzkvFfhckREjWKrPocRnEFa3KGSaFPNchmF1m0BxFZT+BH/TurjsMsU47Ix0PaXrOdxIQg8cJAuX5ZvbrCVLBN5FD/gTyvcPY2OUdgUCpQIgQ8qP4xxOGg7JkNITIL2fI6eavHuRmBFxXtVcFJ2TtHHcGYCGkaOy0mDc23u8xqTU1KlUjYBeqdFUpx/S4oE4E9ocU12fBMEkZ+B3p5/5IjNhlEoiscWJf33hj7Q4OCuZTL/papjuehZUpw3Ap2mkDzVSq40uai5S1aiMslxIZ/i2ahTd0M1ljNjRLDDNYNZcqVblB1ACJr3BpmbLAAAAAElFTkSuQmCC)

Action List
- Capture request id
- Validate response schema
  - Verify `trace-id` header
  - Confirm dependency health
- Complete handoff

Ordered Steps
1. Create request body
2. Run smoke tests
3. Publish release notes
""".trimIndent()

        val colors = darkColorScheme()
        val highlighted = highlight(markdown)

        assertEquals(markdown, highlighted.text)

        assertHasSpan(
            highlighted = highlighted,
            fragment = "Release Plan",
            matcher = { it.color == colors.syntaxColor.keyword && it.fontWeight == FontWeight.Bold },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "-----",
            matcher = { it.color == colors.unimportant },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "Release status board",
            matcher = { it.color == colors.syntaxColor.stringLiteral && it.background == colors.backgroundLight.copy(alpha = 0.5f) },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "session-token",
            matcher = { it.color == colors.syntaxColor.stringLiteral && it.background == colors.backgroundLight.copy(alpha = 0.5f) },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "Keep metrics visible.",
            matcher = { it.color == colors.syntaxColor.comment },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "critical",
            matcher = { it.fontWeight == FontWeight.Bold },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "carefully",
            matcher = { it.fontStyle == FontStyle.Italic },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "deprecated path",
            matcher = { it.hasDecoration(TextDecoration.LineThrough) },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "underlined note",
            matcher = { it.hasDecoration(TextDecoration.Underline) },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "documentation portal",
            matcher = {
                it.color == colors.syntaxColor.objectKey &&
                    it.hasDecoration(TextDecoration.Underline)
            },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "Capture request id",
            matcher = { it.color == colors.syntaxColor.directive },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "Create request body",
            matcher = { it.color == colors.syntaxColor.directive },
        )
        assertHasSpan(
            highlighted = highlighted,
            fragment = "API gateway status summary",
            matcher = { it.color == colors.syntaxColor.type },
        )
    }

    @Test
    fun `highlight should support within-word underline and strikethrough markers`() {
        val markdown = """
Use ab~~test~~ef in migration notes.
Tag a__b__c for temporary emphasis.
""".trimIndent()

        val highlighted = highlight(markdown)

        val strikeContentStart = markdown.indexOf("~~test~~").let { it + 2 }
        assertHasSpanInRange(
            highlighted = highlighted,
            start = strikeContentStart,
            endExclusive = strikeContentStart + "test".length,
            matcher = { it.hasDecoration(TextDecoration.LineThrough) },
            failureMessage = "Expected strikethrough style for within-word ~~test~~",
        )

        val underlineContentStart = markdown.indexOf("__b__").let { it + 2 }
        assertHasSpanInRange(
            highlighted = highlighted,
            start = underlineContentStart,
            endExclusive = underlineContentStart + 1,
            matcher = { it.hasDecoration(TextDecoration.Underline) },
            failureMessage = "Expected underline style for within-word __b__",
        )
    }

    @Test
    fun `customized ast should include only supported custom inline decoration ranges`() {
        val markdown = """
Plain ab~~critical~~ef and a__b__c.
`skip __code__ custom marker`
[skip ~~link~~ marker](https://example.com)
""".trimIndent()

        val customizedAst = parseMarkdownCustomizedAst(markdown)
        val decorations = customizedAst.inlineCustomDecorations

        val expectedStrikeContentStart = markdown.indexOf("~~critical~~") + 2
        assertTrue(
            decorations.any {
                it.type == MarkdownInlineCustomDecorationType.Strikethrough &&
                    it.start == expectedStrikeContentStart &&
                    it.endExclusive == expectedStrikeContentStart + "critical".length
            },
            "Expected custom strikethrough range for ~~critical~~",
        )

        val expectedUnderlineContentStart = markdown.indexOf("__b__") + 2
        assertTrue(
            decorations.any {
                it.type == MarkdownInlineCustomDecorationType.Underline &&
                    it.start == expectedUnderlineContentStart &&
                    it.endExclusive == expectedUnderlineContentStart + 1
            },
            "Expected custom underline range for __b__",
        )

        val codeRange = markdown.indexOf("`skip __code__ custom marker`").let { start ->
            start until (start + "`skip __code__ custom marker`".length)
        }
        assertTrue(
            decorations.none { it.start in codeRange || (it.endExclusive - 1) in codeRange },
            "Custom decorations should not be parsed from code spans",
        )

        val linkRange = markdown.indexOf("[skip ~~link~~ marker](https://example.com)").let { start ->
            start until (start + "[skip ~~link~~ marker](https://example.com)".length)
        }
        assertTrue(
            decorations.none { it.start in linkRange || (it.endExclusive - 1) in linkRange },
            "Custom decorations should not be parsed from inline links",
        )
    }

    private fun highlight(source: String): AnnotatedString {
        val decorator = MarkdownSyntaxHighlightDecorator(darkColorScheme())
        decorator.initialize(BigText.createFromTinyString(source))
        return decorator.onApplyDecorationOnOriginal(source, source.indices) as AnnotatedString
    }

    private fun assertHasSpan(
        highlighted: AnnotatedString,
        fragment: String,
        matcher: (SpanStyle) -> Boolean,
    ) {
        val start = highlighted.text.indexOf(fragment)
        assertTrue(start >= 0, "Cannot find fragment: '$fragment'")
        val endExclusive = start + fragment.length
        assertHasSpanInRange(
            highlighted = highlighted,
            start = start,
            endExclusive = endExclusive,
            matcher = matcher,
            failureMessage = "Expected style not found for fragment: '$fragment'",
        )
    }

    private fun assertHasSpanInRange(
        highlighted: AnnotatedString,
        start: Int,
        endExclusive: Int,
        matcher: (SpanStyle) -> Boolean,
        failureMessage: String,
    ) {
        val hasMatchingSpan = highlighted.spanStyles.any {
            it.start < endExclusive && it.end > start && matcher(it.item)
        }
        assertTrue(hasMatchingSpan, failureMessage)
    }

    private fun SpanStyle.hasDecoration(decoration: TextDecoration): Boolean {
        val appliedDecoration = textDecoration ?: return false
        if (appliedDecoration == decoration) {
            return true
        }
        return appliedDecoration == TextDecoration.combine(
            listOf(
                TextDecoration.Underline,
                TextDecoration.LineThrough,
            )
        )
    }
}
